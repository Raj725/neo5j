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
package org.neo5j.test.rule;

import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.helpers.collection.MapUtil;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.io.pagecache.PageSwapperFactory;
import org.neo5j.io.pagecache.tracing.PageCacheTracer;
import org.neo5j.io.pagecache.tracing.cursor.DefaultPageCursorTracerSupplier;
import org.neo5j.io.pagecache.tracing.cursor.PageCursorTracerSupplier;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.pagecache.ConfiguringPageCacheFactory;
import org.neo5j.logging.FormattedLogProvider;

public class ConfigurablePageCacheRule extends PageCacheRule
{
    public PageCache getPageCache( FileSystemAbstraction fs, Config config )
    {
        return getPageCache( fs, config(), config );
    }

    public PageCache getPageCache( FileSystemAbstraction fs, PageCacheConfig pageCacheConfig, Config config )
    {
        closeExistingPageCache();
        pageCache = createPageCache( fs, pageCacheConfig, config );
        pageCachePostConstruct( pageCacheConfig );
        return pageCache;
    }

    private PageCache createPageCache( FileSystemAbstraction fs, PageCacheConfig pageCacheConfig, Config config )
    {
        PageCacheTracer tracer = selectConfig( baseConfig.tracer, pageCacheConfig.tracer, PageCacheTracer.NULL );
        PageCursorTracerSupplier cursorTracerSupplier = selectConfig( baseConfig.pageCursorTracerSupplier,
                pageCacheConfig.pageCursorTracerSupplier, DefaultPageCursorTracerSupplier.INSTANCE );
        Config finalConfig = config.withDefaults( MapUtil.stringMap(
                GraphDatabaseSettings.pagecache_memory.name(), "8M" ) );
        FormattedLogProvider logProvider = FormattedLogProvider.toOutputStream( System.err );
        ConfiguringPageCacheFactory pageCacheFactory = new ConfiguringPageCacheFactory(
                fs, finalConfig, tracer, cursorTracerSupplier, logProvider.getLog( PageCache.class ) )
        {
            @Override
            public int calculatePageSize( Config config, PageSwapperFactory swapperFactory )
            {
                if ( pageCacheConfig.pageSize != null )
                {
                    return pageCacheConfig.pageSize;
                }
                return super.calculatePageSize( config, swapperFactory );
            }
        };
        return pageCacheFactory.getOrCreatePageCache();
    }
}
