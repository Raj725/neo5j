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
package org.neo5j.server.rest.dbms;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.neo5j.kernel.api.exceptions.InvalidArgumentsException;
import org.neo5j.kernel.api.security.AuthenticationResult;
import org.neo5j.kernel.api.security.SecurityContext;
import org.neo5j.server.rest.repr.OutputFormat;
import org.neo5j.server.rest.repr.formats.JsonFormat;
import org.neo5j.server.security.auth.AuthenticationStrategy;
import org.neo5j.server.security.auth.BasicAuthManager;
import org.neo5j.server.security.auth.BasicSecurityContext;
import org.neo5j.server.security.auth.BasicPasswordPolicy;
import org.neo5j.kernel.impl.security.Credential;
import org.neo5j.server.security.auth.InMemoryUserRepository;
import org.neo5j.kernel.api.security.PasswordPolicy;
import org.neo5j.kernel.impl.security.User;
import org.neo5j.kernel.api.security.UserManager;
import org.neo5j.kernel.api.security.UserManagerSupplier;
import org.neo5j.server.security.auth.UserRepository;
import org.neo5j.test.server.EntityOutputFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserServiceTest
{
    protected static final User NEO5J_USER = new User.Builder( "neo5j", Credential.forPassword( "neo5j" ))
            .withRequiredPasswordChange( true ).build();

    protected final PasswordPolicy passwordPolicy = new BasicPasswordPolicy();
    protected final UserRepository userRepository = new InMemoryUserRepository();

    protected UserManagerSupplier userManagerSupplier;
    protected SecurityContext neo5jContext;
    protected Principal neo5jPrinciple;

    protected void setupAuthManagerAndSubject()
    {
        BasicAuthManager basicAuthManager = new BasicAuthManager( userRepository, passwordPolicy,
                mock( AuthenticationStrategy.class), new InMemoryUserRepository() );

        userManagerSupplier = basicAuthManager;
        neo5jContext = new BasicSecurityContext( basicAuthManager, NEO5J_USER, AuthenticationResult.SUCCESS );
    }

    @Before
    public void setUp() throws InvalidArgumentsException, IOException
    {
        userRepository.create( NEO5J_USER );
        setupAuthManagerAndSubject();
        neo5jPrinciple = new DelegatingPrincipal( "neo5j", neo5jContext );
    }

    @After
    public void tearDown() throws InvalidArgumentsException, IOException
    {
        userRepository.delete( NEO5J_USER );
    }

    @Test
    public void shouldReturnValidUserRepresentation() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.getUser( "neo5j", req );

        // Then
        assertThat( response.getStatus(), equalTo( 200 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"username\" : \"neo5j\"" ) );
        assertThat( json, containsString( "\"password_change\" : \"http://www.example.com/user/neo5j/password\"" ) );
        assertThat( json, containsString( "\"password_change_required\" : true" ) );
    }

    @Test
    public void shouldReturn404WhenRequestingUserIfNotAuthenticated() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( null );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.getUser( "neo5j", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldReturn404WhenRequestingUserIfDifferentUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( mock( BasicAuthManager.class ), new JsonFormat(), outputFormat );

        // When
        Response response = userService.getUser( "fred", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldReturn404WhenRequestingUserIfUnknownUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        userManagerSupplier.getUserManager().deleteUser( "neo5j" );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.getUser( "neo5j", req );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldChangePasswordAndReturnSuccess() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : \"test\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 200 ) );
        userManagerSupplier.getUserManager().getUser( "neo5j" ).credentials().matchesPassword( "test" );
    }

    @Test
    public void shouldReturn404WhenChangingPasswordIfNotAuthenticated() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( null );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( mock( BasicAuthManager.class ), new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : \"test\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
    }

    @Test
    public void shouldReturn404WhenChangingPasswordIfDifferentUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        UserManager userManager = mock( UserManager.class );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "fred", req, "{ \"password\" : \"test\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 404 ) );
        verifyZeroInteractions( userManager );
    }

    @Test
    public void shouldReturn422WhenChangingPasswordIfUnknownUser() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        userRepository.delete( NEO5J_USER );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : \"test\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
    }

    @Test
    public void shouldReturn400IfPayloadIsInvalid() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( mock( BasicAuthManager.class ), new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "xxx" );

        // Then
        assertThat( response.getStatus(), equalTo( 400 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
    }

    @Test
    public void shouldReturn422IfMissingPassword() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( mock( BasicAuthManager.class ), new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"unknown\" : \"unknown\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
        assertThat( json, containsString( "\"message\" : \"Required parameter 'password' is missing.\"" ) );
    }

    @Test
    public void shouldReturn422IfInvalidPasswordType() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( mock( BasicAuthManager.class ), new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : 1 }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.Request.InvalidFormat\"" ) );
        assertThat( json, containsString( "\"message\" : \"Expected 'password' to be a string.\"" ) );
    }

    @Test
    public void shouldReturn422IfEmptyPassword() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : \"\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.General.InvalidArguments\"" ) );
        assertThat( json, containsString( "\"message\" : \"A password cannot be empty.\"" ) );
    }

    @Test
    public void shouldReturn422IfPasswordIdentical() throws Exception
    {
        // Given
        HttpServletRequest req = mock( HttpServletRequest.class );
        when( req.getUserPrincipal() ).thenReturn( neo5jPrinciple );

        OutputFormat outputFormat = new EntityOutputFormat( new JsonFormat(), new URI( "http://www.example.com" ), null );
        UserService userService = new UserService( userManagerSupplier, new JsonFormat(), outputFormat );

        // When
        Response response = userService.setPassword( "neo5j", req, "{ \"password\" : \"neo5j\" }" );

        // Then
        assertThat( response.getStatus(), equalTo( 422 ) );
        String json = new String( (byte[]) response.getEntity() );
        assertNotNull( json );
        assertThat( json, containsString( "\"code\" : \"Neo.ClientError.General.InvalidArguments\"" ) );
        assertThat( json, containsString( "\"message\" : \"Old password and new password cannot be the same.\"" ) );
    }
}
