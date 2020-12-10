package com.jb.blog.webservices;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;

@WebApiServiceGen
public interface UserWebService {
    void register(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    );
}
