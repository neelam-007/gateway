package com.l7tech.server.service.resolution;

import com.l7tech.objectmodel.imp.EntityImp;
import com.l7tech.service.ResolutionParameterTooLongException;

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
    // todo, ideally, this should fail at the database layer but mysql truncs silently
    public static final int MAX_LENGTH_RES_PARAMETER = 255;
    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) throws ResolutionParameterTooLongException {
        if (soapaction == null) soapaction = ""; // Oracle doesn't distinguish between "" and NULL
        if (soapaction.length() > MAX_LENGTH_RES_PARAMETER) {
            throw new ResolutionParameterTooLongException("The soapaction " + soapaction + " is too " +
                                                          "long to remember as a resolution parameter.");
        }
        this.soapaction = soapaction;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) throws ResolutionParameterTooLongException {
        if (urn != null && urn.length() > MAX_LENGTH_RES_PARAMETER) {
            throw new ResolutionParameterTooLongException("The namespace " + urn + " is too " +
                                                          "long to remember as a resolution parameter.");
        }
        this.urn = urn;
    }

    public long getServiceid() {
        return serviceid;
    }

    public void setServiceid(long serviceid) {
        this.serviceid = serviceid;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) throws ResolutionParameterTooLongException {
        if (uri != null && uri.length() > MAX_LENGTH_RES_PARAMETER) {
            throw new ResolutionParameterTooLongException("The URI " + uri + " is too " +
                                                          "long to remember as a resolution parameter.");
        }
        this.uri = uri;

    }
    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ResolutionParameters)) return false;
        ResolutionParameters theotherone = (ResolutionParameters)obj;
        if (uri == null) {
            if (uri != theotherone.getUri()) return false;
        } else if (!uri.equals(theotherone.getUri())) return false;
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
	    return com.l7tech.common.util.HashCode.compute(new String[]{urn, soapaction, uri});
    }

    private String soapaction;
    private String urn;
    private String uri;
    private long serviceid;
}
