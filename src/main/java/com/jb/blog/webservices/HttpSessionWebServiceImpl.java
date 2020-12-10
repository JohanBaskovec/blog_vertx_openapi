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
    private final PgPool pool;
    private final JsonMapper<LoginForm> loginFormMapper;
    private final HttpSessionRepository httpSessionRepository;
    private final SessionConfiguration sessionConfiguration;
    private final JsonMapper<User> userJsonMapper;
    private final OperationRequestService operationRequestService;
    private final UserRepository userRepository;

    public HttpSessionWebServiceImpl(
            PgPool pool,
            JsonMapper<LoginForm> loginFormMapper,
            HttpSessionRepository httpSessionRepository,
            SessionConfiguration sessionConfiguration,
            JsonMapper<User> userJsonMapper,
            OperationRequestService operationRequestService,
            UserRepository userRepository
    ) {
        this.pool = pool;
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
        LoginForm loginForm = loginFormMapper.fromJson(body);
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                handler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection connection = getConnectionResult.result();

            // TODO: password hashing
            userRepository.getUserById(connection, loginForm.getUsername(), (getUserByIdResult) -> {
                if (getUserByIdResult.failed()) {
                    connection.close();
                    handler.handle(Future.failedFuture(getUserByIdResult.cause()));
                    return;
                }

                User user = getUserByIdResult.result();
                if (user == null) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(400);
                    connection.close();
                    handler.handle(Future.succeededFuture(operationResponse));
                    return;
                }

                if (!user.getPassword().equals(loginForm.getPassword())) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(400);
                    connection.close();
                    handler.handle(Future.succeededFuture(operationResponse));
                    return;
                }

                Session session = httpSessionRepository.createSession();
                session.put("username", user.getUsername());
                httpSessionRepository.putSession(session, putSessionResult -> {
                    if (putSessionResult.failed()) {
                        connection.close();
                        handler.handle(Future.failedFuture(putSessionResult.cause()));
                        return;
                    }
                    OperationResponse operationResponse = new OperationResponse();
                    Cookie cookie = Cookie.cookie(sessionConfiguration.sessionCookieName, session.id());
                    cookie.setPath(sessionConfiguration.sessionCookiePath);
                    cookie.setSecure(sessionConfiguration.sessionCookieSecure);
                    cookie.setHttpOnly(sessionConfiguration.sessionCookieHttpOnly);
                    cookie.setSameSite(sessionConfiguration.cookieSameSite);
                    operationResponse.putHeader("Set-Cookie", cookie.encode());
                    operationResponse.setStatusCode(200);
                    operationResponse.setPayload(userJsonMapper.toJson(user).toBuffer());

                    connection.close();
                    handler.handle(Future.succeededFuture(operationResponse));
                });
            });
        });
    }

    @Override
    public void getCurrentAuthenticatedUser(
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        httpSessionRepository.getFromOperationRequest(operationRequest, getSessionResult -> {
            if (getSessionResult.failed()) {
                handler.handle(Future.failedFuture(getSessionResult.cause()));
                return;
            }
            Session session = getSessionResult.result();
            if (session == null) {
                OperationResponse operationResponse = createLogoutResponse(operationRequest);
                operationResponse.setStatusCode(401);
                handler.handle(Future.succeededFuture(operationResponse));
                return;
            }
            pool.getConnection(getConnectionResult -> {
                if (getConnectionResult.failed()) {
                    handler.handle(Future.failedFuture(getConnectionResult.cause()));
                    return;
                }
                SqlClient connection = getConnectionResult.result();
                String username = session.get("username");
                userRepository.getUserById(connection, username, getUserResult -> {
                    if (getUserResult.failed()) {
                        connection.close();
                        handler.handle(Future.failedFuture(getUserResult.cause()));
                        return;
                    }
                    User user = getUserResult.result();
                    if (user == null) {
                        httpSessionRepository.delete(session, deleteResult -> {
                            if (deleteResult.failed()) {
                                connection.close();
                                handler.handle(Future.failedFuture(deleteResult.cause()));
                                return;
                            }
                            OperationResponse operationResponse = createLogoutResponse(operationRequest);
                            operationResponse.setStatusCode(401);
                            connection.close();
                            handler.handle(Future.succeededFuture(operationResponse));
                        });
                        return;
                    }

                    connection.close();
                    handler.handle(Future.succeededFuture(
                            OperationResponse.completedWithJson(userJsonMapper.toJson(user))
                    ));
                });
            });
        });
    }

    @Override
    public void logout(OperationRequest operationRequest, Handler<AsyncResult<OperationResponse>> handler) {
        httpSessionRepository.getFromOperationRequest(operationRequest, getSessionResult -> {
            if (getSessionResult.failed()) {
                handler.handle(Future.failedFuture(getSessionResult.cause()));
                return;
            }

            Session session = getSessionResult.result();
            if (session == null) {
                OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatusCode(401);
                handler.handle(Future.succeededFuture(operationResponse));
                return;
            }
            httpSessionRepository.delete(session, deleteSessionResult -> {
                if (deleteSessionResult.failed()) {
                    handler.handle(Future.failedFuture(deleteSessionResult.cause()));
                    return;
                }
                OperationResponse operationResponse = createLogoutResponse(operationRequest);
                handler.handle(Future.succeededFuture(operationResponse));
            });
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
