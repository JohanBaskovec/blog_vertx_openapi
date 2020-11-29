package com.jb.blog;

import com.jb.blog.persistence.*;
import com.jb.blog.services.ArticleDbConverter;
import com.jb.blog.services.ArticleDbConverterImpl;
import com.jb.blog.services.ArticleWebService;
import com.jb.blog.services.ArticleWebServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;

public class MainVerticle extends AbstractVerticle {

    HttpServer server;
    ServiceBinder serviceBinder;

    MessageConsumer<JsonObject> consumer;
    PgPool pool;

    private Future<Void> startHttpServer() {
        String confFilePath = System.getenv("BLOG_CONF");
        JsonObject config = new JsonObject(vertx.fileSystem().readFileBlocking(confFilePath));
        PgConnectOptions pgConnectOptions = new PgConnectOptions(config.getJsonObject("database"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        pool = PgPool.pool(vertx, pgConnectOptions, poolOptions);
        ArticleDbConverter articleDbConverter = new ArticleDbConverterImpl();
        ArticleRepositoryFactory articleRepositoryFactory = new ArticleRepositoryFactoryImpl(articleDbConverter);
        ArticleWebService articleWebService = new ArticleWebServiceImpl(pool, articleRepositoryFactory);

        serviceBinder = new ServiceBinder(vertx);
        consumer = serviceBinder
                .setAddress("article_service")
                .register(ArticleWebService.class, articleWebService);

        Promise<Void> promise = Promise.promise();
        OpenAPI3RouterFactory.create(this.vertx, "/openapi.yaml", openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                routerFactory.mountServicesFromExtensions();

                Router router = routerFactory.getRouter();
                router.errorHandler(500, routingContext -> {
                    Throwable e = routingContext.failure();
                    e.printStackTrace();
                    JsonObject responseJson = new JsonObject()
                            .put("message", e.getMessage());
                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");
                    response.setStatusCode(500)
                            .end(responseJson.toString());
                });
                router.errorHandler(400, routingContext -> {
                    if (routingContext.failure() instanceof ValidationException) {
                        // Something went wrong during validation!
                        ValidationException e = (ValidationException) routingContext.failure();
                        JsonObject responseJson = new JsonObject()
                                .put("message", e.getMessage())
                                .put("parameterName", e.parameterName());
                        HttpServerResponse response = routingContext.response();
                        response.putHeader("content-type", "application/json");
                        response.setStatusCode(400)
                                .end(responseJson.toString());
                    } else {
                        // Unknown 400 failure happened
                        routingContext.response().setStatusCode(400).end();
                    }
                });
                server = vertx.createHttpServer(new HttpServerOptions().setPort(8081).setHost("localhost"));
                server.requestHandler(router).listen(ar -> {
                    if (ar.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail(ar.cause());
                    }
                });
            } else {
                promise.fail(openAPI3RouterFactoryAsyncResult.cause());
            }
        });
        return promise.future();
    }

    @Override
    public void start(Promise<Void> promise) {
        startHttpServer().onComplete(promise);
    }

    @Override
    public void stop() {
        this.pool.close();
        this.server.close();
        consumer.unregister();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

}
