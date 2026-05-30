package com.hmdp.feign;

import com.hmdp.dto.BlogAssistRequest;
import com.hmdp.dto.BlogAssistResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-blog", url = "${ai.blog-assist.url}")
public interface AIBlogFeignClient {

    @PostMapping
    BlogAssistResponse assist(@RequestBody BlogAssistRequest request);
}
