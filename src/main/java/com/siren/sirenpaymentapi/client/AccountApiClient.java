package com.siren.sirenpaymentapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "4IREN-ACCOUNT", contextId = "accountApi", fallback = AccountApiFallback.class)
public interface AccountApiClient {

    @PostMapping("/api/account/email")
    String getEmail(@RequestBody Long userId);
}
