package com.l7tech.service.resolution;

import java.io.Serializable;

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
public class ResolutionParameters implements Serializable {
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

    public long getServiceid() {
        return serviceid;
    }

    public void setServiceid(long serviceid) {
        this.serviceid = serviceid;
    }

    private String soapaction;
    private String urn;
    private long serviceid;
}
