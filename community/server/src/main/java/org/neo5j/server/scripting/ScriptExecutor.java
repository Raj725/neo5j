/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.server.scripting;

import java.util.Map;

import org.neo5j.server.rest.domain.EvaluationException;

/**
 * Common abstraction for compiled and runtime-evaluated scripts.
 * This represents a single threaded, stateful, script session.
 */
public interface ScriptExecutor
{

    interface Factory
    {
        ScriptExecutor createExecutorForScript( String script ) throws EvaluationException;
    }

    /**
     * Execute the contained script.
     * @return
     * @param variables Is variables that should be available to the script.
     */
    Object execute( Map<String,Object> variables ) throws EvaluationException;

}
