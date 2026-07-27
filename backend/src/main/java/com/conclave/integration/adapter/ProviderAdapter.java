package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import java.util.List;

public interface ProviderAdapter {
    /**
     * Translates the canonical conversation history combined with the WorkflowState summary 
     * into a vendor-specific request payload.
     *
     * @param history The canonical conversation history
     * @param state   The current workflow state summary
     * @return The vendor-specific request payload object
     */
    Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state);

    /**
     * Translates a vendor-specific response payload back into a CanonicalMessage.
     *
     * @param response The vendor-specific response payload object
     * @return The translated CanonicalMessage
     */
    CanonicalMessage fromProviderFormat(Object response);
}
