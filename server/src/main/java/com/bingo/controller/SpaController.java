package com.bingo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

@Controller
public class SpaController implements ErrorController {

    private static final String STATIC_ROOT = "static/";
    private static final ClassPathResource INDEX_HTML =
            new ClassPathResource(STATIC_ROOT + "index.html");

    private static String mimeFor(String path) {
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".ico"))  return "image/x-icon";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        if (path.endsWith(".map"))  return "application/json";
        if (path.endsWith(".txt"))  return "text/plain";
        return "application/octet-stream";
    }

    @RequestMapping("/error")
    @ResponseBody
    public String handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        int code = (status instanceof Integer i) ? i : 500;
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return "{\"error\": \"HTTP " + code + "\"}";
    }

    @RequestMapping("/**")
    public void serve(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Build the servlet-relative path by stripping the context path ourselves.
        // We cannot rely on getServletPath() alone — under spring-boot:run with a
        // context path set, it may return "/" for all requests routed through the
        // catch-all mapping. Instead we strip the context path from the raw URI.
        String contextPath = request.getContextPath();   // "/bingo"
        String uri         = request.getRequestURI();    // "/bingo/static/js/main.js"
        String path = uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())    // "/static/js/main.js"
                : uri;

        if (path.isEmpty()) path = "/";

        // Guard: REST endpoints are handled by GameController before reaching here.
        if (path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Try to serve a real static file from classpath:/static/<path>
        String classpathLocation = STATIC_ROOT + path.replaceFirst("^/", "");
        Resource resource = new ClassPathResource(classpathLocation);

        if (resource.exists() && resource.isReadable()) {
            response.setContentType(mimeFor(path));
            if (!path.endsWith(".ico") && !path.endsWith(".png")) {
                response.setCharacterEncoding("UTF-8");
            }
            try (InputStream in = resource.getInputStream()) {
                StreamUtils.copy(in, response.getOutputStream());
            }
            return;
        }

        // No static file matched — serve index.html for React Router.
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        try (InputStream in = INDEX_HTML.getInputStream()) {
            StreamUtils.copy(in, response.getOutputStream());
        }
    }
}
