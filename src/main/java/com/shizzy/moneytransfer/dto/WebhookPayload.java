package com.shizzy.moneytransfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WebhookPayload {
    private String event;
    @JsonProperty("event.type")
    private String eventType;
    WithdrawalData data;
}
