package com.jb.blog.services;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.api.OperationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OperationRequestServiceImpl implements OperationRequestService {
    // Copied from CookieImpl
    @Override
    public Map<String, ServerCookie> extractCookies(OperationRequest operationRequest) {
        String cookieHeader = operationRequest.getHeaders().get("Cookie");
        if (cookieHeader != null) {
            Set<Cookie> nettyCookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
            Map<String, ServerCookie> cookies = new HashMap<>(nettyCookies.size());
            for (io.netty.handler.codec.http.cookie.Cookie cookie : nettyCookies) {
                ServerCookie ourCookie = new CookieImpl(cookie);
                cookies.put(ourCookie.getName(), ourCookie);
            }
            return cookies;
        } else {
            return new HashMap<>(4);
        }
    }
}
