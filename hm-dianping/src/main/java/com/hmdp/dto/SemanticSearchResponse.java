package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {
    private List<SemanticSearchItem> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemanticSearchItem {
        private Long shopId;
        private Double relevance;
    }
}
