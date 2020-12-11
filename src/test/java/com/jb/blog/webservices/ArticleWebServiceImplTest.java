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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openapitools.vertxweb.server.model.Article;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ArticleWebServiceImplTest {
    private PgPool pool;
    private ArticleRepository articleRepository;
    private ArticleWebService articleWebService;
    private OperationRequest operationRequest;
    private Handler<AsyncResult<OperationResponse>> handler;
    private SqlConnection sqlConnection;

    private JsonMapper<Article> articleMapper;

    @Before
    public void beforeEach() {
        pool = mock(PgPool.class);
        handler = mock(Handler.class);
        sqlConnection = mock(SqlConnection.class);
        articleRepository = mock(ArticleRepository.class);
        articleMapper = mock(JsonMapper.class);
        articleWebService = new ArticleWebServiceImpl(pool, articleRepository, articleMapper);
        operationRequest = mock(OperationRequest.class);
    }

    private void mockSuccessfulPoolGetConnection() {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.succeededFuture(sqlConnection));
            return null;
        }).when(pool).getConnection(any(Handler.class));
    }

    private void mockFailedPoolGetConnection(Exception exception) {
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<SqlConnection>> handler = invocationOnMock.getArgument(0);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(pool).getConnection(any(Handler.class));
    }

    @Test
    public void getAllArticlesWhenThereIsNoError() {
        mockSuccessfulPoolGetConnection();
        List<Article> articles = new ArrayList<>();
        for (int i = 0 ; i < 4 ; i++) {
            Article article = new Article();
            article.setId(RandomStringUtils.randomAlphabetic(20));
            articles.add(article);
        }

        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<List<Article>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.succeededFuture(articles));
            return null;
        }).when(articleRepository).getAllArticles(eq(sqlConnection), any(Handler.class));

        articleWebService.getAllArticles(operationRequest, handler);
        verify(sqlConnection).close();
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
        mockSuccessfulPoolGetConnection();
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<List<Article>>> handler = invocationOnMock.getArgument(1);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getAllArticles(eq(sqlConnection), any(Handler.class));

        articleWebService.getAllArticles(operationRequest, handler);
        verify(sqlConnection).close();
        ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void getAllArticlesWhenErrorWhenGettingConnection() {
        Exception exception = new RuntimeException();
        mockFailedPoolGetConnection(exception);

        articleWebService.getAllArticles(operationRequest, handler);

        ArgumentCaptor<Future<OperationResponse>> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future<OperationResponse> future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getArticleByIdWhenArticleExists() {
        mockSuccessfulPoolGetConnection();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);
        verify(sqlConnection).close();
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
        mockSuccessfulPoolGetConnection();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }

    @Test
    public void getArticleByIdWhenErrorWhenGettingArticle() {
        mockSuccessfulPoolGetConnection();
        String id = "test";
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(id), any(Handler.class));

        articleWebService.getArticleById(id, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }

    @Test
    public void getArticleByIdWhenErrorWhenTryingToGetSqlConnection() {
        Exception exception = new RuntimeException("failure message");
        mockFailedPoolGetConnection(exception);
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
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).insertArticle(eq(sqlConnection), any(Article.class), any(Handler.class));

        articleWebService.insertArticle(body, operationRequest, handler);
        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void insertArticleWhenThereIsAnErrorDuringInsert() {
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).insertArticle(eq(sqlConnection), any(Article.class), any(Handler.class));

        articleWebService.insertArticle(body, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void insertArticleWhenErrorDuringBegin() {
        Exception exception = new RuntimeException();
        mockFailedPoolGetConnection(exception);
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
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(article.getId()), any(Handler.class));
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).updateArticle(eq(sqlConnection), any(Article.class), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenArticleDoesntExist() {
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(article.getId()), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        OperationResponse operationResponse = (OperationResponse) future.result();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenThereIsAnErrorDuringGetArticleById() {
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        when(articleMapper.fromJson(body)).thenReturn(article);
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(article.getId()), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void updateArticleWhenThereIsAnErrorDuringInsert() {
        mockSuccessfulPoolGetConnection();
        JsonObject body = new JsonObject();
        Article article = new Article();
        article.setId("id");
        when(articleMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getArticleById(eq(sqlConnection), eq(article.getId()), any(Handler.class));
        Exception exception = new RuntimeException();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.failedFuture(exception));
            return null;
        }).when(articleRepository).updateArticle(eq(sqlConnection), any(Article.class), any(Handler.class));

        articleWebService.updateArticle(body, operationRequest, handler);

        verify(sqlConnection).close();
        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertSame(exception, future.cause());
    }

    @Test
    public void updateArticleWhenErrorDuringBegin() {
        Exception exception = new RuntimeException();
        mockFailedPoolGetConnection(exception);
        JsonObject body = new JsonObject();

        articleWebService.updateArticle(body, operationRequest, handler);

        ArgumentCaptor<Future> futureArgumentCaptor = ArgumentCaptor.forClass(Future.class);
        verify(handler).handle(futureArgumentCaptor.capture());
        Future future = futureArgumentCaptor.getValue();
        assertTrue(future.failed());
        assertSame(exception, future.cause());
    }
}
