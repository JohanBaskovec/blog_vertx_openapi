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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openapitools.vertxweb.server.model.Article;
import org.openapitools.vertxweb.server.model.ArticleFormData;

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

    private JsonMapper<ArticleFormData> articleFormDataJsonMapper;
    private RequestContextManagerFactory requestContextManagerFactory;
    private RequestContextManager requestContextManager;
    private RequestContext requestContext;

    @Before
    public void beforeEach() {
        pool = mock(PgPool.class);
        handler = mock(Handler.class);
        sqlConnection = mock(SqlConnection.class);
        articleRepository = mock(ArticleRepository.class);
        articleFormDataJsonMapper = mock(JsonMapper.class);
        requestContextManagerFactory = mock(RequestContextManagerFactory.class);
        requestContextManager = mock(RequestContextManager.class);
        requestContext = mock(RequestContext.class);
        operationRequest = mock(OperationRequest.class);
        when(requestContextManagerFactory.create(operationRequest, handler)).thenReturn(requestContextManager);
        articleWebService = new ArticleWebServiceImpl(articleRepository, articleFormDataJsonMapper, requestContextManagerFactory);
    }

    private void mockRequestContextWithConnection() {
        when(requestContext.getSqlConnection()).thenReturn(sqlConnection);
        when(requestContext.createHandler(any(Handler.class))).thenAnswer(new Answer<Handler>() {
            @Override
            public Handler answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler handler = invocationOnMock.getArgument(0);
                return new Handler<AsyncResult>() {
                    @Override
                    public void handle(AsyncResult event) {
                        handler.handle(event.result());
                    }
                };
            }
        });
    }
    private void mockGetContextWithoutUser() {
        mockRequestContextWithConnection();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler<RequestContext> handler = invocationOnMock.getArgument(0);
                handler.handle(requestContext);
                return null;
            }
        }).when(requestContextManager).getContextWithoutUser(any(Handler.class));
    }

    private void mockGetContextWithUser() {
        mockRequestContextWithConnection();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Handler<RequestContext> handler = invocationOnMock.getArgument(0);
                handler.handle(requestContext);
                return null;
            }
        }).when(requestContextManager).getContextWithUser(any(Handler.class));
    }

    @Test
    public void getAllArticlesWhenThereIsNoError() {
        // arrange
        mockGetContextWithoutUser();
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
        }).when(articleRepository).getAll(eq(sqlConnection), any(Handler.class));

        // act
        articleWebService.getAllArticles(operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
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
    public void getArticleByIdWhenArticleExists() {
        // arrange
        mockGetContextWithoutUser();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getById(eq(sqlConnection), eq(id), any(Handler.class));

        // act
        articleWebService.getArticleById(id, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(200, (int)operationResponse.getStatusCode());
        JsonObject responseJson = new JsonObject(operationResponse.getPayload());
        assertEquals(JsonObject.mapFrom(article), responseJson);
    }

    @Test
    public void getArticleByIdWhenArticleDoesNotExists() {
        // arrange
        mockGetContextWithoutUser();
        String id = "test";
        Article article = new Article();
        article.setId(id);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getById(eq(sqlConnection), eq(id), any(Handler.class));

        // act
        articleWebService.getArticleById(id, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }

    @Test
    public void insertArticleWhenItWorks() {
        // arrange
        mockGetContextWithUser();
        JsonObject body = new JsonObject();
        ArticleFormData article = new ArticleFormData();
        when(articleFormDataJsonMapper.fromJson(body)).thenReturn(article);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).insert(eq(sqlConnection), any(Article.class), any(Handler.class));

        // act
        articleWebService.insertArticle(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenItWorks() {
        // arrange
        mockGetContextWithUser();
        JsonObject body = new JsonObject();
        ArticleFormData articleFormData = new ArticleFormData();
        articleFormData.setId("id");
        when(articleFormDataJsonMapper.fromJson(body)).thenReturn(articleFormData);
        Article article = new Article();
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(article));
            return null;
        }).when(articleRepository).getById(eq(sqlConnection), eq(articleFormData.getId()), any(Handler.class));
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture());
            return null;
        }).when(articleRepository).update(eq(sqlConnection), any(Article.class), any(Handler.class));

        // act
        articleWebService.updateArticle(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(204, (int)operationResponse.getStatusCode());
    }

    @Test
    public void updateArticleWhenArticleDoesntExist() {
        // arrange
        mockGetContextWithUser();
        JsonObject body = new JsonObject();
        ArticleFormData articleFormData = new ArticleFormData();
        articleFormData.setId("id");
        when(articleFormDataJsonMapper.fromJson(body)).thenReturn(articleFormData);
        doAnswer(invocationOnMock -> {
            Handler<AsyncResult<Article>> handler = invocationOnMock.getArgument(2);
            handler.handle(Future.succeededFuture(null));
            return null;
        }).when(articleRepository).getById(eq(sqlConnection), eq(articleFormData.getId()), any(Handler.class));

        // act
        articleWebService.updateArticle(body, operationRequest, handler);

        // assert
        ArgumentCaptor<OperationResponse> futureArgumentCaptor = ArgumentCaptor.forClass(OperationResponse.class);
        verify(requestContext).handleSuccess(futureArgumentCaptor.capture());
        OperationResponse operationResponse = futureArgumentCaptor.getValue();
        assertEquals(404, (int)operationResponse.getStatusCode());
    }
}
