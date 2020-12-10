package com.jb.blog.session;

import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.sstore.SessionStore;

public class SessionConfiguration {
    /**
     * Default name of session cookie
     */
    static String DEFAULT_SESSION_COOKIE_NAME = "vertx-web.session";

    /**
     * Default path of session cookie
     */
    static String DEFAULT_SESSION_COOKIE_PATH = "/";

    /**
     * Default time, in ms, that a session lasts for without being accessed before
     * expiring.
     */
    static long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    /**
     * Default of whether a nagging log warning should be written if the session
     * handler is accessed over HTTP, not HTTPS
     */
    static boolean DEFAULT_NAG_HTTPS = true;

    /**
     * Default of whether the cookie has the HttpOnly flag set More info:
     * https://www.owasp.org/index.php/HttpOnly
     */
    static boolean DEFAULT_COOKIE_HTTP_ONLY_FLAG = false;

    /**
     * Default of whether the cookie has the 'secure' flag set to allow transmission
     * over https only. More info: https://www.owasp.org/index.php/SecureFlag
     */
    static boolean DEFAULT_COOKIE_SECURE_FLAG = false;

    /**
     * Default min length for a session id. More info:
     * https://www.owasp.org/index.php/Session_Management_Cheat_Sheet
     */
    static int DEFAULT_SESSIONID_MIN_LENGTH = 16;

    /**
     * Default of whether the session should be created lazily.
     */
    static boolean DEFAULT_LAZY_SESSION = false;

    public final String sessionCookieName;
    public final String sessionCookiePath;
    public final long sessionTimeout;
    public final boolean sessionCookieSecure;
    public final boolean sessionCookieHttpOnly;
    public final int minLength;
    public CookieSameSite cookieSameSite;
    public final boolean lazySession;

    public SessionConfiguration(
            String sessionCookieName,
            String sessionCookiePath,
            long sessionTimeout,
            boolean sessionCookieSecure,
            boolean sessionCookieHttpOnly,
            int minLength,
            boolean lazySession
    ) {
        this.sessionCookieName = sessionCookieName;
        this.sessionCookiePath = sessionCookiePath;
        this.sessionTimeout = sessionTimeout;
        this.sessionCookieSecure = sessionCookieSecure;
        this.sessionCookieHttpOnly = sessionCookieHttpOnly;
        this.minLength = minLength;
        this.lazySession = lazySession;
    }

    public static SessionConfiguration createDefault() {
        return new SessionConfiguration(
                DEFAULT_SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_PATH, DEFAULT_SESSION_TIMEOUT,
                DEFAULT_COOKIE_SECURE_FLAG, DEFAULT_COOKIE_HTTP_ONLY_FLAG,
                DEFAULT_SESSIONID_MIN_LENGTH, DEFAULT_LAZY_SESSION
        );
    }
}

