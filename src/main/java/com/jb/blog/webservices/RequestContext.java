package com.jb.blog.webservices;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.sqlclient.SqlConnection;
import org.openapitools.vertxweb.server.model.User;

public class RequestContext {
    private final SqlConnection sqlConnection;
    private final Session session;
    private final User user;
    private final Handler<AsyncResult<OperationResponse>> operationResponseHandler;

    public RequestContext(SqlConnection sqlConnection, Session session, User user, Handler<AsyncResult<OperationResponse>> operationResponseHandler) {
        this.sqlConnection = sqlConnection;
        this.session = session;
        this.user = user;
        this.operationResponseHandler = operationResponseHandler;
    }

    public void handleThrowable(Throwable exception) {
        sqlConnection.close();
        operationResponseHandler.handle(Future.failedFuture(exception));
    }

    public <T> void handleAsyncResult(AsyncResult<T> asyncResult, Handler<T> successHandler) {
        if (asyncResult.failed()) {
            sqlConnection.close();
            operationResponseHandler.handle(Future.failedFuture(asyncResult.cause()));
            return;
        }
        successHandler.handle(asyncResult.result());
    }

    public void handleSuccess(OperationResponse operationResponse) {
        sqlConnection.close();
        operationResponseHandler.handle(Future.succeededFuture(operationResponse));
    }

    public <T> Handler<AsyncResult<T>> createHandler(Handler<T> handler) {
        return new Handler<AsyncResult<T>>() {
            @Override
            public void handle(AsyncResult<T> event) {
                handleAsyncResult(event, handler);
            }
        };
    }

    public SqlConnection getSqlConnection() {
        return sqlConnection;
    }

    public Session getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }

    public Handler<AsyncResult<OperationResponse>> getOperationResponseHandler() {
        return operationResponseHandler;
    }
}
