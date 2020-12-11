package com.jb.blog.webservices;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.user.UserRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;

public class RequestContextManagerFactory {
    private final PgPool pool;
    private final HttpSessionRepository httpSessionRepository;
    private final UserRepository userRepository;

    public RequestContextManagerFactory(
            PgPool pool,
            HttpSessionRepository httpSessionRepository,
            UserRepository userRepository
    ) {
        this.pool = pool;
        this.httpSessionRepository = httpSessionRepository;
        this.userRepository = userRepository;
    }

    public RequestContextManager create(OperationRequest operationRequest, Handler<AsyncResult<OperationResponse>> resultHandler) {
        return new RequestContextManager(operationRequest, pool, httpSessionRepository, userRepository, resultHandler);
    }
}
