package com.likelion.animalface.global.advice;

import com.likelion.animalface.global.dto.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.likelion.animalFace.domain")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 이미 ApiResponse 타입이거나 String 타입인 경우 제외
        return !returnType.getParameterType().equals(ApiResponse.class) &&
                !returnType.getParameterType().equals(String.class);
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, org.springframework.http.server.ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response) {
        // 데이터가 null인 경우에도 성공 응답 규격을 맞춤
        if (body == null) {
            return ApiResponse.success(null);
        }

        // 도메인 컨트롤러에서 반환된 객체를 ApiResponse로 래핑
        return ApiResponse.success(body);
    }
}
