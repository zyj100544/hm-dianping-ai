package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAuditResultMessage {
    private Long commentId;
    private String risk;
    private String reason;
    private List<String> flags;
}
