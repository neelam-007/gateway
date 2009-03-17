package com.l7tech.server.event;

import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationEvent;

/**
 * Event generated when a SOAP Fault response is sent (an SSG generated fault).
 * <p/>
 * This event is sent and received on the high-speed messageProcessingEventChannel rather than the
 * primary Spring ApplicationEvent channel.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class FaultProcessed extends ApplicationEvent {

    //- PUBLIC

    public FaultProcessed(PolicyEnforcementContext context, String faultMessage, MessageProcessor messageProcessor) {
        super(messageProcessor);
        this.context = context;
        this.faultMessage = faultMessage;
    }

    public PolicyEnforcementContext getContext() {
        return context;
    }

    public String getFaultMessage() {
        return faultMessage;
    }

    //- PRIVATE

    private final PolicyEnforcementContext context;
    private final String faultMessage;
}
