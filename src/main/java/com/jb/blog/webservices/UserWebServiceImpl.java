package com.jb.blog.webservices;

import com.jb.blog.persistence.user.UserRepository;
import com.jb.blog.services.JsonMapper;
import com.jb.blog.services.RegistrationFormService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;
import org.openapitools.vertxweb.server.model.RegistrationForm;
import org.openapitools.vertxweb.server.model.User;

public class UserWebServiceImpl implements UserWebService {
    private final PgPool pool;
    private final UserRepository userRepository;
    private final JsonMapper<RegistrationForm> registrationFormJsonMapper;
    private final RegistrationFormService registrationFormService;

    public UserWebServiceImpl(
            PgPool pool,
            UserRepository userRepository,
            JsonMapper<RegistrationForm> registrationFormJsonMapper,
            RegistrationFormService registrationFormService) {
        this.pool = pool;
        this.userRepository = userRepository;
        this.registrationFormJsonMapper = registrationFormJsonMapper;
        this.registrationFormService = registrationFormService;
    }

    @Override
    public void register(
            JsonObject body,
            OperationRequest operationRequest,
            Handler<AsyncResult<OperationResponse>> handler
    ) {
        pool.getConnection(getConnectionResult -> {
            if (getConnectionResult.failed()) {
                handler.handle(Future.failedFuture(getConnectionResult.cause()));
                return;
            }
            SqlClient connection = getConnectionResult.result();
            RegistrationForm registrationForm = registrationFormJsonMapper.fromJson(body);
            userRepository.getUserById(connection, registrationForm.getUsername(), (getUserByIdResult) -> {
                if (getUserByIdResult.failed()) {
                    connection.close();
                    handler.handle(Future.failedFuture(getUserByIdResult.cause()));
                    return;
                }
                User userInDb = getUserByIdResult.result();
                if (userInDb != null) {
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(400);
                    connection.close();
                    handler.handle(Future.succeededFuture(operationResponse));
                }
                // TODO: password hashing
                User newUser = registrationFormService.toUser(registrationForm);
                userRepository.insert(connection, newUser, insertResult -> {
                    if (insertResult.failed()) {
                        connection.close();
                        handler.handle(Future.failedFuture(insertResult.cause()));
                        return;
                    }
                    OperationResponse operationResponse = new OperationResponse();
                    operationResponse.setStatusCode(204);
                    connection.close();
                    handler.handle(Future.succeededFuture(operationResponse));
                });
            });
        });

    }
}
