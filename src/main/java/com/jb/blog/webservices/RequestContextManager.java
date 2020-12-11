package com.jb.blog.webservices;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.user.UserRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.openapitools.vertxweb.server.model.User;

public class RequestContextManager {
    private final OperationRequest operationRequest;
    private final PgPool pool;
    private final HttpSessionRepository httpSessionRepository;
    private final UserRepository userRepository;
    private final Handler<AsyncResult<OperationResponse>> operationResponseHandler;

    public RequestContextManager(
            OperationRequest operationRequest,
            PgPool pool,
            HttpSessionRepository httpSessionRepository,
            UserRepository userRepository,
            Handler<AsyncResult<OperationResponse>> operationResponseHandler) {
        this.operationRequest = operationRequest;
        this.pool = pool;
        this.httpSessionRepository = httpSessionRepository;
        this.userRepository = userRepository;
        this.operationResponseHandler = operationResponseHandler;
    }

    public void getContextWithoutUser(Handler<RequestContext> handler) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                operationResponseHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection connection = getConnectionResult.result();
            RequestContext requestContext = new RequestContext(connection, null, null, operationResponseHandler);
            handler.handle(requestContext);
        });
    }

    public void getContextWithUser(Handler<RequestContext> handler) {
        httpSessionRepository.getFromOperationRequest(operationRequest, getSessionResult -> {
            if (getSessionResult.failed()) {
                operationResponseHandler.handle(Future.failedFuture(getSessionResult.cause()));
                return;
            }
            Session session = getSessionResult.result();
            if (session == null) {
                OperationResponse operationResponse = createLogoutResponse(operationRequest);
                operationResponse.setStatusCode(401);
                operationResponseHandler.handle(Future.succeededFuture(operationResponse));
                return;
            }
            pool.getConnection(getConnectionResult -> {
                if (getConnectionResult.failed()) {
                    operationResponseHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                    return;
                }
                SqlConnection connection = getConnectionResult.result();
                String username = session.get("username");
                userRepository.getUserById(connection, username, getUserResult -> {
                    if (getUserResult.failed()) {
                        connection.close();
                        operationResponseHandler.handle(Future.failedFuture(getUserResult.cause()));
                        return;
                    }
                    User user = getUserResult.result();
                    if (user == null) {
                        httpSessionRepository.delete(session, deleteResult -> {
                            if (deleteResult.failed()) {
                                connection.close();
                                operationResponseHandler.handle(Future.failedFuture(deleteResult.cause()));
                                return;
                            }
                            OperationResponse operationResponse = createLogoutResponse(operationRequest);
                            operationResponse.setStatusCode(401);
                            connection.close();
                            operationResponseHandler.handle(Future.succeededFuture(operationResponse));
                        });
                        return;
                    }

                    RequestContext requestContext = new RequestContext(connection, session, user, operationResponseHandler);
                    handler.handle(requestContext);
                });
            });
        });
    }

    public OperationResponse createLogoutResponse(OperationRequest operationRequest) {
        OperationResponse operationResponse = new OperationResponse();
        ServerCookie sessionCookie = httpSessionRepository.getSessionCookie(operationRequest);
        if (sessionCookie != null) {
            sessionCookie.setMaxAge(0L);
            operationResponse.putHeader("Set-Cookie", sessionCookie.encode());
        }
        return operationResponse;
    }
}
