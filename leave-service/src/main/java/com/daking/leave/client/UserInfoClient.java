package com.daking.leave.client;

import com.daking.auth.api.service.UserInfoApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "auth-service", configuration = UserInfoFeignConfig.class)
public interface UserInfoClient extends UserInfoApi {
        // This interface intentionally left blank.
        // It inherits all endpoint definitions from UserInfoApi.
        // The Feign client will automatically create implementations for them.
        // The Authorization header is handled by the RequestInterceptor in
        // UserInfoFeignConfig.
}