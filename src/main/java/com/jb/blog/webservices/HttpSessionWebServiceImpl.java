package com.jb.blog.webservices;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.services.JsonMapper;
import com.jb.blog.services.OperationRequestService;
import com.jb.blog.session.SessionConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnection;
import org.openapitools.vertxweb.server.model.LoginForm;
import org.openapitools.vertxweb.server.model.User;

public class HttpSessionWebServiceImpl implements HttpSessionWebService {
    private final RequestContextManagerFactory requestContextManagerFactory;
    private final JsonMapper<LoginForm> loginFormMapper;
    private final HttpSessionRepository httpSessionRepository;
    private final SessionConfiguration sessionConfiguration;
    private final JsonMapper<User> userJsonMapper;
    private final OperationRequestService operationRequestService;
    private final UserRepository userRepository;

    public HttpSessionWebServiceImpl(
            RequestContextManagerFactory requestContextManagerFactory,
            JsonMapper<LoginForm> loginFormMapper,
            HttpSessionRepository httpSessionRepository,
            SessionConfiguration sessionConfiguration,
            JsonMapper<User> userJsonMapper,
            OperationRequestService operationRequestService,
            UserRepository userRepository
    ) {
        this.requestContextManagerFactory = requestContextManagerFactory;
        this.loginFormMapper = loginFormMapper;
        this.httpSessionRepository = httpSessionRepository;
        this.sessionConfiguration = sessionConfiguration;
        this.userJsonMapper = userJsonMapper;
        this.operationRequestService = operationRequestService;
        this.userRepository = userRepository;
    }

    public void login(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithoutUser(requestContext -> {
            LoginForm loginForm = loginFormMapper.fromJson(body);
            SqlConnection sqlConnection = requestContext.getSqlConnection();
            String username = loginForm.getUsername();

            // TODO: password hashing
            userRepository.getUserById(sqlConnection, username, requestContext.createHandler((user) -> {
                if (user == null || !user.getPassword().equals(loginForm.getPassword())) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(400);
                    requestContext.handleSuccess(operationResponse);
                    return;
                }

                Session session = httpSessionRepository.createSession();
                session.put("username", user.getUsername());
                httpSessionRepository.putSession(session, requestContext.createHandler((Void putSessionResult) -> {
                    OperationResponse operationResponse = new OperationResponse();
                    Cookie cookie = Cookie.cookie(sessionConfiguration.sessionCookieName, session.id());
                    cookie.setPath(sessionConfiguration.sessionCookiePath);
                    cookie.setSecure(sessionConfiguration.sessionCookieSecure);
                    cookie.setHttpOnly(sessionConfiguration.sessionCookieHttpOnly);
                    cookie.setSameSite(sessionConfiguration.cookieSameSite);
                    cookie.setMaxAge(10000000);
                    operationResponse.putHeader("Set-Cookie", cookie.encode());
                    operationResponse.setStatusCode(200);
                    operationResponse.setPayload(userJsonMapper.toJson(user).toBuffer());

                    requestContext.handleSuccess(operationResponse);
                }));
            }));
        });
    }

    @Override
    public void getCurrentAuthenticatedUser(
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithUser(requestContext -> {
            requestContext.handleSuccess(
                    OperationResponse.completedWithJson(userJsonMapper.toJson(requestContext.getUser()))
            );
        });
    }

    @Override
    public void logout(OperationRequest operationRequest, Handler<AsyncResult<OperationResponse>> handler) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithUser(requestContext -> {
            OperationResponse operationResponse = requestContextManager.createLogoutResponse(operationRequest);
            requestContext.handleSuccess(operationResponse);
        });
    }

    private OperationResponse createLogoutResponse(OperationRequest operationRequest) {
        OperationResponse operationResponse = new OperationResponse();
        ServerCookie sessionCookie = httpSessionRepository.getSessionCookie(operationRequest);
        if (sessionCookie != null) {
            sessionCookie.setMaxAge(0L);
            operationResponse.putHeader("Set-Cookie", sessionCookie.encode());
        }
        return operationResponse;
    }
}
