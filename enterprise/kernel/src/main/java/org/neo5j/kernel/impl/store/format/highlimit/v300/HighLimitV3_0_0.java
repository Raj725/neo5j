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
package org.neo5j.kernel.impl.store.format.highlimit.v300;

import org.neo5j.kernel.impl.store.format.BaseRecordFormats;
import org.neo5j.kernel.impl.store.format.Capability;
import org.neo5j.kernel.impl.store.format.FormatFamily;
import org.neo5j.kernel.impl.store.format.RecordFormat;
import org.neo5j.kernel.impl.store.format.RecordFormats;
import org.neo5j.kernel.impl.store.format.StoreVersion;
import org.neo5j.kernel.impl.store.format.highlimit.DynamicRecordFormat;
import org.neo5j.kernel.impl.store.format.highlimit.HighLimitFormatFamily;
import org.neo5j.kernel.impl.store.format.standard.LabelTokenRecordFormat;
import org.neo5j.kernel.impl.store.format.standard.PropertyKeyTokenRecordFormat;
import org.neo5j.kernel.impl.store.format.standard.RelationshipTypeTokenRecordFormat;
import org.neo5j.kernel.impl.store.record.DynamicRecord;
import org.neo5j.kernel.impl.store.record.LabelTokenRecord;
import org.neo5j.kernel.impl.store.record.NodeRecord;
import org.neo5j.kernel.impl.store.record.PropertyKeyTokenRecord;
import org.neo5j.kernel.impl.store.record.PropertyRecord;
import org.neo5j.kernel.impl.store.record.RelationshipGroupRecord;
import org.neo5j.kernel.impl.store.record.RelationshipRecord;
import org.neo5j.kernel.impl.store.record.RelationshipTypeTokenRecord;

/**
 * Record format with very high limits, 50-bit per ID, while at the same time keeping store size small.
 *
 * NOTE: this format is also vE.H.0, but it's the first incarnation of it, without fixed references.
 * The reason the same store version was kept when introducing fixed references was to avoid migration
 * because the change was backwards compatible. Although this turned out to be a mistake because the
 * format isn't forwards compatible and the way we prevent downgrading a db is by using store version,
 * therefore we cannot prevent opening a db with fixed reference format on a neo5j patch version before
 * fixed references were introduced (3.0.4).
 *
 * @see BaseHighLimitRecordFormatV3_0_0
 */
public class HighLimitV3_0_0 extends BaseRecordFormats
{
    /**
     * Default maximum number of bits that can be used to represent id
     */
    static final int DEFAULT_MAXIMUM_BITS_PER_ID = 50;

    public static final String STORE_VERSION = StoreVersion.HIGH_LIMIT_V3_0_0.versionString();
    public static final RecordFormats RECORD_FORMATS = new HighLimitV3_0_0();
    public static final String NAME = "high_limitV3_0_0";

    public HighLimitV3_0_0()
    {
        super( STORE_VERSION, StoreVersion.HIGH_LIMIT_V3_0_0.introductionVersion(), 1, Capability.DENSE_NODES,
                Capability.SCHEMA, Capability.LUCENE_5 );
    }

    @Override
    public RecordFormat<NodeRecord> node()
    {
        return new NodeRecordFormatV3_0_0();
    }

    @Override
    public RecordFormat<RelationshipRecord> relationship()
    {
        return new RelationshipRecordFormatV3_0_0();
    }

    @Override
    public RecordFormat<RelationshipGroupRecord> relationshipGroup()
    {
        return new RelationshipGroupRecordFormatV3_0_0();
    }

    @Override
    public RecordFormat<PropertyRecord> property()
    {
        return new PropertyRecordFormatV3_0_0();
    }

    @Override
    public RecordFormat<LabelTokenRecord> labelToken()
    {
        return new LabelTokenRecordFormat();
    }

    @Override
    public RecordFormat<PropertyKeyTokenRecord> propertyKeyToken()
    {
        return new PropertyKeyTokenRecordFormat();
    }

    @Override
    public RecordFormat<RelationshipTypeTokenRecord> relationshipTypeToken()
    {
        return new RelationshipTypeTokenRecordFormat();
    }

    @Override
    public RecordFormat<DynamicRecord> dynamic()
    {
        return new DynamicRecordFormat();
    }

    @Override
    public FormatFamily getFormatFamily()
    {
        return HighLimitFormatFamily.INSTANCE;
    }
}
