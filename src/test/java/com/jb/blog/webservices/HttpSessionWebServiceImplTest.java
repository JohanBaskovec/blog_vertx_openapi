package com.jb.blog.webservices;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.services.JsonMapper;
import com.jb.blog.services.OperationRequestService;
import com.jb.blog.session.SessionConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.vertxweb.server.model.LoginForm;
import org.openapitools.vertxweb.server.model.User;


import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HttpSessionWebServiceImplTest {
    HttpSessionWebService httpSessionWebService;
    PgPool pgPool;
    JsonMapper<LoginForm> loginFormMapper;
    HttpSessionRepository httpSessionRepository;
    SessionConfiguration sessionConfiguration;
    JsonMapper<User> userJsonMapper;
    OperationRequestService operationRequestService;
    UserRepository userRepository;
    JsonObject body;
    OperationRequest operationRequest;
    Handler<AsyncResult<OperationResponse>> handler;
    ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        pgPool = mock(PgPool.class);
        loginFormMapper = mock(JsonMapper.class);
        httpSessionRepository = mock(HttpSessionRepository.class);
        sessionConfiguration = SessionConfiguration.createDefault();
        userJsonMapper = mock(JsonMapper.class);
        operationRequestService = mock(OperationRequestService.class);
        userRepository = mock(UserRepository.class);
        handler = mock(Handler.class);
        futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);

        httpSessionWebService = new HttpSessionWebServiceImpl(
                pgPool,
                loginFormMapper,
                httpSessionRepository,
                sessionConfiguration,
                userJsonMapper,
                operationRequestService,
                userRepository);
    }

    @Test
    public void loginWhenErrorWhenGettingConnection() {
        // arrange
        Exception exception = mockFailureWhenGettingConnection();

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void loginWhenErrorWhenGettingUser() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection connection = mockSuccessfulGettingConnection();

        LoginForm loginForm = mockLoginForm();
        mockGetUserById(connection, loginForm.getUsername(), Future.failedFuture(exception));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
        verify(connection).close();
    }

    @Test
    public void loginWhenUserNotFound() {
        // arrange
        SqlConnection connection = mockSuccessfulGettingConnection();

        LoginForm loginForm = mockLoginForm();
        mockGetUserById(connection, loginForm.getUsername(), Future.succeededFuture(null));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(400, (int) operationResponse.getStatusCode());
        verify(connection).close();
    }

    @Test
    public void loginWhenPasswordNotEqual() {
        // arrange
        SqlConnection connection = mockSuccessfulGettingConnection();

        LoginForm loginForm = mockLoginForm();
        User user = new User();
        user.setPassword("different");
        mockGetUserById(connection, loginForm.getUsername(), Future.succeededFuture(user));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(400, (int) operationResponse.getStatusCode());
        verify(connection).close();
    }

    @Test
    public void loginWhenExceptionWhileStartingNewSession() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection connection = mockSuccessfulGettingConnection();
        LoginForm loginForm = mockLoginForm();
        User user = new User();
        user.setPassword(loginForm.getPassword());
        user.setUsername(loginForm.getUsername());
        mockGetUserById(connection, loginForm.getUsername(), Future.succeededFuture(user));
        Session session = mockCreateSession();
        mockPutSession(session, Future.failedFuture(exception));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
        verify(connection).close();
        verify(session).put("username", user.getUsername());
    }

    @Test
    public void loginWhenNoError() {
        // arrange
        Exception exception = new RuntimeException();
        SqlConnection connection = mockSuccessfulGettingConnection();
        LoginForm loginForm = mockLoginForm();
        User user = new User();
        user.setPassword(loginForm.getPassword());
        user.setUsername(loginForm.getUsername());
        mockGetUserById(connection, loginForm.getUsername(), Future.succeededFuture(user));
        Session session = mockCreateSession();
        mockPutSession(session, Future.succeededFuture());
        JsonObject userAsJson = mock(JsonObject.class);
        Buffer userAsJsonBuffer = mock(Buffer.class);
        when(userAsJson.toBuffer()).thenReturn(userAsJsonBuffer);
        when(userJsonMapper.toJson(user)).thenReturn(userAsJson);

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(200, (int) operationResponse.getStatusCode());
        assertSame(userAsJsonBuffer, operationResponse.getPayload());
        assertEquals("Set-Cookie: vertx-web.session=sessionid; Path=/\n", operationResponse.getHeaders().toString());
        verify(session).put("username", user.getUsername());
        verify(connection).close();
    }

    private Session mockCreateSession() {
        Session session = mock(Session.class);
        when(session.id()).thenReturn("sessionid");
        when(httpSessionRepository.createSession()).thenReturn(session);
        return session;
    }

    @Test
    public void getCurrentAuthenticatedUserWhenExceptionWhenGettingSession() {
        // arrange
        Exception exception = new RuntimeException();
        mockGetSessionFromOperationRequest(Future.failedFuture(exception));

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getCurrentAuthenticatedUserWhenNoSessionButCookie() {
        // arrange
        mockGetSessionFromOperationRequest(Future.succeededFuture(null));
        ServerCookie cookie = mockRequestCookies();

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(401, (int)operationResponse.getStatusCode());
        assertEquals("Set-Cookie: " + cookie.encode() + "\n", operationResponse.getHeaders().toString());
        verify(cookie).setMaxAge(0);
    }

    @Test
    public void getCurrentAuthenticatedUserWhenNoSessionAndNoCookie() {
        // arrange
        mockGetSessionFromOperationRequest(Future.succeededFuture(null));

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(401, (int)operationResponse.getStatusCode());
        assertEquals("", operationResponse.getHeaders().toString());
    }

    @Test
    public void getCurrentAuthenticatedUserWhenExceptionWhenGettingConnection() {
        // arrange
        Session session = mockSuccessfulGetSessionFromOperationRequest("username");
        Exception exception = mockFailureWhenGettingConnection();

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getCurrentAuthenticatedUserWhenExceptionWhenGettingUserOfSession() {
        // arrange
        String username = "username";
        Session session = mockSuccessfulGetSessionFromOperationRequest(username);
        SqlConnection connection = mockSuccessfulGettingConnection();
        Exception exception = new RuntimeException();
        mockGetUserById(connection, username, Future.failedFuture(exception));

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
        verify(connection).close();
    }

    @Test
    public void getCurrentAuthenticatedUserWhenUserOfSessionDoesntExistAndSessionDeletionWorks() {
        // arrange
        String username = "username";
        Session session = mockSuccessfulGetSessionFromOperationRequest(username);
        SqlConnection connection = mockSuccessfulGettingConnection();
        mockGetUserById(connection, username, Future.succeededFuture(null));
        doAnswer(invocationOnMock -> {
            Handler<Future<Void>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(httpSessionRepository).delete(eq(session), any(Handler.class));
        ServerCookie cookie = mockRequestCookies();

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(401, (int)operationResponse.getStatusCode());
        verify(connection).close();
        assertEquals("Set-Cookie: " + cookie.encode() + "\n", operationResponse.getHeaders().toString());
        verify(cookie).setMaxAge(0);
    }

    @Test
    public void getCurrentAuthenticatedUserWhenUserOfSessionDoesntExistButDeletionFailed() {
        // arrange
        String username = "username";
        Session session = mockSuccessfulGetSessionFromOperationRequest(username);
        SqlConnection connection = mockSuccessfulGettingConnection();
        mockGetUserById(connection, username, Future.succeededFuture(null));
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<Future<Void>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(httpSessionRepository).delete(eq(session), any(Handler.class));

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
        verify(connection).close();
    }

    @Test
    public void getCurrentAuthenticatedUserWhenNoError() {
        // arrange
        String username = "username";
        Session session = mockSuccessfulGetSessionFromOperationRequest(username);
        SqlConnection connection = mockSuccessfulGettingConnection();
        User user = new User();
        user.setUsername(username);
        mockGetUserById(connection, username, Future.succeededFuture(user));
        JsonObject responseJson = new JsonObject();
        when(userJsonMapper.toJson(user)).thenReturn(responseJson);

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(responseJson.toBuffer(), operationResponse.getPayload());
        verify(connection).close();
    }

    @Test
    public void logout() {
        // TODO
    }

    private void mockGetSessionFromOperationRequest(AsyncResult<Session> result) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Session>> handler = invocationOnMock.getArgument(1);
            handler.handle(result);
            return null;
        }).when(httpSessionRepository).getFromOperationRequest(eq(operationRequest), any(Handler.class));
    }

    private Session mockSuccessfulGetSessionFromOperationRequest(String username) {
        Session session = mock(Session.class);
        when(session.get("username")).thenReturn(username);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Session>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(session));
            return null;
        }).when(httpSessionRepository).getFromOperationRequest(eq(operationRequest), any(Handler.class));
        return session;
    }

    private SqlConnection mockSuccessfulGettingConnection() {
        SqlConnection connection = mock(SqlConnection.class);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(connection));
            return null;
        }).when(pgPool).getConnection(any(Handler.class));
        return connection;
    }

    private void mockGetUserById(SqlConnection connection, String username, AsyncResult<User> userResult) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<User>> handler = invocationOnMock.getArgument(2);
            handler.handle(userResult);
            return null;
        }).when(userRepository).getUserById(eq(connection), eq(username), any(Handler.class));
    }

    private LoginForm mockLoginForm() {
        LoginForm loginForm = new LoginForm();
        loginForm.setUsername("username");
        loginForm.setPassword("password");
        when(loginFormMapper.fromJson(body)).thenReturn(loginForm);
        return loginForm;
    }

    private void mockPutSession(Session session, AsyncResult<Void> result) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Void>> handler = invocationOnMock.getArgument(1);
            handler.handle(result);
            return null;
        }).when(httpSessionRepository).putSession(eq(session), any(Handler.class));
    }

    private Exception mockFailureWhenGettingConnection() {
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(pgPool).getConnection(any(Handler.class));
        return exception;
    }

    private ServerCookie mockRequestCookies() {
        ServerCookie serverCookie = mock(ServerCookie.class);
        String cookieAsString = "toto";
        when(serverCookie.encode()).thenReturn(cookieAsString);
        when(httpSessionRepository.getSessionCookie(operationRequest)).thenReturn(serverCookie);
        return serverCookie;
    }
}
