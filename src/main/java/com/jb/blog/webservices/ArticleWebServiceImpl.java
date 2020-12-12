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
import org.openapitools.vertxweb.server.model.ArticleCreationRequest;
import org.openapitools.vertxweb.server.model.EntityVersionId;
import org.openapitools.vertxweb.server.model.User;

public class ArticleWebServiceImpl implements ArticleWebService {
    private final ArticleRepository articleRepository;
    private final JsonMapper<ArticleCreationRequest> articleCreationRequestJsonMapper;
    private final RequestContextManagerFactory requestContextManagerFactory;

    public ArticleWebServiceImpl(
            ArticleRepository articleRepository,
            JsonMapper<ArticleCreationRequest> articleCreationRequestJsonMapper,
            RequestContextManagerFactory requestContextManagerFactory
    ) {
        this.articleRepository = articleRepository;
        this.articleCreationRequestJsonMapper = articleCreationRequestJsonMapper;
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

                        JsonObject jsonObject = JsonObject.mapFrom(article);
                        requestContext.handleSuccess(OperationResponse.completedWithJson(jsonObject));
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
            ArticleCreationRequest articleCreationRequest = articleCreationRequestJsonMapper.fromJson(body);
            User user = requestContext.getUser();
            Article article = new Article();
            article.setId(articleCreationRequest.getId());
            article.setContent(articleCreationRequest.getContent());
            article.setTitle(articleCreationRequest.getTitle());
            article.setAuthor(user);
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
            ArticleCreationRequest articleCreationRequest = articleCreationRequestJsonMapper.fromJson(body);
            articleRepository.getArticleById(
                    requestContext.getSqlConnection(),
                    articleCreationRequest.getId(),
                    requestContext.createHandler((articleInDb) -> {
                        if (articleInDb == null) {
                            OperationResponse operationResponse = new OperationResponse();
                            operationResponse.setStatusCode(404);
                            requestContext.handleSuccess(operationResponse);
                            return;
                        }
                        articleInDb.setContent(articleCreationRequest.getContent());
                        articleInDb.setTitle(articleCreationRequest.getTitle());

                        articleRepository.updateArticle(
                                requestContext.getSqlConnection(),
                                articleInDb,
                                requestContext.createHandler((Void updateArticleResult) -> {
                                    OperationResponse operationResponse = new OperationResponse();
                                    operationResponse.setStatusCode(204);
                                    requestContext.handleSuccess(operationResponse);
                                }));
                    }));
        });
    }
}
