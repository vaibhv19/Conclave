package com.conclave.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event broadcast when an AI agent starts its turn (thinking/generating).
 */
@Getter
@Setter
@NoArgsConstructor
public class TurnStartedEvent extends WsEvent {
    private String roleName;
    private String modelId;
    private boolean isMocked;

    public TurnStartedEvent(String roleName, String modelId, boolean isMocked) {
        super("TURN_STARTED");
        this.roleName = roleName;
        this.modelId = modelId;
        this.isMocked = isMocked;
    }
}
