package com.jb.blog;

import com.jb.blog.persistence.ArticleRepositoryFactory;
import com.jb.blog.persistence.ArticleRepositoryFactoryImpl;
import com.jb.blog.services.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
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

import java.time.Duration;
import java.time.Instant;

public class MainVerticle extends AbstractVerticle {

    private static Instant start;
    private HttpServer server;
    private ServiceBinder serviceBinder;

    private MessageConsumer<JsonObject> consumer;
    private PgPool pool;

    private Future<Void> startHttpServer() {
        String confFilePath = System.getenv("BLOG_CONF");
        JsonObject config = new JsonObject(vertx.fileSystem().readFileBlocking(confFilePath));
        PgConnectOptions pgConnectOptions = new PgConnectOptions(config.getJsonObject("database"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        pool = PgPool.pool(vertx, pgConnectOptions, poolOptions);
        ArticleDbConverter articleDbConverter = new ArticleDbConverterImpl();
        ArticleRepositoryFactory articleRepositoryFactory = new ArticleRepositoryFactoryImpl(articleDbConverter);
        ArticleMapper articleMapper = new ArticleMapperImpl();
        ArticleWebService articleWebService = new ArticleWebServiceImpl(pool, articleRepositoryFactory, articleMapper);

        serviceBinder = new ServiceBinder(vertx);
        consumer = serviceBinder
                .setAddress("article_service")
                .register(ArticleWebService.class, articleWebService);

        Promise<Void> promise = Promise.promise();
        OpenAPI3RouterFactory.create(this.vertx, "/openapi.yaml", openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                routerFactory.mountServicesFromExtensions();
                routerFactory.addGlobalHandler(routingContext -> {
                    routingContext.response()
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .putHeader("Access-Control-Allow-Methods", "PUT,POST,HEAD,GET,OPTIONS")
                            .putHeader("Access-Control-Allow-Headers", "content-type");
                    if (routingContext.request().method().equals(HttpMethod.OPTIONS)) {
                        routingContext.response().end();
                        return;
                    }
                    routingContext.next();
                });
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
                server.requestHandler(router).listen((AsyncResult<HttpServer> httpServer) -> {
                    Duration duration = Duration.between(start, Instant.now());
                    System.out.println("Server ready! Startup time: " + duration.toMillis() + " milliseconds.");
                    if (httpServer.succeeded()) {
                        promise.complete();
                    } else {
                        promise.fail(httpServer.cause());
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
        start = Instant.now();
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

}
