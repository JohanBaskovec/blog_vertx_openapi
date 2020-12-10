package com.jb.blog.webservices;

import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.services.JsonMapper;
import com.jb.blog.services.RegistrationFormService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.vertxweb.server.model.RegistrationForm;
import org.openapitools.vertxweb.server.model.User;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UserWebServiceImplTest {
    PgPool pgPool;
    UserRepository userRepository;
    JsonMapper<RegistrationForm> registrationFormJsonMapper;
    UserWebService userWebService;
    JsonObject body;
    OperationRequest operationRequest;
    Handler<AsyncResult<OperationResponse>> handler;
    ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor;
    RegistrationFormService registrationFormService;

    @Before
    public void before() {
        pgPool = mock(PgPool.class);
        userRepository = mock(UserRepository.class);
        registrationFormJsonMapper = mock(JsonMapper.class);
        registrationFormService = mock(RegistrationFormService.class);
        userWebService = new UserWebServiceImpl(
                pgPool,
                userRepository,
                registrationFormJsonMapper,
                registrationFormService
        );
        operationRequest = mock(OperationRequest.class);
        handler = mock(Handler.class);
        futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
    }

    @Test
    public void registerWhenExceptionWhenGettingConnection() {
        // arrange
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(pgPool).getConnection(any(Handler.class));

        // act
        userWebService.register(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void registerWhenExceptionWhenGettingUserWithSameUsername() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection sqlConnection = mock(SqlConnection.class);
        mockGettingConnection(sqlConnection);
        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setUsername("username");

        when(registrationFormJsonMapper.fromJson(body)).thenReturn(registrationForm);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<User>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(userRepository).getUserById(eq(sqlConnection), eq(registrationForm.getUsername()), any(Handler.class));

        // act
        userWebService.register(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
        verify(sqlConnection).close();
    }

    @Test
    public void registerWhenUserWithSameUsernameIsNotNull() {
        // arrange
        SqlConnection sqlConnection = mock(SqlConnection.class);
        mockGettingConnection(sqlConnection);

        RegistrationForm registrationForm = mockRegistrationForm();
        User user = new User();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<User>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(user));
            return null;
        }).when(userRepository).getUserById(eq(sqlConnection), eq(registrationForm.getUsername()), any(Handler.class));

        // act
        userWebService.register(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(400, (int)operationResponse.getStatusCode());
        verify(sqlConnection).close();
    }

    @Test
    public void registerWhenExceptionWhenInsertingNewUser() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection sqlConnection = mock(SqlConnection.class);
        mockGettingConnection(sqlConnection);
        RegistrationForm registrationForm = mockRegistrationForm();
        mockGettingUserReturnsNull(registrationForm.getUsername());
        User user = new User();
        when(registrationFormService.toUser(registrationForm)).thenReturn(user);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(userRepository).insert(any(SqlClient.class), eq(user), any(Handler.class));

        // act
        userWebService.register(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
        verify(sqlConnection).close();
    }

    @Test
    public void registerWhenNoError() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection sqlConnection = mock(SqlConnection.class);
        mockGettingConnection(sqlConnection);
        RegistrationForm registrationForm = mockRegistrationForm();
        mockGettingUserReturnsNull(registrationForm.getUsername());
        User user = new User();
        when(registrationFormService.toUser(registrationForm)).thenReturn(user);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(userRepository).insert(any(SqlClient.class), eq(user), any(Handler.class));

        // act
        userWebService.register(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(204, (int)operationResponse.getStatusCode());
        verify(sqlConnection).close();
    }

    private void mockGettingConnection(SqlConnection sqlConnection) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(sqlConnection));
            return null;
        }).when(pgPool).getConnection(any(Handler.class));
    }

    private void mockGettingUserReturnsNull(String username) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<User>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(userRepository).getUserById(any(SqlClient.class), eq(username), any(Handler.class));
    }

    private RegistrationForm mockRegistrationForm() {
        RegistrationForm registrationForm = new RegistrationForm();
        registrationForm.setUsername("username");

        when(registrationFormJsonMapper.fromJson(body)).thenReturn(registrationForm);
        return registrationForm;
    }
}
