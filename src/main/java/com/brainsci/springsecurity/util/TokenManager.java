package com.brainsci.springsecurity.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ...
 *
 * @author izhx 2018/7/26 9:23
 */

public class TokenManager {

    private final static Integer TIMESTAMP_BEGIN = 0;
    private final static Integer TIMESTAMP_LEN = 13;
    private final static Integer SESSIONID_BEGIN = 13;
    private final static Integer SESSIONID_LEN = 32;
    private final static Integer USERNAME_BEGIN = TIMESTAMP_LEN + SESSIONID_LEN;

    public static ConcurrentHashMap<String, Long> tokenPool = new ConcurrentHashMap<String, Long>() {{
        this.put("1111111111111111111111111111111111111111111110918160108", (long) 1);
    }};

    public static void addTestToken(String username) {
        tokenPool.put("11111111111111111111111111111111111111111111f" + username, (long) 1);
    }

    public static int getTokenNum() {
        return tokenPool.size();
    }

    /**
     * 上古秘籍 之 翻转加密 大道至简
     */
    public static String createToken(String sessionId, String username) {
        Long time = System.currentTimeMillis();
        String timestamp = (time).toString();  // 精确到毫秒
        String pmatsemit = new StringBuffer(timestamp).reverse().toString();
        String dInoisses = new StringBuffer(sessionId).reverse().toString();
        String token = pmatsemit + dInoisses + username;
        tokenPool.put(token, time);
        return token;
    }

    public static String getUsername(String token) {
        return token.substring(USERNAME_BEGIN);
    }

    public static String getSessionId(String token) {
        String dInoisses = token.substring(SESSIONID_BEGIN, SESSIONID_BEGIN + SESSIONID_LEN);
        return new StringBuffer(dInoisses).reverse().toString();
    }

    public static Long getTimestamp(String token) {
//        String pmatsemit = token.substring(TIMESTAMP_BEGIN, TIMESTAMP_BEGIN + TIMESTAMP_LEN - 1);
//        String timestamp = new StringBuffer(pmatsemit).reverse().toString();
//        Long tokenTimestamp = Long.valueOf(timestamp);
        return tokenPool.get(token);
    }

    public static boolean validateToken(String token) {
        return tokenPool.containsKey(token);
    }

    public static void removeToken(String token) {
        tokenPool.remove(token);
    }

}
