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
package org.neo5j.server.enterprise;

import org.neo5j.configuration.Description;
import org.neo5j.configuration.LoadableConfig;
import org.neo5j.graphdb.config.Setting;

import static org.neo5j.kernel.configuration.Settings.BOOLEAN;
import static org.neo5j.kernel.configuration.Settings.DURATION;
import static org.neo5j.kernel.configuration.Settings.STRING;
import static org.neo5j.kernel.configuration.Settings.TRUE;
import static org.neo5j.kernel.configuration.Settings.setting;

@Description("Settings available in the Enterprise server")
public class EnterpriseServerSettings implements LoadableConfig
{
    @SuppressWarnings("unused") // accessed from the browser
    @Description( "Configure the Neo5j Browser to time out logged in users after this idle period. " +
                  "Setting this to 0 indicates no limit." )
    public static final Setting<Long> browser_credentialTimeout = setting( "browser.credential_timeout", DURATION, "0" );

    @SuppressWarnings("unused") // accessed from the browser
    @Description( "Configure the Neo5j Browser to store or not store user credentials." )
    public static final Setting<Boolean> browser_retainConnectionCredentials = setting( "browser.retain_connection_credentials", BOOLEAN, TRUE );

    @SuppressWarnings("unused") // accessed from the browser
    @Description( "Whitelist of hosts for the Neo5j Browser to be allowed to fetch content from." )
    public static final Setting<String> browser_remoteContentHostnameWhitelist = setting( "browser.remote_content_hostname_whitelist", STRING, "http://guides.neo5j.com,https://guides.neo5j.com,http://localhost,https://localhost" );

    @SuppressWarnings("unused") // accessed from the browser
    @Description( "Configure the policy for outgoing Neo5j Browser connections." )
    public static final Setting<Boolean> browser_allowOutgoingBrowserConnections = setting( "browser.allow_outgoing_connections", BOOLEAN, TRUE );
}
