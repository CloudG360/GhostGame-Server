package net.cg360.spookums.server.auth;

import java.util.UUID;
import java.util.regex.Pattern;

public final class AuthToken {

    public static final long AUTH_LENGTH = 1000L * 60 * 60 * 24 * 30;

    private final String authToken;
    private final long expireTime;


    public AuthToken(String token, long expireMillis) {
        this.authToken = token;
        this.expireTime = expireMillis;
    }



    public boolean hasExpired() {
        return System.currentTimeMillis() > expireTime;
    }


    public String getAuthToken() {
        return authToken;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public static String generateTokenString() {
        String[] uuid = UUID.randomUUID().toString().split(Pattern.quote("-"));
        return String.join("", uuid);
    }

    public static AuthToken generateToken() {
        return new AuthToken(generateTokenString(), System.currentTimeMillis() + AUTH_LENGTH);
    }
}
