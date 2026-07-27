package com.conclave.dto.ws;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event broadcast during coordinator system interventions (for Phase 08).
 */
@Getter
@Setter
@NoArgsConstructor
public class SystemInterventionEvent extends WsEvent {
    private String reason;

    public SystemInterventionEvent(String reason) {
        super("SYSTEM_INTERVENTION");
        this.reason = reason;
    }
}
