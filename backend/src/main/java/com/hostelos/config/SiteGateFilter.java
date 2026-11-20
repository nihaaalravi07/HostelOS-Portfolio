package com.hostelos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Optional shared-passcode gate for a PUBLIC deployment (free hosting URLs are reachable by anyone who has the link).
 *
 * Enabled only when SITE_PASSCODE is set; otherwise this filter does nothing (local development is unaffected).
 * Visitors without the cookie see a one-field passcode page; the right passcode sets an HttpOnly cookie. It covers the
 * UI, the API and Swagger. This keeps casual visitors and crawlers out of the demo. It is a convenience gate, not
 * strong security: there is no rate limiting, so use a long passcode and do not reuse a real password.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SiteGateFilter extends OncePerRequestFilter {
    static final String COOKIE = "hostelos_gate";

    private final String passcode;
    private final String cookieValue;

    public SiteGateFilter(@Value("${hostelos.site-passcode:}") String passcode) {
        this.passcode = passcode == null ? "" : passcode;
        this.cookieValue = this.passcode.isEmpty() ? "" : hmac(this.passcode);
    }

    public boolean enabled() { return !passcode.isEmpty(); }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (!enabled() || path.equals("/healthz")) { chain.doFilter(req, res); return; }

        if (path.equals("/gate") && "POST".equals(req.getMethod())) { submit(req, res); return; }
        if (hasValidCookie(req)) { chain.doFilter(req, res); return; }

        res.setHeader("Cache-Control", "no-store");
        boolean api = path.startsWith("/api/") || path.startsWith("/v3/") || path.startsWith("/swagger-ui");
        if (api) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Passcode required\"}");
        } else {
            writeGatePage(res, false);
        }
    }

    private void submit(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String given = req.getParameter("passcode");
        boolean ok = given != null && MessageDigest.isEqual(hmac(given).getBytes(StandardCharsets.UTF_8),
                cookieValue.getBytes(StandardCharsets.UTF_8));            // constant-time comparison
        res.setHeader("Cache-Control", "no-store");
        if (!ok) { writeGatePage(res, true); return; }
        Cookie c = new Cookie(COOKIE, cookieValue);
        c.setHttpOnly(true);
        c.setSecure(req.isSecure());                                      // true behind the platform's HTTPS proxy
        c.setPath("/");
        c.setMaxAge(7 * 24 * 3600);
        res.addCookie(c);
        res.sendRedirect("/");
    }

    private boolean hasValidCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return false;
        for (Cookie c : req.getCookies()) {
            if (COOKIE.equals(c.getName()) && MessageDigest.isEqual(c.getValue().getBytes(StandardCharsets.UTF_8),
                    cookieValue.getBytes(StandardCharsets.UTF_8))) return true;
        }
        return false;
    }

    private void writeGatePage(HttpServletResponse res, boolean wrong) throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write("""
            <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>HostelOS</title><style>
            body{margin:0;min-height:100vh;display:grid;place-items:center;background:#f8f5ee;color:#23262b;font-family:'DM Sans',system-ui,sans-serif}
            main{width:min(360px,88vw)}h1{font:500 30px Georgia,serif;color:#213629;margin:0 0 6px}
            p{margin:0 0 18px;color:#6c6f66;font-size:14px}input{box-sizing:border-box;width:100%%;padding:10px 12px;border:1px solid #e4decf;border-radius:6px;font-size:15px;background:#fffefa}
            button{margin-top:12px;width:100%%;padding:11px;border:0;border-radius:6px;background:#2e4a3a;color:#f8f5ee;font-size:15px;font-weight:600;cursor:pointer}
            .e{color:#b4532a;font-size:13px;margin:10px 0 0}</style></head><body><main>
            <h1>HostelOS</h1><p>This demo is private. Enter the access passcode shared by the team.</p>
            <form method="post" action="/gate"><input type="password" name="passcode" placeholder="Passcode" autofocus required autocomplete="current-password">
            <button type="submit">Continue</button>%s</form></main></body></html>
            """.formatted(wrong ? "<p class=\"e\" role=\"alert\">That passcode is not right.</p>" : ""));
    }

    private static String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("hostelos-site-gate".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
