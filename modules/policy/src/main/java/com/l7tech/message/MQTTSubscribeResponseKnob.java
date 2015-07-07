package com.l7tech.message;

import java.util.List;

/**
 * The MQTT subscription response knob
 */
public interface MQTTSubscribeResponseKnob extends MessageKnob {
    /**
     * This is the list of qos's granted from the mqtt broker.
     *
     * @return The list of granted qos's
     */
    String getGrantedQOS();

    /**
     * Sets the granted qos's
     *
     * @param grantedQOS The list of granted qos's
     */
    void setGrantedQOS(String grantedQOS);
}
