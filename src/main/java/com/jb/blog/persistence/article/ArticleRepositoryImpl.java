package com.jb.blog.persistence.article;

import com.jb.blog.persistence.CrudRepositoryImpl;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.sqlclient.*;
import org.openapitools.vertxweb.server.model.User;

public class ArticleRepositoryImpl extends CrudRepositoryImpl<Article> implements ArticleRepository {
    public ArticleRepositoryImpl() {
        super(
                "select article.id article_id, article.title article_title, article.content article_content, appuser.username user_username\n" +
                        "from article\n" +
                        "         join appuser on article.author_id = appuser.username\n",
                "insert into article(id, title, content, author_id) values ($1, $2, $3, $4)",
                "update article set title = $1, content = $2, version = version + 1 where id = $3 returning version"
        );
    }


    @Override
    protected Article fromRow(Row row) {
        Article article = new Article();
        article.setId(row.getString("article_id"));
        article.setTitle(row.getString("article_title"));
        article.setContent(row.getString("article_content"));
        User user = new User();
        user.setUsername(row.getString("user_username"));
        article.setAuthor(user);
        return article;
    }

    protected Tuple insertTuple(Article entity) {
        return Tuple.of(entity.getId(), entity.getTitle(), entity.getContent(), entity.getAuthor().getUsername());
    }

    protected Tuple updateTuple(Article entity) {
        return Tuple.of(entity.getTitle(), entity.getContent(), entity.getId());
    }

}
