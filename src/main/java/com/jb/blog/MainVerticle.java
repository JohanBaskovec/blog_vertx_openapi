package com.jb.blog;

import com.jb.blog.persistence.HttpSessionRepository;
import com.jb.blog.persistence.article.ArticleRepository;
import com.jb.blog.persistence.article.ArticleRepositoryImpl;
import com.jb.blog.persistence.user.UserDbConverter;
import com.jb.blog.persistence.user.UserDbConverterImpl;
import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.persistence.user.UserRepositoryImpl;
import com.jb.blog.services.*;
import com.jb.blog.session.SessionConfiguration;
import com.jb.blog.webservices.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.openapitools.vertxweb.server.model.*;

import java.time.Duration;
import java.time.Instant;

public class MainVerticle extends AbstractVerticle {

    private static Instant start;
    private HttpServer server;
    private ServiceBinder serviceBinder;

    private MessageConsumer<JsonObject> articleWebServiceConsumer;
    private MessageConsumer<JsonObject> httpSessionWebServiceConsumer;
    private MessageConsumer<JsonObject> userServiceConsumer;
    private PgPool pool;

    private Future<Void> startHttpServer() {
        String confFilePath = System.getenv("BLOG_CONF");
        JsonObject config = new JsonObject(vertx.fileSystem().readFileBlocking(confFilePath));
        PgConnectOptions pgConnectOptions = new PgConnectOptions(config.getJsonObject("database"));
        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        pool = PgPool.pool(vertx, pgConnectOptions, poolOptions);

        JsonMapper<User> userMapper = new DefaultJsonMapperImpl<>(User.class);
        LocalSessionStore localSessionStore = LocalSessionStore.create(vertx);
        SessionHandler sessionHandler = SessionHandler.create(localSessionStore);
        sessionHandler.setCookieSameSite(CookieSameSite.STRICT);
        SessionConfiguration sessionConfiguration = SessionConfiguration.createDefault();
        OperationRequestService operationRequestService = new OperationRequestServiceImpl();
        HttpSessionRepository httpSessionRepository = new HttpSessionRepository(
                localSessionStore,
                sessionConfiguration,
                operationRequestService
        );
        UserDbConverter userDbConverter = new UserDbConverterImpl();
        UserRepository userRepository = new UserRepositoryImpl(userDbConverter);

        RequestContextManagerFactory requestContextManagerFactory = new RequestContextManagerFactory(
                pool,
                httpSessionRepository,
                userRepository
        );
        ArticleDbConverter articleDbConverter = new ArticleDbConverterImpl();
        JsonMapper<ArticleCreationRequest> articleCreationRequestJsonMapper = new DefaultJsonMapperImpl<>(ArticleCreationRequest.class);
        ArticleRepository articleRepository = new ArticleRepositoryImpl(articleDbConverter);
        ArticleWebService articleWebService = new ArticleWebServiceImpl(
                articleRepository,
                articleCreationRequestJsonMapper,
                requestContextManagerFactory
        );

        HttpSessionWebService httpSessionWebService = new HttpSessionWebServiceImpl(
                requestContextManagerFactory,
                new DefaultJsonMapperImpl<>(LoginForm.class),
                httpSessionRepository,
                sessionConfiguration,
                userMapper,
                operationRequestService,
                userRepository
        );
        RegistrationFormService registrationFormService = new RegistrationFormServiceImpl();
        UserWebService userWebService = new UserWebServiceImpl(
                pool,
                userRepository,
                new DefaultJsonMapperImpl<>(RegistrationForm.class),
                registrationFormService
        );

        serviceBinder = new ServiceBinder(vertx);
        articleWebServiceConsumer = serviceBinder
                .setAddress("article_service")
                .register(ArticleWebService.class, articleWebService);
        httpSessionWebServiceConsumer = serviceBinder
                .setAddress("session_service")
                .register(HttpSessionWebService.class, httpSessionWebService);
        userServiceConsumer = serviceBinder
                .setAddress("user_service")
                .register(UserWebService.class, userWebService);

        Promise<Void> promise = Promise.promise();
        OpenAPI3RouterFactory.create(this.vertx, "/openapi.yaml", openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                // We can't use SessionHandler because we can't access RoutingContext in handlers,
                // only OperationRequest, which doesn't have a session() method :(
                // So we write our own session handling
                routerFactory.addSecurityHandler("cookieAuth", routingContext -> {
                    routingContext.next();
                });

                routerFactory.mountServicesFromExtensions();
                routerFactory.addGlobalHandler(routingContext -> {
                    routingContext.response()
                            .putHeader("Access-Control-Allow-Origin", "http://localhost:3000")
                            .putHeader("Access-Control-Allow-Methods", "PUT,POST,HEAD,GET,OPTIONS")
                            .putHeader("Access-Control-Allow-Credentials", "true")
                            .putHeader("Access-Control-Allow-Headers", "content-type, cookie");
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
                    ServerError serverError = new ServerError(e.getMessage());
                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");
                    response.setStatusCode(500)
                            .end(Json.encode(serverError));
                });
                router.errorHandler(400, routingContext -> {
                    if (routingContext.failure() instanceof ValidationException) {
                        // Something went wrong during validation!
                        ValidationException e = (ValidationException) routingContext.failure();
                        ValidationError validationError = new ValidationError(e.getMessage(), e.parameterName());
                        HttpServerResponse response = routingContext.response();
                        response.putHeader("content-type", "application/json");
                        response.setStatusCode(400)
                                .end(Json.encode(validationError));
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
        articleWebServiceConsumer.unregister();
        userServiceConsumer.unregister();
        httpSessionWebServiceConsumer.unregister();
    }

    public static void main(String[] args) {
        start = Instant.now();
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

}
