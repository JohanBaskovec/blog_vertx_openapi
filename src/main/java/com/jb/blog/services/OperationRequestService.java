package com.jb.blog.services;

import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.api.OperationRequest;

import java.util.Map;

public interface OperationRequestService {
    Map<String, ServerCookie> extractCookies(OperationRequest operationRequest);
}
