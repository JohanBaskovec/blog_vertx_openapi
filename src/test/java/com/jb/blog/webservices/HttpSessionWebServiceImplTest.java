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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
    ArgumentCaptor<OperationResponse> operationResponseArgumentCaptor;
    private RequestContextManagerFactory requestContextManagerFactory;
    private RequestContextManager requestContextManager;
    private RequestContext requestContext;
    private SqlConnection sqlConnection;
    User user = new User();

    @Before
    public void setUp() throws Exception {
        sqlConnection = mock(SqlConnection.class);
        pgPool = mock(PgPool.class);
        loginFormMapper = mock(JsonMapper.class);
        httpSessionRepository = mock(HttpSessionRepository.class);
        sessionConfiguration = SessionConfiguration.createDefault();
        userJsonMapper = mock(JsonMapper.class);
        operationRequestService = mock(OperationRequestService.class);
        userRepository = mock(UserRepository.class);
        handler = mock(Handler.class);
        operationResponseArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        requestContext = mock(RequestContext.class);
        operationRequest = mock(OperationRequest.class);
        requestContextManager = mock(RequestContextManager.class);
        requestContextManagerFactory = mock(RequestContextManagerFactory.class);
        when(requestContextManagerFactory.create(operationRequest, handler)).thenReturn(requestContextManager);
        user.setUsername("toto");

        httpSessionWebService = new HttpSessionWebServiceImpl(
                requestContextManagerFactory,
                loginFormMapper,
                httpSessionRepository,
                sessionConfiguration,
                userJsonMapper,
                operationRequestService,
                userRepository);
    }

    private void mockRequestContextWithConnection() {
        when(requestContext.getSqlConnection()).thenReturn(sqlConnection);
        when(requestContext.createHandler(any(Handler.class))).thenAnswer(new Answer<Handler>() {
            @Override
            public Handler answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler handler = invocationOnMock.getArgument(0);
                return new Handler<AsyncResult>() {
                    @Override
                    public void handle(AsyncResult event) {
                        handler.handle(event.result());
                    }
                };
            }
        });
    }

    private void mockGetContextWithoutUser() {
        mockRequestContextWithConnection();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler<RequestContext> handler = invocationOnMock.getArgument(0);
                handler.handle(requestContext);
                return null;
            }
        }).when(requestContextManager).getContextWithoutUser(any(Handler.class));
    }

    private void mockGetContextWithUser() {
        mockRequestContextWithConnection();
        when(requestContext.getUser()).thenReturn(user);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler<RequestContext> handler = invocationOnMock.getArgument(0);
                handler.handle(requestContext);
                return null;
            }
        }).when(requestContextManager).getContextWithUser(any(Handler.class));
    }


    @Test
    public void loginWhenUserNotFound() {
        // arrange
        mockGetContextWithoutUser();
        LoginForm loginForm = mockLoginForm();
        mockGetUserById(sqlConnection, loginForm.getUsername(), Future.succeededFuture(null));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(400, (int) operationResponse.getStatusCode());
    }

    @Test
    public void loginWhenPasswordNotEqual() {
        // arrange
        mockGetContextWithoutUser();
        LoginForm loginForm = mockLoginForm();
        User user = new User();
        user.setPassword("different");
        mockGetUserById(sqlConnection, loginForm.getUsername(), Future.succeededFuture(user));

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(400, (int) operationResponse.getStatusCode());
    }

    @Test
    public void loginWhenNoError() {
        // arrange
        mockGetContextWithoutUser();
        LoginForm loginForm = mockLoginForm();
        User user = new User();
        user.setPassword(loginForm.getPassword());
        user.setUsername(loginForm.getUsername());
        mockGetUserById(sqlConnection, loginForm.getUsername(), Future.succeededFuture(user));
        Session session = mockCreateSession();
        mockPutSession(session, Future.succeededFuture());
        JsonObject userAsJson = mock(JsonObject.class);
        Buffer userAsJsonBuffer = mock(Buffer.class);
        when(userAsJson.toBuffer()).thenReturn(userAsJsonBuffer);
        when(userJsonMapper.toJson(user)).thenReturn(userAsJson);

        // act
        httpSessionWebService.login(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(200, (int) operationResponse.getStatusCode());
        assertSame(userAsJsonBuffer, operationResponse.getPayload());
        String headers = operationResponse.getHeaders().toString();
        String regex = "Set-Cookie: vertx-web\\.session=sessionid; Max-Age=10000000; Expires=.*; Path=/\n";
        boolean match = headers.matches(regex);
        assertTrue(match);
        verify(session).put("username", user.getUsername());
    }

    private Session mockCreateSession() {
        Session session = mock(Session.class);
        when(session.id()).thenReturn("sessionid");
        when(httpSessionRepository.createSession()).thenReturn(session);
        return session;
    }

    @Test
    public void getCurrentAuthenticatedUserWhenNoError() {
        // arrange
        mockGetContextWithUser();
        JsonObject responseJson = new JsonObject();
        when(userJsonMapper.toJson(user)).thenReturn(responseJson);

        // act
        httpSessionWebService.getCurrentAuthenticatedUser(operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(responseJson.toBuffer(), operationResponse.getPayload());
    }

    @Test
    public void logout() {
        // TODO
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
}
