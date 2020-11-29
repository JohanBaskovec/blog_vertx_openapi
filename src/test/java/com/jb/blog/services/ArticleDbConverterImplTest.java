package com.jb.blog.services;

import com.jb.blog.persistence.ArticleRepositoryImpl;
import io.swagger.client.model.Article;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Transaction;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArticleDbConverterImplTest {
    @Test
    public void fromRow() {
        ArticleDbConverter articleDbConverter = new ArticleDbConverterImpl();
        Row row = mock(Row.class);
        String id = "id";
        String title = "title";
        String content = "content";
        when(row.getString("id")).thenReturn(id);
        when(row.getString("title")).thenReturn(title);
        when(row.getString("content")).thenReturn(content);

        Article article = articleDbConverter.fromRow(row);
        assertEquals(article.getId(), id);
        assertEquals(article.getTitle(), title);
        assertEquals(article.getContent(), content);
    }
}
