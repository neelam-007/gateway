package com.l7tech.message;

import java.util.Map;

/**
 * Message knob for messages received from an email listener.
 */
public interface EmailKnob extends MessageKnob, HasSoapAction {
    /**
     * @return a map of JMS message properties
     */
    Map<String, Object> getEmailMsgPropMap();
}
