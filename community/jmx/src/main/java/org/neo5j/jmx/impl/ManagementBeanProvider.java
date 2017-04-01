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
package org.neo5j.jmx.impl;

import java.util.Collection;
import java.util.Collections;
import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;

import org.neo5j.helpers.Service;
import org.neo5j.kernel.internal.KernelData;

public abstract class ManagementBeanProvider extends Service
{
    final Class<?> beanInterface;

    public ManagementBeanProvider( Class<?> beanInterface )
    {
        super( ManagementSupport.beanName( beanInterface ) );
        if ( DynamicMBean.class.isAssignableFrom( beanInterface ) )
        {
            beanInterface = DynamicMBean.class;
        }
        this.beanInterface = beanInterface;
    }

    protected Iterable<? extends Neo5jMBean> createMBeans( ManagementData management )
            throws NotCompliantMBeanException
    {
        return singletonOrNone( createMBean( management ) );
    }

    protected Iterable<? extends Neo5jMBean> createMXBeans( ManagementData management )
            throws NotCompliantMBeanException
    {
        Class<?> implClass;
        try
        {
            implClass = getClass().getDeclaredMethod( "createMBeans", ManagementData.class ).getDeclaringClass();
        }
        catch ( Exception e )
        {
            implClass = ManagementBeanProvider.class; // Assume no override
        }
        if ( implClass != ManagementBeanProvider.class )
        { // if createMBeans is overridden, delegate to it
            return createMBeans( management );
        }
        else
        { // otherwise delegate to the createMXBean method and create a list
            return singletonOrNone( createMXBean( management ) );
        }
    }

    protected abstract Neo5jMBean createMBean( ManagementData management ) throws NotCompliantMBeanException;

    protected Neo5jMBean createMXBean( ManagementData management ) throws NotCompliantMBeanException
    {
        return createMBean( management );
    }

    final Iterable<? extends Neo5jMBean> loadBeans( KernelData kernel, ManagementSupport support )
            throws Exception
    {
        if ( support.supportsMxBeans() )
        {
            return createMXBeans( new ManagementData( this, kernel, support ) );
        }
        else
        {
            return createMBeans( new ManagementData( this, kernel, support ) );
        }
    }

    private static Collection<? extends Neo5jMBean> singletonOrNone( Neo5jMBean mbean )
    {
        if ( mbean == null )
        {
            return Collections.emptySet();
        }
        return Collections.singleton( mbean );
    }
}
