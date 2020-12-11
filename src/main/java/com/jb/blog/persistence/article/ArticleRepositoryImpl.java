package com.jb.blog.persistence.article;

import com.jb.blog.services.ArticleDbConverter;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;

public class ArticleRepositoryImpl implements ArticleRepository {
    private final ArticleDbConverter articleDbConverter;

    public ArticleRepositoryImpl(
            ArticleDbConverter articleDbConverter
    ) {
        this.articleDbConverter = articleDbConverter;
    }

    @Override
    public void getArticleById(SqlClient sqlClient, String id, Handler<AsyncResult<Article>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "select id, title, content from article where id=$1"
        );
        preparedQuery.execute(
                Tuple.of(id),
                (AsyncResult<RowSet<Row>> event) -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    RowSet<Row> rowSet = event.result();
                    if (rowSet.size() == 0) {
                        handler.handle(Future.succeededFuture());
                        return;
                    }
                    RowIterator<Row> it = rowSet.iterator();
                    Row row = it.next();
                    Article article = this.articleDbConverter.fromRow(row);
                    handler.handle(Future.succeededFuture(article));
                });
    }

    @Override
    public void insertArticle(
            SqlClient sqlClient, Article article,
            Handler<AsyncResult<Void>> handler
    ) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "insert into article(id, title, content) values ($1, $2, $3)"
        );
        preparedQuery.execute(
                Tuple.of(article.getId(), article.getTitle(), article.getContent()),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    handler.handle(Future.succeededFuture());
                });
    }

    public void updateArticle(SqlClient sqlClient, Article article, Handler<AsyncResult<Void>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "update article set title = $1, content = $2 where id = $3"
        );
        preparedQuery.execute(
                Tuple.of(article.getTitle(), article.getContent(), article.getId()),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    handler.handle(Future.succeededFuture());
                });
    }

    @Override
    public void getAllArticles(SqlClient sqlClient, Handler<AsyncResult<List<Article>>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "select id, title, content from article"
        );
        preparedQuery.execute((AsyncResult<RowSet<Row>> event) -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause()));
                return;
            }
            RowSet<Row> rowSet = event.result();
            List<Article> articles = new ArrayList<>();
            for (Row row : rowSet) {
                Article article = this.articleDbConverter.fromRow(row);
                articles.add(article);
            }
            handler.handle(Future.succeededFuture(articles));
        });
    }
}
