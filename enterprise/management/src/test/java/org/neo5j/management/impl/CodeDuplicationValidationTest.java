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
package org.neo5j.management.impl;

import javax.management.ObjectName;

import org.junit.Test;

import org.neo5j.jmx.Kernel;
import org.neo5j.jmx.Primitives;
import org.neo5j.jmx.StoreFile;
import org.neo5j.jmx.impl.ManagementSupport;
import org.neo5j.management.BranchedStore;
import org.neo5j.management.Cache;
import org.neo5j.management.HighAvailability;
import org.neo5j.management.IndexSamplingManager;
import org.neo5j.management.LockManager;
import org.neo5j.management.MemoryMapping;
import org.neo5j.management.TransactionManager;

import static org.junit.Assert.assertEquals;

public class CodeDuplicationValidationTest
{
    private class DefaultManagementSupport extends ManagementSupport
    {
        @Override
        protected ObjectName createObjectName( String instanceId, String beanName, boolean query, String... extraNaming )
        {
            return super.createObjectName( instanceId, beanName, query, extraNaming );
        }

        @Override
        protected String getBeanName( Class<?> beanInterface )
        {
            return super.getBeanName( beanInterface );
        }
    }

    private class CustomManagementSupport extends AdvancedManagementSupport
    {
        // belongs to this package - no override needed
    }

    @Test
    public void kernelBeanTypeNameMatchesExpected() throws Exception
    {
        assertEquals( Kernel.class.getName(), KernelProxy.KERNEL_BEAN_TYPE );
        assertEquals( Kernel.NAME, KernelProxy.KERNEL_BEAN_NAME );
    }

    @Test
    public void mbeanQueryAttributeNameMatchesMethodName() throws Exception
    {
        assertEquals( ObjectName.class, Kernel.class.getMethod( "get" + KernelProxy.MBEAN_QUERY ).getReturnType() );
    }

    @Test
    public void interfacesGetsTheSameBeanNames() throws Exception
    {
        assertEqualBeanName( Kernel.class );
        assertEqualBeanName( Primitives.class );
        assertEqualBeanName( Cache.class );
        assertEqualBeanName( HighAvailability.class );
        assertEqualBeanName( BranchedStore.class );
        assertEqualBeanName( LockManager.class );
        assertEqualBeanName( MemoryMapping.class );
        assertEqualBeanName( StoreFile.class );
        assertEqualBeanName( TransactionManager.class );
        assertEqualBeanName( IndexSamplingManager.class );
    }

    private void assertEqualBeanName( Class<?> beanClass )
    {
        assertEquals( new DefaultManagementSupport().getBeanName( beanClass ),
                new CustomManagementSupport().getBeanName( beanClass ) );
    }

    @Test
    public void generatesEqualObjectNames() throws Exception
    {
        assertEquals( new DefaultManagementSupport().createMBeanQuery( "test-instance" ),
                new CustomManagementSupport().createMBeanQuery( "test-instance" ) );
        assertEquals( new DefaultManagementSupport().createObjectName( "test-instace", Kernel.class ),
                new CustomManagementSupport().createObjectName( "test-instace", Kernel.class ) );
    }
}
