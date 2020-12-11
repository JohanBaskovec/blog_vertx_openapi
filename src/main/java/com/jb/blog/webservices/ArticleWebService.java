package com.jb.blog.webservices;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;

@WebApiServiceGen
public interface ArticleWebService {
    void getArticleById(
            String id,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    );

    void insertArticle(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    );

    void updateArticle(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    );

    void getAllArticles(
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    );
}
