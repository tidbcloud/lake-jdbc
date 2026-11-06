package com.tidbcloud.jdbc.internal.session;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LakeSessionCookieJar implements CookieJar {
    private final Map<String, Cookie> cookieStore = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            add(cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        cookieStore.forEach((key, cookie) -> {
            if (cookie.expiresAt() <= now) {
                cookieStore.remove(key, cookie);
            } else if (cookie.matches(url)) {
                result.add(cookie);
            }
        });
        result.sort((left, right) -> Integer.compare(right.path().length(), left.path().length()));
        return result;
    }

    public void add(Cookie cookie) {
        cookieStore.put(cookie.domain() + "\n" + cookie.path() + "\n" + cookie.name(), cookie);
    }
}
