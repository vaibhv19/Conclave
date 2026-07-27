package com.conclave.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * Event broadcast for each generated text token/chunk.
 */
@Getter
@Setter
@NoArgsConstructor
public class ContentChunkEvent extends WsEvent {
    private String delta;
    private UUID messageId;

    public ContentChunkEvent(String delta, UUID messageId) {
        super("CONTENT_CHUNK");
        this.delta = delta;
        this.messageId = messageId;
    }
}
