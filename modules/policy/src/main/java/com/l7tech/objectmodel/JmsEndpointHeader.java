package com.l7tech.objectmodel;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author jbufu
 */
@XmlRootElement
public class JmsEndpointHeader extends EntityHeader {

    private String providerName;
    private boolean incoming;

    public JmsEndpointHeader(String id, String name, String description, int version, boolean incoming) {
        super(id, EntityType.JMS_ENDPOINT, name, description, version);
        this.incoming = incoming;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    @Override
    public String toString() {
        return getName() + "(" + providerName + ", " + (incoming ? "incoming" : "outgoing") +")";
    }
}
