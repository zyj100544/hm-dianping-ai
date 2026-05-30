package com.hmdp.feign;

import com.hmdp.dto.SemanticSearchRequest;
import com.hmdp.dto.SemanticSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-search", url = "${ai.search.url}")
public interface AISearchFeignClient {

    @PostMapping
    SemanticSearchResponse search(@RequestBody SemanticSearchRequest request);
}
