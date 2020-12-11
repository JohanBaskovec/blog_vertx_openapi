package com.jb.blog.webservices;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.services.JsonMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import jdk.dynalink.Operation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.vertxweb.server.model.User;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class RequestContextManagerTest {
    private OperationRequest operationRequest;
    private PgPool pgPool;
    private HttpSessionRepository httpSessionRepository;
    private UserRepository userRepository;
    private Handler<AsyncResult<OperationResponse>> operationResponseHandler;
    private RequestContextManager requestContextManager;
    private Handler<RequestContext> handler;
    private ArgumentCaptor<Future<OperationResponse>> operationResponseArgumentCaptor;
    JsonMapper<User> userJsonMapper;

    @Before
    public void setUp() {
        userJsonMapper = mock(JsonMapper.class);
        operationResponseArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        handler = mock(Handler.class);
        operationRequest = mock(OperationRequest.class);
        pgPool = mock(PgPool.class);
        httpSessionRepository = mock(HttpSessionRepository.class);
        userRepository = mock(UserRepository.class);
        operationResponseHandler = mock(Handler.class);
        requestContextManager = new RequestContextManager(operationRequest, pgPool, httpSessionRepository, userRepository, operationResponseHandler);
    }

    @Test
    public void getContextWithoutUser() {
    }

    @Test
    public void getContextWithUserWhenErrorWhenGettingSession() {
        // arrange
        Exception exception = new RuntimeException();
        mockGetSessionFromOperationRequest(Future.failedFuture(exception));

        // act
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getContextWithUserWhenNoSessionButCookie() {
        // arrange
        mockGetSessionFromOperationRequest(Future.succeededFuture(null));
        ServerCookie cookie = mockRequestCookies();

        // act
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
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
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
        assertTrue(future.succeeded());
        OperationResponse operationResponse = future.result();
        assertEquals(401, (int)operationResponse.getStatusCode());
        assertEquals("", operationResponse.getHeaders().toString());
    }

    @Test
    public void getContextWithUserWhenErrorWhenGettingConnection() {
        // arrange
        Session session = mockSuccessfulGetSessionFromOperationRequest("username");
        Exception exception = mockFailureWhenGettingConnection();

        // act
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
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
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
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
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
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
        requestContextManager.getContextWithUser(handler);

        // assert
        verify(operationResponseHandler).handle(operationResponseArgumentCaptor.capture());
        Future<OperationResponse> future = operationResponseArgumentCaptor.getValue();
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
        requestContextManager.getContextWithUser(handler);

        // assert
        ArgumentCaptor<RequestContext> requestContextArgumentCaptor = ArgumentCaptor.forClass(RequestContext.class);
        verify(handler).handle(requestContextArgumentCaptor.capture());
        RequestContext requestContext = requestContextArgumentCaptor.getValue();
        assertSame(connection, requestContext.getSqlConnection());
        assertSame(session, requestContext.getSession());
        assertSame(user, requestContext.getUser());
        assertSame(operationResponseHandler, requestContext.getOperationResponseHandler());
    }

    private void mockPutSession(Session session, AsyncResult<Void> result) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Void>> handler = invocationOnMock.getArgument(1);
            handler.handle(result);
            return null;
        }).when(httpSessionRepository).putSession(eq(session), any(Handler.class));
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
