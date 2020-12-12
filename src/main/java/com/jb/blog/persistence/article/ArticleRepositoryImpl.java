package com.jb.blog.persistence.article;

import com.jb.blog.services.ArticleDbConverter;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;
import org.openapitools.vertxweb.server.model.EntityVersionId;
import org.openapitools.vertxweb.server.model.User;

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
                "select article.id article_id, article.title article_title, article.content article_content, appuser.username user_username\n" +
                        "from article\n" +
                        "         join appuser on article.author_id = appuser.username\n" +
                        "where id = $1\n"
        );
        preparedQuery.execute(
                Tuple.of(id),
                (AsyncResult<RowSet<Row>> event) -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    try {
                        RowSet<Row> rowSet = event.result();
                        if (rowSet.size() == 0) {
                            handler.handle(Future.succeededFuture());
                            return;
                        }
                        RowIterator<Row> it = rowSet.iterator();
                        Row row = it.next();
                        handler.handle(Future.succeededFuture(fromRow(row)));
                    } catch (Throwable t) {
                        handler.handle(Future.failedFuture(t));
                    }
                });
    }

    @Override
    public void getAllArticles(SqlClient sqlClient, Handler<AsyncResult<List<Article>>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "select article.id article_id, article.title article_title, article.content article_content, appuser.username user_username\n" +
                        "from article\n" +
                        "         join appuser on article.author_id = appuser.username\n"
        );
        preparedQuery.execute((AsyncResult<RowSet<Row>> event) -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause()));
                return;
            }
            try {
                RowSet<Row> rowSet = event.result();
                List<Article> articles = new ArrayList<>();
                for (Row row : rowSet) {
                    articles.add(this.fromRow(row));
                }
                handler.handle(Future.succeededFuture(articles));
            } catch (Throwable t) {
                handler.handle(Future.failedFuture(t));
            }
        });
    }

    private Article fromRow(Row row) {
        Article article = new Article();
        article.setId(row.getString("article_id"));
        article.setTitle(row.getString("article_title"));
        article.setContent(row.getString("article_content"));
        User user = new User();
        user.setUsername(row.getString("user_username"));
        article.setAuthor(user);
        return article;
    }

    @Override
    public void insertArticle(
            SqlClient sqlClient, Article article,
            Handler<AsyncResult<Void>> handler
    ) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "insert into article(id, title, content, author_id) values ($1, $2, $3, $4)"
        );
        preparedQuery.execute(
                Tuple.of(article.getId(), article.getTitle(), article.getContent(), article.getAuthor().getUsername()),
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
                "update article set title = $1, content = $2, version = version + 1 where id = $3 returning version"
        );
        preparedQuery.execute(
                Tuple.of(article.getTitle(), article.getContent(), article.getId()),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    try {
                        RowSet<Row> rows = event.result();
                        Row row = rows.iterator().next();
                        handler.handle(Future.succeededFuture());
                    } catch (Throwable t) {
                        handler.handle(Future.failedFuture(t));
                    }
                });
    }

}
