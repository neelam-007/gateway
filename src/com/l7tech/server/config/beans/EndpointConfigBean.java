package com.l7tech.server.config.beans;

import com.l7tech.common.transport.SsgConnector;

import java.util.Collection;
import java.util.List;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 12:22:00 PM
 */
public class EndpointConfigBean extends BaseConfigurationBean{
    private final static String NAME = "Endpoint Configuration";
    private final static String DESCRIPTION = "Configures Gateway Endpoints";

    private List<SsgConnector> endpointsToAdd;
    private Collection<SsgConnector> legacyEndpoints;

    public EndpointConfigBean() {
        super(NAME, DESCRIPTION);
    }

    public void reset() {
    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (endpointsToAdd == null || endpointsToAdd.size() == 0)
            explanations.add("\tNo new endpoints to add.\n");
        else {
            explanations.add("\tAdd the following new endpoints");
            for (SsgConnector endpoint : endpointsToAdd) {
                String info = endpoint.getName() + "(" + endpoint.getPort() + ")";
                explanations.add("\t\t" + info);
            }
        }

        if (legacyEndpoints != null && !legacyEndpoints.isEmpty()) {
            explanations.add("\tImport the following existing legacy endpoints");
            for (SsgConnector legacyOne : legacyEndpoints) {
                String info = legacyOne.getName() + "(" + legacyOne.getPort() + ")";
                explanations.add("\t\t" + info);
            }
        }
    }

    public Collection<SsgConnector> getLegacyEndpoints() {
        return legacyEndpoints;
    }

    public void setLegacyEndpoints(Collection<SsgConnector> legacyEndpoints) {
        this.legacyEndpoints = legacyEndpoints;
    }

    public void setEndpointsToAdd(List<SsgConnector> endpointsToAdd) {
        this.endpointsToAdd = endpointsToAdd;
    }

    public Collection<SsgConnector> getEndpointsToAdd() {
        return endpointsToAdd;
    }
}
