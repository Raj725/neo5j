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
package org.neo5j.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.neo5j.graphdb.config.BaseSetting;
import org.neo5j.graphdb.config.Setting;
import org.neo5j.graphdb.config.SettingGroup;

/**
 * Describes one or several configuration options.
 */
public class ConfigOptions
{
    private final SettingGroup<?> settingGroup;

    public ConfigOptions( @Nonnull SettingGroup<?> settingGroup )
    {
        this.settingGroup = settingGroup;
    }

    @Nonnull
    public SettingGroup<?> settingGroup()
    {
        return settingGroup;
    }

    @Nonnull
    public List<ConfigValue> asConfigValues( @Nonnull Map<String,String> validConfig )
    {
        Map<String,Setting<?>> settings = settingGroup.settings( validConfig ).stream()
                .collect( Collectors.toMap( Setting::name, s -> s ) );

        return settingGroup.values( validConfig ).entrySet().stream()
                .map( val ->
                {
                    BaseSetting<?> setting = (BaseSetting) settings.get( val.getKey() );
                    return new ConfigValue( setting.name(), setting.description(),
                            setting.documentedDefaultValue(),
                        Optional.ofNullable( val.getValue() ),
                            setting.valueDescription(), setting.internal(),
                            setting.deprecated(), setting.replacement() );
                } )
                .collect( Collectors.toList() );
    }
}
