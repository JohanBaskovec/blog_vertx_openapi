package com.jb.blog.webservices;

import com.jb.blog.persistence.article.ArticleRepository;
import com.jb.blog.services.JsonMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import org.openapitools.vertxweb.server.model.Article;

public class ArticleWebServiceImpl implements ArticleWebService {
    private final ArticleRepository articleRepository;
    private final JsonMapper<Article> articleMapper;
    private final RequestContextManagerFactory requestContextManagerFactory;

    public ArticleWebServiceImpl(
            ArticleRepository articleRepository,
            JsonMapper<Article> articleMapper,
            RequestContextManagerFactory requestContextManagerFactory
    ) {
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
        this.requestContextManagerFactory = requestContextManagerFactory;
    }

    public void getAllArticles(
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithoutUser(requestContext -> {
            articleRepository.getAllArticles(
                    requestContext.getSqlConnection(),
                    requestContext.createHandler((articles) -> {
                        JsonArray jsonArray = new JsonArray(articles);
                        requestContext.handleSuccess(OperationResponse.completedWithJson(jsonArray));
                    }));
        });
    }

    public void getArticleById(
            String id,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithoutUser(requestContext -> {
            articleRepository.getArticleById(
                    requestContext.getSqlConnection(),
                    id,
                    requestContext.createHandler((article) -> {
                        if (article == null) {
                            OperationResponse operationResponse = new OperationResponse();
                            operationResponse.setStatusCode(404);
                            requestContext.handleSuccess(operationResponse);
                            return;
                        }

                        requestContext.handleSuccess(OperationResponse.completedWithJson(JsonObject.mapFrom(article)));
                    }));
        });
    }

    public void insertArticle(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithUser(requestContext -> {
            Article article = articleMapper.fromJson(body);
            articleRepository.insertArticle(
                    requestContext.getSqlConnection(),
                    article,
                    requestContext.createHandler((Void result) -> {
                        OperationResponse operationResponse = new OperationResponse();
                        operationResponse.setStatusCode(204);
                        requestContext.handleSuccess(operationResponse);
                    }));
        });
    }

    public void updateArticle(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithUser(requestContext -> {
            Article article = articleMapper.fromJson(body);
            articleRepository.getArticleById(
                    requestContext.getSqlConnection(),
                    article.getId(),
                    requestContext.createHandler((articleInDb) -> {
                        if (articleInDb == null) {
                            OperationResponse operationResponse = new OperationResponse();
                            operationResponse.setStatusCode(404);
                            requestContext.handleSuccess(operationResponse);
                            return;
                        }

                        articleRepository.updateArticle(
                                requestContext.getSqlConnection(),
                                article,
                                requestContext.createHandler((Void updateArticleResult) -> {
                                    OperationResponse operationResponse = new OperationResponse();
                                    operationResponse.setStatusCode(204);
                                    requestContext.handleSuccess(operationResponse);
                                }));
                    }));
        });
    }
}
