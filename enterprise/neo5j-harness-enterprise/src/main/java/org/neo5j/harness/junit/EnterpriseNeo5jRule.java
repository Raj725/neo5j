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
package org.neo5j.harness.junit;

import java.io.File;

import org.neo5j.harness.EnterpriseTestServerBuilders;

public class EnterpriseNeo5jRule extends Neo5jRule
{
    public EnterpriseNeo5jRule()
    {
        super( EnterpriseTestServerBuilders.newInProcessBuilder() );
    }

    public EnterpriseNeo5jRule( File workingDirectory )
    {
        super( EnterpriseTestServerBuilders.newInProcessBuilder( workingDirectory ) );
    }
}
