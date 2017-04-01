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
package org.neo5j.server.security.enterprise.auth.integration.bolt;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.neo5j.bolt.v1.transport.integration.TransportTestUtil;
import org.neo5j.graphdb.config.Setting;
import org.neo5j.kernel.api.exceptions.Status;
import org.neo5j.server.security.enterprise.auth.plugin.TestCacheableAuthPlugin;
import org.neo5j.server.security.enterprise.auth.plugin.TestCacheableAuthenticationPlugin;
import org.neo5j.server.security.enterprise.auth.plugin.TestCustomCacheableAuthenticationPlugin;
import org.neo5j.server.security.enterprise.configuration.SecuritySettings;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.neo5j.bolt.v1.messaging.message.PullAllMessage.pullAll;
import static org.neo5j.bolt.v1.messaging.message.RunMessage.run;
import static org.neo5j.bolt.v1.messaging.util.MessageMatchers.msgFailure;
import static org.neo5j.bolt.v1.messaging.util.MessageMatchers.msgSuccess;
import static org.neo5j.bolt.v1.transport.integration.TransportTestUtil.eventuallyReceives;
import static org.neo5j.helpers.collection.MapUtil.map;

public class PluginAuthenticationIT extends EnterpriseAuthenticationTestBase
{
    private static final List<String> defaultTestPluginRealmList = Arrays.asList(
            "TestAuthenticationPlugin",
            "TestAuthPlugin",
            "TestCacheableAdminAuthPlugin",
            "TestCacheableAuthenticationPlugin",
            "TestCacheableAuthPlugin",
            "TestCustomCacheableAuthenticationPlugin",
            "TestCustomParametersAuthenticationPlugin"
    );

    private static final String DEFAULT_TEST_PLUGIN_REALMS = String.join( ", ",
            defaultTestPluginRealmList.stream()
                    .map( s -> StringUtils.prependIfMissing( s, SecuritySettings.PLUGIN_REALM_NAME_PREFIX ) )
                    .collect( Collectors.toList() )
    );

    @Override
    protected Consumer<Map<Setting<?>, String>> getSettingsFunction()
    {
        return super.getSettingsFunction().andThen( settings -> settings.put( SecuritySettings.auth_providers, DEFAULT_TEST_PLUGIN_REALMS ) );
    }

    @Test
    public void shouldAuthenticateWithTestAuthenticationPlugin() throws Throwable
    {
        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestAuthenticationPlugin" ) );
    }

    @Test
    public void shouldAuthenticateWithTestCacheableAuthenticationPlugin() throws Throwable
    {
        Map<String,Object> authToken = authToken( "neo5j", "neo5j",
                "plugin-TestCacheableAuthenticationPlugin" );

        TestCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.set( 0 );

        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_cache_ttl, "60m" ) );

        // When we log in the first time our plugin should get a call
        assertConnectionSucceeds( authToken );
        assertThat( TestCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );

        // When we log in the second time our plugin should _not_ get a call since authentication info should be cached
        reconnect();
        assertConnectionSucceeds( authToken );
        assertThat( TestCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );

        // When we log in the with the wrong credentials it should fail and
        // our plugin should _not_ get a call since authentication info should be cached
        reconnect();
        authToken.put( "credentials", "wrong_password" );
        assertConnectionFails( authToken );
        assertThat( TestCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );
    }

    @Test
    public void shouldAuthenticateWithTestCustomCacheableAuthenticationPlugin() throws Throwable
    {
        Map<String,Object> authToken = authToken( "neo5j", "neo5j",
                "plugin-TestCustomCacheableAuthenticationPlugin" );

        TestCustomCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.set( 0 );

        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_cache_ttl, "60m" ) );

        // When we log in the first time our plugin should get a call
        assertConnectionSucceeds( authToken );
        assertThat( TestCustomCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );

        // When we log in the second time our plugin should _not_ get a call since authentication info should be cached
        reconnect();
        assertConnectionSucceeds( authToken );
        assertThat( TestCustomCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );

        // When we log in the with the wrong credentials it should fail and
        // our plugin should _not_ get a call since authentication info should be cached
        reconnect();
        authToken.put( "credentials", "wrong_password" );
        assertConnectionFails( authToken );
        assertThat( TestCustomCacheableAuthenticationPlugin.getAuthenticationInfoCallCount.get(), equalTo( 1 ) );
    }

    @Test
    public void shouldAuthenticateAndAuthorizeWithTestAuthPlugin() throws Throwable
    {
        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestAuthPlugin" ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );
    }

    @Test
    public void shouldAuthenticateAndAuthorizeWithCacheableTestAuthPlugin() throws Throwable
    {
        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestCacheableAuthPlugin" ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );
    }

    @Test
    public void shouldAuthenticateWithTestCacheableAuthPlugin() throws Throwable
    {
        Map<String,Object> authToken = authToken( "neo5j", "neo5j",
                "plugin-TestCacheableAuthPlugin" );

        TestCacheableAuthPlugin.getAuthInfoCallCount.set( 0 );

        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_cache_ttl, "60m" ) );

        // When we log in the first time our plugin should get a call
        assertConnectionSucceeds( authToken );
        assertThat( TestCacheableAuthPlugin.getAuthInfoCallCount.get(), equalTo( 1 ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );

        // When we log in the second time our plugin should _not_ get a call since auth info should be cached
        reconnect();
        assertConnectionSucceeds( authToken );
        assertThat( TestCacheableAuthPlugin.getAuthInfoCallCount.get(), equalTo( 1 ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );

        // When we log in the with the wrong credentials it should fail and
        // our plugin should _not_ get a call since auth info should be cached
        reconnect();
        authToken.put( "credentials", "wrong_password" );
        assertConnectionFails( authToken );
        assertThat( TestCacheableAuthPlugin.getAuthInfoCallCount.get(), equalTo( 1 ) );
    }

    @Test
    public void shouldAuthenticateAndAuthorizeWithTestCombinedAuthPlugin() throws Throwable
    {
        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_providers, "plugin-TestCombinedAuthPlugin" ) );

        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestCombinedAuthPlugin" ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );
    }

    @Test
    public void shouldAuthenticateAndAuthorizeWithTwoSeparateTestPlugins() throws Throwable
    {
        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_providers,
                "plugin-TestAuthenticationPlugin,plugin-TestAuthorizationPlugin" ) );

        assertConnectionSucceeds( authToken( "neo5j", "neo5j", null ) );
        assertReadSucceeds();
        assertWriteFails( "neo5j", "reader" );
    }

    @Test
    public void shouldFailIfAuthorizationExpiredWithAuthPlugin() throws Throwable
    {
        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_providers, "plugin-TestCacheableAdminAuthPlugin" ) );

        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestCacheableAdminAuthPlugin" ) );
        assertReadSucceeds();

        // When
        client.send( TransportTestUtil.chunk(
                run( "CALL dbms.security.clearAuthCache()" ), pullAll() ) );
        assertThat( client, eventuallyReceives( msgSuccess(), msgSuccess() ) );

        // Then
        client.send( TransportTestUtil.chunk(
                run( "MATCH (n) RETURN n" ), pullAll() ) );
        assertThat( client, eventuallyReceives(
                msgFailure( Status.Security.AuthorizationExpired,
                        "Plugin 'plugin-TestCacheableAdminAuthPlugin' authorization info expired." ) ) );
    }

    @Test
    public void shouldSucceedIfAuthorizationExpiredWithinTransactionWithAuthPlugin() throws Throwable
    {
        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_providers, "plugin-TestCacheableAdminAuthPlugin" ) );

        // Then
        assertConnectionSucceeds( authToken( "neo5j", "neo5j", "plugin-TestCacheableAdminAuthPlugin" ) );

        client.send( TransportTestUtil.chunk(
                run( "CALL dbms.security.clearAuthCache() MATCH (n) RETURN n" ), pullAll() ) );

        // Then
        assertThat( client, eventuallyReceives( msgSuccess(), msgSuccess() ) );
    }

    @Test
    public void shouldAuthenticateWithTestCustomParametersAuthenticationPlugin() throws Throwable
    {
        assertConnectionSucceeds( map(
                "scheme", "custom",
                "principal", "neo5j",
                "realm", "plugin-TestCustomParametersAuthenticationPlugin",
                "parameters", map( "my_credentials", Arrays.asList( 1L, 2L, 3L, 4L ) ) ) );
    }

    @Test
    public void shouldPassOnAuthorizationExpiredException() throws Throwable
    {
        restartNeo5jServerWithOverriddenSettings( settings -> settings.put( SecuritySettings.auth_providers,
                "plugin-TestCombinedAuthPlugin" ) );

        assertConnectionSucceeds( authToken( "authorization_expired_user", "neo5j", null ) );

        // Then
        client.send( TransportTestUtil.chunk(
                run( "MATCH (n) RETURN n" ), pullAll() ) );
        assertThat( client, eventuallyReceives(
                msgFailure( Status.Security.AuthorizationExpired,
                        "Plugin 'plugin-TestCombinedAuthPlugin' authorization info expired: " +
                        "authorization_expired_user needs to re-authenticate." ) ) );
    }
}
