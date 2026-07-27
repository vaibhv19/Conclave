package com.conclave.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for all WebSocket real-time events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class WsEvent {
    private String type;
}
