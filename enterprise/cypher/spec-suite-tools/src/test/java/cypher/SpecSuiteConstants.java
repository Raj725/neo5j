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
package cypher;

interface SpecSuiteConstants
{
    String DB_CONFIG = "cypher.cucumber.db.DatabaseConfigProvider:/db-config/";
    String GLUE_PATH = "classpath:cypher/feature/steps";
    String HTML_REPORT = "html:target/";
    String JSON_REPORT = "cypher.feature.reporting.CypherResultReporter:target/";
    String BLACKLIST_PLUGIN = "cypher.cucumber.BlacklistPlugin:/blacklists/";
    String CYPHER_OPTION_PLUGIN = "cypher.cucumber.CypherOptionPlugin:/cypher-options/";
}