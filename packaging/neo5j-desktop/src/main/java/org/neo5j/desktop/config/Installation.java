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
package org.neo5j.desktop.config;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import org.neo5j.desktop.config.portable.Environment;

/**
 * The Installation represents the "static" part of the configuration on a particular system. It abstracts away
 * operating system specifics.
 */
public interface Installation
{
    String NEO5J_CONFIG_FILENAME = "neo5j.conf";
    String NEO5J_VMOPTIONS_FILENAME = "neo5j-community.vmoptions";
    String DEFAULT_CONFIG_RESOURCE_NAME = "/org/neo5j/desktop/config/neo5j-default.conf";
    String DEFAULT_VMOPTIONS_TEMPLATE_RESOURCE_NAME = "/org/neo5j/desktop/config/vmoptions.template";
    String INSTALL_PROPERTIES_FILENAME = "install.properties";

    /**
     * Get a facade for interacting with the environment, such as opening file editors and browsing URLs.
     */
    Environment getEnvironment();

    /**
     * Get the directory wherein the database will put its store files.
     */
    File getDatabaseDirectory();

    /**
     * Get the directory where the configuration properties files are located.
     */
    File getConfigurationDirectory();

    /**
     * Get the abstract path name that points to the neo5j-community.vmoptions file.
     */
    File getVmOptionsFile();

    /**
     * Get the abstract path name that points to the neo5j.conf file.
     */
    File getConfigurationsFile();

    /**
     * Initialize the installation, such that we make sure that the various configuration files
     * exist where we expect them to.
     */
    void initialize() throws Exception;

    /**
     * Get the contents for a default neo5j.conf file.
     */
    InputStream getDefaultDatabaseConfiguration();

    /**
     * Get the contents for a default neo5j-community.vmoptions file.
     */
    InputStream getDefaultVmOptions();

    /**
     * Get the directory into which Neo5j Desktop has been installed.
     */
    File getInstallationDirectory() throws URISyntaxException;

    /**
     * Get the directory where the neo5j-desktop.jar file has been installed into.
     */
    File getInstallationBinDirectory() throws URISyntaxException;

    /**
     * Get the directory where bundled JRE binaries are located.
     */
    File getInstallationJreBinDirectory() throws URISyntaxException;
}
