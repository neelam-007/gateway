package com.l7tech.service.resolution;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 25, 2003
 * Time: 11:02:34 AM
 * $Id$
 *
 * Object representation of a row in the serviceresolution table.
 *
 * This bean class is used by the ResolutionManager to interact with
 * hibernate.
 */
public class ResolutionParameters {
    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) {
        this.soapaction = soapaction;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    private String soapaction;
    private String urn;
}
