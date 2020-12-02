package com.jb.blog.persistence;

import com.jb.blog.services.ArticleDbConverter;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ArticleRepositoryImplTest {
    ArticleDbConverter articleDbConverter;
    Transaction transaction;
    ArticleRepositoryImpl articleRepository;

    @Before
    public void beforeEach() {
        articleDbConverter = mock(ArticleDbConverter.class);
        transaction = mock(Transaction.class);
        articleRepository = new ArticleRepositoryImpl(articleDbConverter, transaction);
    }

    @Test
    public void getArticleByIdWhenThereIsOneMatchingArticle() {
        RowSet<Row> rowSet = mock(RowSet.class);
        when(rowSet.size()).thenReturn(1);
        RowIterator<Row> rowIterator = mock(RowIterator.class);
        when(rowSet.iterator()).thenReturn(rowIterator);
        Row row = mock(Row.class);
        when(rowIterator.next()).thenReturn(row);

        Article article = new Article();
        when(articleDbConverter.fromRow(row)).thenReturn(article);

        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);
        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(rowSet));
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));
        Handler<AsyncResult<Article>> handler = mock(Handler.class);

        articleRepository.getArticleById("id", handler);

        ArgumentCaptor<AsyncResult<Article>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Article> result = argumentCaptor.getValue();
        assertTrue(result.succeeded());
        assertSame(article, result.result());
    }

    @Test
    public void getArticleByIdWhenThereIsNoMatchingArticle() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);
        RowSet<Row> rowSet = mock(RowSet.class);
        when(rowSet.size()).thenReturn(0);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(rowSet));
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));
        Handler<AsyncResult<Article>> handler = mock(Handler.class);

        articleRepository.getArticleById("id", handler);

        ArgumentCaptor<AsyncResult<Article>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Article> result = argumentCaptor.getValue();
        assertTrue(result.succeeded());
        assertNull(result.result());
    }

    @Test
    public void getArticleByIdWhenThereIsAnError() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        Exception exception = new RuntimeException("hello");
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));

        Handler<AsyncResult<Article>> handler = mock(Handler.class);

        articleRepository.getArticleById("id", handler);

        ArgumentCaptor<AsyncResult<Article>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Article> result = argumentCaptor.getValue();
        assertTrue(result.failed());
        assertSame(exception, result.cause());
    }

    @Test
    public void insertArticleWhenSuccess() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));

        Article article = new Article();
        Handler<AsyncResult<Void>> handler = mock(Handler.class);

        articleRepository.insertArticle(article, handler);

        ArgumentCaptor<AsyncResult<Void>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Void> result = argumentCaptor.getValue();
        assertTrue(result.succeeded());
    }

    @Test
    public void insertArticleWhenException() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        Exception exception = new RuntimeException();
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));

        Article article = new Article();
        Handler<AsyncResult<Void>> handler = mock(Handler.class);

        articleRepository.insertArticle(article, handler);

        ArgumentCaptor<AsyncResult<Void>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Void> result = argumentCaptor.getValue();
        assertTrue(result.failed());
        assertSame(exception, result.cause());
    }

    @Test
    public void getAllArticlesWhenItWorks() {
        RowSet<Row> rowSet = mock(RowSet.class);
        List<Row> rows = new ArrayList<>();
        rows.add(mock(Row.class));
        rows.add(mock(Row.class));
        rows.add(mock(Row.class));
        List<Article> articles = new ArrayList<>();
        ArticleDbConverter articleDbConverter = mock(ArticleDbConverter.class);
        for (Row row : rows) {
            Article article = new Article();
            article.setId(RandomStringUtils.randomAlphabetic(20));
            articles.add(article);
            when(articleDbConverter.fromRow(row)).thenReturn(article);
        }

        Iterator<Row> rowListIterator = rows.iterator();
        RowIterator rowIterator = new RowIterator() {
            @Override
            public boolean hasNext() {
                return rowListIterator.hasNext();
            }

            @Override
            public Object next() {
                return rowListIterator.next();
            }
        };
        when(rowSet.iterator()).thenReturn(rowIterator);

        Transaction transaction = mock(Transaction.class);
        ArticleRepositoryImpl articleRepository = new ArticleRepositoryImpl(articleDbConverter, transaction);

        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);
        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(rowSet));
            return null;
        }).when(preparedQuery).execute(any(Handler.class));

        Handler<AsyncResult<List<Article>>> handler = mock(Handler.class);

        articleRepository.getAllArticles(handler);

        ArgumentCaptor<AsyncResult<List<Article>>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<List<Article>> result = argumentCaptor.getValue();
        assertTrue(result.succeeded());
        assertArrayEquals(articles.toArray(), result.result().toArray());
    }

    @Test
    public void getAllArticlesWhenException() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        Exception exception = new RuntimeException();
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(preparedQuery).execute(any(Handler.class));

        Handler<AsyncResult<List<Article>>> handler = mock(Handler.class);

        articleRepository.getAllArticles(handler);

        ArgumentCaptor<AsyncResult<List<Article>>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<List<Article>> result = argumentCaptor.getValue();
        assertTrue(result.failed());
        assertSame(exception, result.cause());
    }


    @Test
    public void updateArticleWhenArticleDoesntExist() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));

        Article article = new Article();
        Handler<AsyncResult<Void>> handler = mock(Handler.class);

        articleRepository.insertArticle(article, handler);

        ArgumentCaptor<AsyncResult<Void>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Void> result = argumentCaptor.getValue();
        assertTrue(result.succeeded());
    }

    @Test
    public void updateArticleWhenException() {
        PreparedQuery<RowSet<Row>> preparedQuery = mock(PreparedQuery.class);

        when(transaction.preparedQuery(anyString())).thenReturn(preparedQuery);
        Exception exception = new RuntimeException();
        doAnswer((Answer<Void>) invocationOnMock -> {
            Handler<AsyncResult<RowSet<Row>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(preparedQuery).execute(any(Tuple.class), any(Handler.class));

        Article article = new Article();
        Handler<AsyncResult<Void>> handler = mock(Handler.class);

        articleRepository.updateArticle(article, handler);

        ArgumentCaptor<AsyncResult<Void>> argumentCaptor = ArgumentCaptor.forClass(AsyncResult.class);
        verify(handler).handle(argumentCaptor.capture());
        AsyncResult<Void> result = argumentCaptor.getValue();
        assertTrue(result.failed());
        assertSame(exception, result.cause());
    }
}
