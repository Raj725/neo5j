/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cypher.feature.steps

import java.util

import cucumber.api.DataTable
import cypher.SpecSuiteResources
import cypher.cucumber.CypherOptionPlugin
import cypher.cucumber.db.DatabaseConfigProvider._
import cypher.cucumber.db.{GraphArchive, GraphArchiveImporter, GraphArchiveLibrary, GraphFileRepository}
import cypher.feature.parser._
import cypher.feature.parser.matchers.ResultWrapper
import org.neo5j.collection.RawIterator
import org.neo5j.cypher.internal.frontend.v3_2.symbols.{CypherType, _}
import org.neo5j.graphdb.factory.{EnterpriseGraphDatabaseFactory, GraphDatabaseSettings}
import org.neo5j.graphdb.{GraphDatabaseService, QueryStatistics, Result, Transaction}
import org.neo5j.kernel.api.KernelAPI
import org.neo5j.kernel.api.exceptions.ProcedureException
import org.neo5j.kernel.api.proc.CallableProcedure.BasicProcedure
import org.neo5j.kernel.api.proc.{Context, Neo5jTypes}
import org.neo5j.kernel.internal.GraphDatabaseAPI
import org.neo5j.procedure.Mode
import org.neo5j.test.TestEnterpriseGraphDatabaseFactory
import org.opencypher.tools.tck.TCKCucumberTemplate
import org.opencypher.tools.tck.constants.TCKStepDefinitions._
import org.scalatest.{FunSuiteLike, Matchers}

import scala.collection.JavaConverters._
import scala.reflect.io.Path
import scala.util.Try

trait SpecSuiteSteps extends FunSuiteLike with Matchers with TCKCucumberTemplate with MatcherMatchingSupport {

  // Implement in subclasses

  def specSuiteClass: Class[_]

  lazy val graphArchiveLibrary = new GraphArchiveLibrary(new GraphFileRepository(Path(SpecSuiteResources.targetDirectory(specSuiteClass, "graphs"))))
  lazy val requiredScenarioName = specSuiteClass.getField( "SCENARIO_NAME_REQUIRED" ).get( null ).toString.trim.toLowerCase

  var scenarioBuilder: ScenarioExecutionBuilder = _

  // Steps

  Before() { scenario =>
    val currentScenarioName = scenario.getName.toLowerCase
    val skip = requiredScenarioName.nonEmpty && !currentScenarioName.contains(requiredScenarioName)
    scenarioBuilder = new ScenarioExecutionBuilder
    scenarioBuilder.register(scenario.getName, skip)
  }

  After() { _ =>
    scenarioBuilder.build().run()
  }

  Background(BACKGROUND) {
    // do nothing, but necessary for the scala match
  }

  Given(NAMED_GRAPH) { (dbName: String) =>
    scenarioBuilder.setDb(lendForReadOnlyUse(dbName))
  }

  Given(ANY_GRAPH) {
    scenarioBuilder.setDb(DbBuilder.initAny(currentDatabaseConfig("8m")))
  }

  Given(EMPTY_GRAPH) {
    scenarioBuilder.setDb(DbBuilder.initEmpty(currentDatabaseConfig("8m")))
  }

  And(INIT_QUERY) { (query: String) =>
    scenarioBuilder.init { g: GraphDatabaseAPI =>
      // side effects are necessary for setting up graph state
      g.execute(s"CYPHER runtime=interpreted $query")
    }
  }

  And(PARAMETERS) { (values: DataTable) =>
    scenarioBuilder.setParams(parseParameters(values))
  }

  private val INSTALLED_PROCEDURE = """^there exists a procedure (.+):$"""
  And(INSTALLED_PROCEDURE){ (signatureText: String, values: DataTable) =>
    scenarioBuilder.procedureRegistration { g: GraphDatabaseAPI =>
      val parsedSignature = ProcedureSignature.parse(signatureText)
      val kernelProcedure = buildProcedure(parsedSignature, values)
      g.getDependencyResolver.resolveDependency(classOf[KernelAPI]).registerProcedure(kernelProcedure)
    }
  }

  When(EXECUTING_QUERY) { (query: String) =>
    scenarioBuilder.exec { (g: GraphDatabaseAPI, params: util.Map[String, Object]) =>
      g.execute(s"${CypherOptionPlugin.options} $query", params)
    }
  }

  Then(EXPECT_RESULT) { (expectedTable: DataTable) =>
    scenarioBuilder.expect { r: Result =>
      val matcher = constructResultMatcher(expectedTable)

      matcher should accept(new ResultWrapper(r))
    }
  }

  Then(EXPECT_RESULT_UNORDERED_LISTS) { (expectedTable: DataTable) =>
    scenarioBuilder.expect { r: Result =>
      val matcher = constructResultMatcher(expectedTable, unorderedLists = true)

      matcher should accept(new ResultWrapper(r))
    }
  }


  Then(EXPECT_ERROR) { (typ: String, phase: String, detail: String) =>
    scenarioBuilder.expectError { (r: Try[Result], tx: Transaction) =>
      SpecSuiteErrorHandler(typ, phase, detail).check(r, tx)
    }
  }

  Then(EXPECT_SORTED_RESULT) { (expectedTable: DataTable) =>
    scenarioBuilder.expect { r: Result =>
      val matcher = constructResultMatcher(expectedTable)

      matcher should acceptOrdered(new ResultWrapper(r))
    }
  }

  Then(EXPECT_EMPTY_RESULT) {
    scenarioBuilder.expect { r: Result =>
      withClue("Expected empty result") {
        r.hasNext shouldBe false
      }
    }
  }

  And(SIDE_EFFECTS) { (expectations: DataTable) =>
    scenarioBuilder.sideEffects { stats: QueryStatistics =>
      statisticsParser(expectations) should accept(stats)
    }
  }

  And(NO_SIDE_EFFECTS) {
    scenarioBuilder.sideEffects { stats: QueryStatistics =>
      stats.containsUpdates() shouldBe false
    }
  }

  When(EXECUTING_CONTROL_QUERY) { (query: String) =>
    scenarioBuilder.exec { (g: GraphDatabaseAPI, params: util.Map[String, Object]) =>
      g.execute(query, params)
    }
  }

  private def lendForReadOnlyUse(recipeName: String) = {
    val recipe = graphArchiveLibrary.recipe(recipeName)
    val recommendedPcSize = recipe.recommendedPageCacheSize
    val pcSize = (recommendedPcSize/MB(32)+1)*MB(32)
    val config = currentDatabaseConfig(pcSize.toString)
    val archiveUse = GraphArchive(recipe, config).readOnlyUse
    val path = graphArchiveLibrary.lendForReadOnlyUse(archiveUse)(graphImporter)
    val builder = new EnterpriseGraphDatabaseFactory().newEmbeddedDatabaseBuilder(path.jfile)
    builder.setConfig(archiveUse.dbConfig.asJava)
    builder.newGraphDatabase().asInstanceOf[GraphDatabaseAPI]
  }

  private def MB(v: Int) = v * 1024 * 1024

  private def currentDatabaseConfig(sizeHint: String) = {
    val builder = Map.newBuilder[String, String]
    builder += GraphDatabaseSettings.pagecache_memory.name() -> sizeHint
    builder += GraphDatabaseSettings.cypher_hints_error.name() -> "true"
    cypherConfig().foreach { case (s, v) => builder += s.name() -> v }
    builder.result()
  }

  private def buildProcedure(parsedSignature: ProcedureSignature, values: DataTable) = {
    val signatureFields = parsedSignature.fields
    val (tableColumns, tableValues) = parseValueTable(values)
    if (tableColumns != signatureFields)
      throw new scala.IllegalArgumentException(
        s"Data table columns must be the same as all signature fields (inputs + outputs) in order (Actual: ${formatColumns(tableColumns)} Expected: ${formatColumns(signatureFields)})"
      )
    val kernelSignature = asKernelSignature(parsedSignature)
    val kernelProcedure = new BasicProcedure(kernelSignature) {
      override def apply(ctx: Context, input: Array[AnyRef]): RawIterator[Array[AnyRef], ProcedureException] = {
        val scalaIterator = tableValues
          .filter { row => input.indices.forall { index => row(index) == input(index) } }
          .map { row => row.drop(input.length).clone() }
          .toIterator

        val rawIterator = RawIterator.wrap[Array[AnyRef], ProcedureException](scalaIterator.asJava)
        rawIterator
      }
    }
    kernelProcedure
  }

  private def formatColumns(columns: List[String]) = columns.map(column => s"'${column.replace("'", "\\'")}'")

  private def asKernelSignature(parsedSignature: ProcedureSignature): org.neo5j.kernel.api.proc.ProcedureSignature = {
    val builder = org.neo5j.kernel.api.proc.ProcedureSignature.procedureSignature(parsedSignature.namespace.toArray, parsedSignature.name)
    builder.mode(Mode.READ)
    parsedSignature.inputs.foreach { case (name, tpe) => builder.in(name, asKernelType(tpe)) }
    parsedSignature.outputs match {
      case Some(fields) => fields.foreach { case (name, tpe) => builder.out(name, asKernelType(tpe)) }
      case None => builder.out(org.neo5j.kernel.api.proc.ProcedureSignature.VOID)
    }
    builder.build()
  }

  private def asKernelType(tpe: CypherType):  Neo5jTypes.AnyType = tpe match {
    case CTMap => Neo5jTypes.NTMap
    case CTNode => Neo5jTypes.NTNode
    case CTRelationship => Neo5jTypes.NTRelationship
    case CTPath => Neo5jTypes.NTPath
    case ListType(innerTpe) => Neo5jTypes.NTList(asKernelType(innerTpe))
    case CTString => Neo5jTypes.NTString
    case CTBoolean => Neo5jTypes.NTBoolean
    case CTNumber => Neo5jTypes.NTNumber
    case CTInteger => Neo5jTypes.NTInteger
    case CTFloat => Neo5jTypes.NTFloat
  }

  object graphImporter extends GraphArchiveImporter {
    protected def createDatabase(archive: GraphArchive.Descriptor, destination: Path): GraphDatabaseService = {
      val builder = new EnterpriseGraphDatabaseFactory().newEmbeddedDatabaseBuilder(destination.jfile)
      builder.setConfig(archive.dbConfig.asJava)
      builder.newGraphDatabase()
    }
  }
}

object DbBuilder {
  def initEmpty(config: Map[String, String]): GraphDatabaseAPI = {
      val builder = new TestEnterpriseGraphDatabaseFactory().newImpermanentDatabaseBuilder()
      builder.setConfig(config.asJava)
      builder.newGraphDatabase().asInstanceOf[GraphDatabaseAPI]
  }

  /**
    * Creates a database with 10 unlabeled nodes.
    */
  def initAny(config: Map[String, String]): GraphDatabaseAPI = {
    val api = initEmpty(config)
    // This may prevent mistakes where a scenario is actually reliant on an empty db
    api.execute("CYPHER runtime=interpreted UNWIND range(0, 9) AS i CREATE ()")
    api
  }
}
