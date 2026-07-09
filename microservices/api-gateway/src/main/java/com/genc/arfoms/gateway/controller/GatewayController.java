package com.genc.arfoms.gateway.controller;

import com.genc.arfoms.gateway.service.GatewayProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class GatewayController {

    private final GatewayProxyService gatewayProxyService;
    private final Map<String, String> routeMap;

    public GatewayController(
            GatewayProxyService gatewayProxyService,
            @Value("${gateway.flight-service.base-url}") String flightBaseUrl,
            @Value("${gateway.booking-service.base-url}") String bookingBaseUrl,
            @Value("${gateway.checkin-service.base-url}") String checkinBaseUrl,
            @Value("${gateway.crew-service.base-url}") String crewBaseUrl,
            @Value("${gateway.loyalty-service.base-url}") String loyaltyBaseUrl,
            @Value("${gateway.auth-service.base-url:http://localhost:8180}") String authBaseUrl) {
        this.gatewayProxyService = gatewayProxyService;
        this.routeMap = Map.of(
                "/api/flights", flightBaseUrl,
                "/api/v1/flights", flightBaseUrl,
                "/api/bookings", bookingBaseUrl,
                "/flights", bookingBaseUrl,
                "/airline/api/seats", bookingBaseUrl,
                "/api/checkin", checkinBaseUrl,
                "/api/crew", crewBaseUrl,
                "/api/loyalty", loyaltyBaseUrl,
                "/api/auth", authBaseUrl
        );
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home() {
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>ARFOMS API Gateway</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; color: #1f2937; }
                        .card { max-width: 760px; padding: 24px; border: 1px solid #d1d5db; border-radius: 12px; }
                        code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
                        a { color: #2563eb; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>ARFOMS API Gateway</h1>
                        <p>The gateway is running successfully on <code>http://localhost:8210</code>.</p>
                        <p>Open the frontend at <a href="http://localhost:5600">http://localhost:5600</a>.</p>
                        <p>Available gateway paths:</p>
                        <ul>
                            <li><code>/api/flights</code></li>
                            <li><code>/api/bookings</code></li>
                            <li><code>/api/checkin</code></li>
                            <li><code>/api/crew</code></li>
                            <li><code>/api/loyalty</code></li>
                        </ul>
                    </div>
                </body>
                </html>
                """;
        return ResponseEntity.ok(html);
    }

    @RequestMapping(path = "/api/**", method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request,
                                               @RequestHeader HttpHeaders headers,
                                               @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        String baseUrl = resolveBaseUrl(path);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return gatewayProxyService.forward(baseUrl, path, request.getQueryString(), method, headers, body);
    }

    private String resolveBaseUrl(String path) {
        for (Map.Entry<String, String> route : routeMap.entrySet()) {
            if (path.startsWith(route.getKey())) {
                return route.getValue();
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No gateway route configured for path");
    }
}

