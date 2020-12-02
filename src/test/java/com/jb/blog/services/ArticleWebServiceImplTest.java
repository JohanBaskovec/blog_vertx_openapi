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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ArticleWebServiceImplTest {
    private PgPool pool;
    private ArticleRepositoryFactory articleRepositoryFactory;
    private ArticleWebService articleWebService;
    private OperationRequest operationRequest;
    private Handler<AsyncResult<OperationResponse>> handler;
    private Transaction transaction;

    private ArticleRepository articleRepository;
    private ArticleMapper articleMapper;

    @Before
    public void beforeEach() {
        pool = mock(PgPool.class);
        articleRepositoryFactory = mock(ArticleRepositoryFactory.class);
        handler = mock(Handler.class);
        transaction = mock(Transaction.class);
        articleRepository = mock(ArticleRepository.class);
        articleMapper = mock(ArticleMapper.class);
        when(articleRepositoryFactory.create(transaction)).thenReturn(articleRepository);
        articleWebService = new ArticleWebServiceImpl(pool, articleRepositoryFactory, articleMapper);
        operationRequest = mock(OperationRequest.class);
    }

    private void mockSuccessfulPoolBegin() {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Transaction>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(transaction));
            return null;
        }).when(pool).begin(any(Handler.class));
    }

    private void mockFailedPoolBegin(Exception exception) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Transaction>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(pool).begin(any(Handler.class));
    }

    @Test
    public void getAllArticlesWhenThereIsNoError() {
        mockSuccessfulPoolBegin();
        List<Article> articles = new ArrayList<>();
        for (int i = 0 ; i < 4 ; i++) {
            Article article = new Article();
            article.setId(RandomStringUtils.randomAlphabetic(20));
            articles.add(article);
        }

        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<List<Article>>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(articles));
            return null;
        }).when(articleRepository).getAllArticles(any(Handler.class));

        articleWebService.getAllArticles(operationRequest, handler);
        verify(transaction).commit();
        ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = future.result();
        assertEquals(200, (int)operationResponse.getStatusCode());
        JsonArray responseJson = new JsonArray(operationResponse.getPayload());
        List<Article> responseArticles = new ArrayList<>();
        for (Object o : responseJson) {
            JsonObject articleJsonObject = (JsonObject) o;
            responseArticles.add(articleJsonObject.mapTo(Article.class));
        }
        assertEquals(articles, responseArticles);
    }

    @Test
    public void getAllArticlesWhenExceptionWhenGettingArticles() {
        mockSuccessfulPoolBegin();
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<List<Article>>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getAllArticles(any(Handler.class));

        articleWebService.getAllArticles(operationRequest, handler);
        verify(transaction).rollback();
        ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void getAllArticlesWhenErrorDuringBegin() {
        Exception exception = new RuntimeException();
        mockFailedPoolBegin(exception);

        articleWebService.getAllArticles(operationRequest, handler);

        ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getArticleByIdWhenArticleExists() {
        mockSuccessfulPoolBegin();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);
        verify(transaction).commit();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(200, (int)operationResponse.getStatusCode());
        JsonObject responseJson = new JsonObject(operationResponse.getPayload());
        assertEquals(JsonObject.mapFrom(article), responseJson);
    }

    @Test
    public void getArticleByIdWhenArticleDoesNotExists() {
        mockSuccessfulPoolBegin();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getArticleById(eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);

        verify(transaction).commit();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }

    @Test
    public void getArticleByIdWhenErrorWhenGettingArticle() {
        mockSuccessfulPoolBegin();
        String id = "test";
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getArticleById(eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);

        verify(transaction).rollback();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getArticleByIdWhenErrorDuringBegin() {
        Exception exception = new RuntimeException("failure message");
        mockFailedPoolBegin(exception);
        String id = "test";

        articleWebService.getArticleById(id, operationRequest, handler);

        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void insertArticleWhenItWorks() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).insertArticle(any(Article.class), any(Handler.class));

        articleWebService.insertArticle(body, operationRequest, handler);
        verify(transaction).commit();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void insertArticleWhenThereIsAnErrorDuringInsert() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).insertArticle(any(Article.class), any(Handler.class));

        articleWebService.insertArticle(body, operationRequest, handler);

        verify(transaction).rollback();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void insertArticleWhenErrorDuringBegin() {
        Exception exception = new RuntimeException();
        mockFailedPoolBegin(exception);
        JsonObject body = new JsonObject();
        articleWebService.insertArticle(body, operationRequest, handler);

        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void updateArticleWhenItWorks() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(article.getId()), any(Handler.class));
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).updateArticle(any(Article.class), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(transaction).commit();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenArticleDoesntExist() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getArticleById(eq(article.getId()), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(transaction).rollback();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenThereIsAnErrorDuringGetArticleById() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getArticleById(eq(article.getId()), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(transaction).rollback();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void updateArticleWhenThereIsAnErrorDuringInsert() {
        mockSuccessfulPoolBegin();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(article.getId()), any(Handler.class));
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).updateArticle(any(Article.class), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(transaction).rollback();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void updateArticleWhenErrorDuringBegin() {
        Exception exception = new RuntimeException();
        mockFailedPoolBegin(exception);
        JsonObject body = new JsonObject();

        articleWebService.updateArticle(body, operationRequest, handler);

        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }
}
