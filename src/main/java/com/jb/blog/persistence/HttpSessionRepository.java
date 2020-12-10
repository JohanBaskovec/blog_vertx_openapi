package com.jb.blog.persistence;

import com.jb.blog.services.OperationRequestService;
import com.jb.blog.session.SessionConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.Map;

public class HttpSessionRepository {
    private final SessionStore sessionStore;
    private final SessionConfiguration sessionConfiguration;
    private final OperationRequestService operationRequestService;

    public HttpSessionRepository(
            SessionStore sessionStore,
            SessionConfiguration sessionConfiguration,
            OperationRequestService operationRequestService
    ) {
        this.sessionStore = sessionStore;
        this.sessionConfiguration = sessionConfiguration;
        this.operationRequestService = operationRequestService;
    }

    public void getFromOperationRequest(
            OperationRequest operationRequest,
            Handler<AsyncResult<Session>> handler
    ) {
        ServerCookie sessionCookie = getSessionCookie(operationRequest);
        if (sessionCookie == null) {
            handler.handle(Future.succeededFuture(null));
        }
        sessionStore.get(sessionCookie.getValue(), sessionStoreGetResult -> {
            if (sessionStoreGetResult.failed()) {
                handler.handle(Future.failedFuture(sessionStoreGetResult.cause()));
                return;
            }
            Session session = sessionStoreGetResult.result();
            handler.handle(Future.succeededFuture(session));
        });
    }

    public ServerCookie getSessionCookie(OperationRequest operationRequest) {
        Map<String, ServerCookie> cookies = operationRequestService.extractCookies(operationRequest);
        return cookies.get(sessionConfiguration.sessionCookieName);
    }

    public Session createSession() {
        return sessionStore.createSession(
                sessionConfiguration.sessionTimeout,
                sessionConfiguration.minLength
        );
    }

    public void delete(Session session, Handler<AsyncResult<Void>> handler) {
        sessionStore.delete(session.id(), handler);
    }

    public void putSession(Session session, Handler<AsyncResult<Void>> handler) {
        sessionStore.put(session, handler);
    }
}
