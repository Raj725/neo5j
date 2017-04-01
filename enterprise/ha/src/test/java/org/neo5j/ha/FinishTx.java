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
package org.neo5j.ha;

import org.neo5j.graphdb.Transaction;
import org.neo5j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo5j.test.OtherThreadExecutor.WorkerCommand;

public class FinishTx implements WorkerCommand<HighlyAvailableGraphDatabase, Void>
{
    private final Transaction tx;
    private final boolean successful;

    public FinishTx( Transaction tx, boolean successful )
    {
        this.tx = tx;
        this.successful = successful;
    }

    @Override
    public Void doWork( HighlyAvailableGraphDatabase state )
    {
        if ( successful )
        {
            tx.success();
        }
        tx.close();
        return null;
    }
}
