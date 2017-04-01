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
package org.neo5j.consistency;

import java.io.PrintWriter;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

import org.neo5j.function.Suppliers;
import org.neo5j.logging.AbstractPrintWriterLogger;
import org.neo5j.logging.Logger;

public class ConsistencyReportLogger extends AbstractPrintWriterLogger
{
    private final String prefix;

    public ConsistencyReportLogger( @Nonnull Supplier<PrintWriter> writerSupplier, @Nonnull Object lock, String prefix,
            boolean autoFlush )
    {
        super( writerSupplier, lock, autoFlush );
        this.prefix = prefix;
    }

    @Override
    protected void writeLog( @Nonnull PrintWriter out, @Nonnull String message )
    {
        out.write( prefix );
        out.write( ": " );
        out.write( message );
        out.println();
    }

    @Override
    protected void writeLog( @Nonnull PrintWriter out, @Nonnull String message, @Nonnull Throwable throwable )
    {
        out.write( prefix );
        out.write( ": " );
        out.write( message );
        out.write( ' ' );
        out.write( throwable.getMessage() );
        out.println();
        throwable.printStackTrace( out );
    }

    @Override
    protected Logger getBulkLogger( @Nonnull PrintWriter out, @Nonnull Object lock )
    {
        return new ConsistencyReportLogger( Suppliers.singleton( out ), lock, prefix, false );
    }
}
