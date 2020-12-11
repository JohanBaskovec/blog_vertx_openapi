package com.jb.blog.webservices;

import com.jb.blog.persistence.article.ArticleRepository;
import com.jb.blog.services.JsonMapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.openapitools.vertxweb.server.model.Article;

import java.util.List;

public class ArticleWebServiceImpl implements ArticleWebService {
    private final PgPool pool;
    private final ArticleRepository articleRepository;
    private final JsonMapper<Article> articleMapper;

    public ArticleWebServiceImpl(
            PgPool pool,
            ArticleRepository articleRepository,
            JsonMapper<Article> articleMapper
    ) {
        this.pool = pool;
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
    }

    public void getAllArticles(
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                resultHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection sqlConnection = getConnectionResult.result();
            articleRepository.getAllArticles(sqlConnection, (getAllArticlesResult) -> {
                if (getAllArticlesResult.failed()) {
                    sqlConnection.close();
                    resultHandler.handle(Future.failedFuture(getAllArticlesResult.cause()));
                    return;
                }
                sqlConnection.close();
                List<Article> articles = getAllArticlesResult.result();
                JsonArray jsonArray = new JsonArray(articles);
                resultHandler.handle(Future.succeededFuture(
                        OperationResponse.completedWithJson(jsonArray)
                ));
            });
        });
    }

    public void getArticleById(
            String id,
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                resultHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection sqlConnection = getConnectionResult.result();
            articleRepository.getArticleById(sqlConnection, id, (getArticleByIdResult) -> {
                if (getArticleByIdResult.failed()) {
                    sqlConnection.close();
                    resultHandler.handle(Future.failedFuture(getArticleByIdResult.cause()));
                    return;
                }
                Article article = getArticleByIdResult.result();
                if (article == null) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(404);
                    sqlConnection.close();
                    resultHandler.handle(Future.succeededFuture(operationResponse));
                    return;
                }

                sqlConnection.close();
                resultHandler.handle(Future.succeededFuture(
                        OperationResponse.completedWithJson(JsonObject.mapFrom(article))
                ));
            });
        });
    }

    public void insertArticle(
            JsonObject body,
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                resultHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection sqlConnection = getConnectionResult.result();
            Article article = articleMapper.fromJson(body);
            articleRepository.insertArticle(sqlConnection, article, (AsyncResult<Void> insertArticleResult) -> {
                if (insertArticleResult.failed()) {
                    sqlConnection.close();
                    resultHandler.handle(Future.failedFuture(insertArticleResult.cause()));
                    return;
                }
                sqlConnection.close();
                OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatusCode(204);
                resultHandler.handle(Future.succeededFuture(operationResponse));
            });
        });
    }

    public void updateArticle(
            JsonObject body,
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                resultHandler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlConnection sqlConnection = getConnectionResult.result();
            Article article = articleMapper.fromJson(body);
            articleRepository.getArticleById(sqlConnection, article.getId(), (AsyncResult<Article> getArticleByIdResult) -> {
                if (getArticleByIdResult.failed()) {
                    sqlConnection.close();
                    resultHandler.handle(Future.failedFuture(getArticleByIdResult.cause()));
                    return;
                }
                Article articleInDb = getArticleByIdResult.result();
                if (articleInDb == null) {
                    sqlConnection.close();
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(404);
                    resultHandler.handle(Future.succeededFuture(operationResponse));
                    return;
                }

                articleRepository.updateArticle(sqlConnection, article, (AsyncResult<Void> updateArticleResult) -> {
                    if (updateArticleResult.failed()) {
                        sqlConnection.close();
                        resultHandler.handle(Future.failedFuture(updateArticleResult.cause()));
                        return;
                    }
                    sqlConnection.close();
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(204);
                    resultHandler.handle(Future.succeededFuture(operationResponse));
                });
            });
        });
    }
}
