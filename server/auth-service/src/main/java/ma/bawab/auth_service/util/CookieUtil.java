package ma.bawab.auth_service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessTokenCookieName;

    @Value("${jwt.cookie.refresh-token-name:refresh_token}")
    private String refreshTokenCookieName;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String sameSite;

    public ResponseCookie createAccessTokenCookie(String token, long maxAgeMs) {
        return ResponseCookie.from(accessTokenCookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeMs / 1000)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token, long maxAgeMs) {
        return ResponseCookie.from(refreshTokenCookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(maxAgeMs / 1000)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie createCleanAccessTokenCookie() {
        return ResponseCookie.from(accessTokenCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }

    public Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public String getAccessTokenCookieName() {
        return accessTokenCookieName;
    }

    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }
}
