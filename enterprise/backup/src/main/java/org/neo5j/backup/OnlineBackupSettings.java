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
package org.neo5j.backup;

import org.neo5j.configuration.Description;
import org.neo5j.configuration.LoadableConfig;
import org.neo5j.graphdb.config.Setting;
import org.neo5j.helpers.HostnamePort;

import static org.neo5j.kernel.configuration.Settings.BOOLEAN;
import static org.neo5j.kernel.configuration.Settings.HOSTNAME_PORT;
import static org.neo5j.kernel.configuration.Settings.TRUE;
import static org.neo5j.kernel.configuration.Settings.setting;

/**
 * Settings for online backup
 */
@Description( "Online backup configuration settings" )
public class OnlineBackupSettings implements LoadableConfig
{
    @Description("Enable support for running online backups")
    public static final Setting<Boolean> online_backup_enabled = setting( "dbms.backup.enabled", BOOLEAN, TRUE );

    @Description("Listening server for online backups")
    public static final Setting<HostnamePort> online_backup_server = setting( "dbms.backup.address", HOSTNAME_PORT,
            "127.0.0.1:6362-6372" );
}
