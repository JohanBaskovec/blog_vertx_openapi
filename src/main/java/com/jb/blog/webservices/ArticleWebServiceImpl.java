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
import org.openapitools.vertxweb.server.model.ArticleFormData;
import org.openapitools.vertxweb.server.model.User;

public class ArticleWebServiceImpl implements ArticleWebService {
    private final ArticleRepository articleRepository;
    private final JsonMapper<ArticleFormData> articleFormDataJsonMapper;
    private final RequestContextManagerFactory requestContextManagerFactory;

    public ArticleWebServiceImpl(
            ArticleRepository articleRepository,
            JsonMapper<ArticleFormData> articleFormDataJsonMapper,
            RequestContextManagerFactory requestContextManagerFactory
    ) {
        this.articleRepository = articleRepository;
        this.articleFormDataJsonMapper = articleFormDataJsonMapper;
        this.requestContextManagerFactory = requestContextManagerFactory;
    }

    public void getAllArticles(
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        RequestContextManager requestContextManager = requestContextManagerFactory.create(operationRequest, handler);
        requestContextManager.getContextWithoutUser(requestContext -> {
            articleRepository.getAll(
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
            articleRepository.getById(
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
            ArticleFormData articleFormData = articleFormDataJsonMapper.fromJson(body);
            User user = requestContext.getUser();
            Article article = new Article();
            article.setId(articleFormData.getId());
            article.setContent(articleFormData.getContent());
            article.setTitle(articleFormData.getTitle());
            article.setAuthor(user);
            articleRepository.insert(
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
            ArticleFormData articleFormData = articleFormDataJsonMapper.fromJson(body);
            articleRepository.getById(
                    requestContext.getSqlConnection(),
                    articleFormData.getId(),
                    requestContext.createHandler((articleInDb) -> {
                        if (articleInDb == null) {
                            OperationResponse operationResponse = new OperationResponse();
                            operationResponse.setStatusCode(404);
                            requestContext.handleSuccess(operationResponse);
                            return;
                        }
                        articleInDb.setContent(articleFormData.getContent());
                        articleInDb.setTitle(articleFormData.getTitle());

                        articleRepository.update(
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
