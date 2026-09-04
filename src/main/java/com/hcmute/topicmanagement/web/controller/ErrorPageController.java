package com.hcmute.topicmanagement.web.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorPageController implements ErrorViewResolver {

    private static final Map<Integer, String> ERROR_PATHS = Map.of(
            400, "/bad-request",
            401, "/unauthorized",
            403, "/forbidden",
            404, "/not-found",
            500, "/internal-server-error",
            503, "/service-unavailable");

    @GetMapping({"/bad-request", "/error/400"})
    public String badRequest(HttpServletResponse response) {
        return render(response, 400);
    }

    @GetMapping({"/unauthorized", "/error/401"})
    public String unauthorized(HttpServletResponse response) {
        return render(response, 401);
    }

    @GetMapping({"/forbidden", "/error/403"})
    public String forbidden(HttpServletResponse response) {
        return render(response, 403);
    }

    @GetMapping({"/not-found", "/error/404"})
    public String notFound(HttpServletResponse response) {
        return render(response, 404);
    }

    @GetMapping({"/internal-server-error", "/error/500"})
    public String internalServerError(HttpServletResponse response) {
        return render(response, 500);
    }

    @GetMapping({"/service-unavailable", "/error/503"})
    public String serviceUnavailable(HttpServletResponse response) {
        return render(response, 503);
    }

    private String render(HttpServletResponse response, int statusCode) {
        response.setStatus(statusCode);
        return "error/" + statusCode;
    }

    @Override
    public ModelAndView resolveErrorView(
            jakarta.servlet.http.HttpServletRequest request,
            HttpStatus status,
            java.util.Map<String, Object> model) {
        if (status == null || !ERROR_PATHS.containsKey(status.value())) {
            return null;
        }
        return new ModelAndView("redirect:" + ERROR_PATHS.get(status.value()));
    }
}
