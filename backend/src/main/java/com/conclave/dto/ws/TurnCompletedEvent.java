package com.conclave.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * Event broadcast when an AI agent completes its response generation and audits.
 */
@Getter
@Setter
@NoArgsConstructor
public class TurnCompletedEvent extends WsEvent {
    private UUID messageId;
    private String summary;
    private UsageInfo usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageInfo {
        private int promptTokens;
        private int completionTokens;
    }

    public TurnCompletedEvent(UUID messageId, String summary, int promptTokens, int completionTokens) {
        super("TURN_COMPLETED");
        this.messageId = messageId;
        this.summary = summary;
        this.usage = new UsageInfo(promptTokens, completionTokens);
    }
}
