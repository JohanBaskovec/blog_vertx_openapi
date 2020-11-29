package com.jb.blog.services;

import com.jb.blog.persistence.ArticleRepository;
import com.jb.blog.persistence.ArticleRepositoryFactory;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Transaction;

import java.util.List;

public class ArticleWebServiceImpl implements ArticleWebService {
    private final PgPool pool;
    private final ArticleRepositoryFactory articleRepositoryFactory;

    public ArticleWebServiceImpl(
            PgPool pool,
            ArticleRepositoryFactory articleRepositoryFactory) {
        this.pool = pool;
        this.articleRepositoryFactory = articleRepositoryFactory;
    }

    public void getAllArticles(
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.begin(beginResult -> {
            if (beginResult.failed()) {
                resultHandler.handle(Future.failedFuture(beginResult.cause()));
                return;
            }
            Transaction transaction = beginResult.result();
            ArticleRepository articleRepository = this.articleRepositoryFactory.create(transaction);
            articleRepository.getAllArticles((getAllArticlesResult) -> {
                if (getAllArticlesResult.failed()) {
                    transaction.rollback();
                    resultHandler.handle(Future.failedFuture(getAllArticlesResult.cause()));
                    return;
                }
                transaction.commit();
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
        pool.begin(beginResult -> {
            if (beginResult.failed()) {
                resultHandler.handle(Future.failedFuture(beginResult.cause()));
                return;
            }
            Transaction transaction = beginResult.result();
            ArticleRepository articleRepository = this.articleRepositoryFactory.create(transaction);
            articleRepository.getArticleById(id, (getArticleByIdResult) -> {
                if (getArticleByIdResult.failed()) {
                    transaction.rollback();
                    resultHandler.handle(Future.failedFuture(getArticleByIdResult.cause()));
                    return;
                }
                transaction.commit();
                Article article = getArticleByIdResult.result();
                if (article == null) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(404);
                    resultHandler.handle(Future.succeededFuture(operationResponse));
                } else {
                    resultHandler.handle(Future.succeededFuture(
                            OperationResponse.completedWithJson(JsonObject.mapFrom(article))
                    ));
                }
            });
        });
    }

    public void insertArticle(
            JsonObject body,
            OperationRequest context,
            Handler<AsyncResult<OperationResponse>> resultHandler
    ) {
        pool.begin(beginResult -> {
            if (beginResult.failed()) {
                resultHandler.handle(Future.failedFuture(beginResult.cause()));
                return;
            }
            Transaction transaction = beginResult.result();
            ArticleRepository articleRepository = this.articleRepositoryFactory.create(transaction);
            Article article = body.mapTo(Article.class);
            articleRepository.insertArticle(article, (AsyncResult<Void> insertArticleResult) -> {
                if (insertArticleResult.failed()) {
                    transaction.rollback();
                    resultHandler.handle(Future.failedFuture(insertArticleResult.cause()));
                    return;
                }
                transaction.commit();
                OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatusCode(204);
                resultHandler.handle(Future.succeededFuture(operationResponse));
            });
        });
    }
}
