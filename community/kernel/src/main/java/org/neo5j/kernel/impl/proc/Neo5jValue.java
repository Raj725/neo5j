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
package org.neo5j.kernel.impl.proc;

import java.util.List;
import java.util.Map;

import org.neo5j.kernel.api.proc.Neo5jTypes;

public class Neo5jValue
{
    private final Object value;
    private final Neo5jTypes.AnyType type;

    public Neo5jValue( Object value, Neo5jTypes.AnyType type )
    {
        this.value = value;
        this.type = type;
    }

    public Object value()
    {
        return value;
    }

    public Neo5jTypes.AnyType neo5jType()
    {
        return type;
    }

    public static  Neo5jValue ntString(String value)
    {
        return new Neo5jValue( value, Neo5jTypes.NTString );
    }

    public static  Neo5jValue ntInteger(long value)
    {
        return new Neo5jValue( value, Neo5jTypes.NTInteger );
    }

    public static  Neo5jValue ntFloat(double value)
    {
        return new Neo5jValue( value, Neo5jTypes.NTFloat );
    }

    public static  Neo5jValue ntBoolean(boolean value)
    {
        return new Neo5jValue( value, Neo5jTypes.NTBoolean );
    }

    public static  Neo5jValue ntMap(Map<String, Object> value)
    {
        return new Neo5jValue( value, Neo5jTypes.NTMap );
    }

    public static Neo5jValue ntList(List<?> value, Neo5jTypes.AnyType inner)
    {
        return new Neo5jValue( value, Neo5jTypes.NTList( inner ) );
    }

    @Override
    public String toString()
    {
        return "Neo5jValue{" +
               "value=" + value +
               ", type=" + type +
               '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        { return true; }
        if ( o == null || getClass() != o.getClass() )
        { return false; }

        Neo5jValue that = (Neo5jValue) o;

        if ( value != null ? !value.equals( that.value ) : that.value != null )
        { return false; }
        return type.equals( that.type );

    }

    @Override
    public int hashCode()
    {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + type.hashCode();
        return result;
    }
}
