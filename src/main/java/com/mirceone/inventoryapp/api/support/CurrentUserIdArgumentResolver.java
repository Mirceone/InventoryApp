package com.mirceone.inventoryapp.api.support;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            if (annotation != null && !annotation.required()) {
                return null;
            }
            throw new ResponseStatusException(UNAUTHORIZED, "Missing or invalid access token");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid access token subject");
        }
    }
}
