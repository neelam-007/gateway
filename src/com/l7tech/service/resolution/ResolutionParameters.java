package com.l7tech.service.resolution;

import com.l7tech.objectmodel.imp.EntityImp;

import java.io.Serializable;

/**
 * Object representation of a row in the serviceresolution table.
 *
 * This bean class is used by the ResolutionManager to interact with
 * hibernate.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Nov 25, 2003<br/>
 * $Id$
 */
public class ResolutionParameters extends EntityImp implements Serializable {
    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) {
        if ( soapaction == null ) soapaction = ""; // Oracle doesn't distinguish between "" and NULL
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

    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ResolutionParameters)) return false;
        ResolutionParameters theotherone = (ResolutionParameters)obj;
        if (soapaction == null) {
            if (soapaction != theotherone.getSoapaction()) return false;
        } else if (!soapaction.equals(theotherone.getSoapaction())) return false;
        if (urn == null) {
            if (urn != theotherone.getUrn()) return false;
        } else if (!urn.equals(theotherone.getUrn())) return false;
        if (serviceid != theotherone.getServiceid()) return false;
        return true;
    }

    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public int hashCode() {
	return com.l7tech.common.util.HashCode.compute(new String[]{urn, soapaction});
    }

    private String soapaction;
    private String urn;
    private long serviceid;
}
