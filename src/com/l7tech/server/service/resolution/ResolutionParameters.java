package com.l7tech.server.service.resolution;

import com.l7tech.common.util.HashCode;
import com.l7tech.objectmodel.imp.EntityImp;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Object representation of a row in the serviceresolution table.
 * <p/>
 * This bean class is used by the ResolutionManager to interact with
 * hibernate.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 25, 2003<br/>
 * $Id$
 */
public class ResolutionParameters extends EntityImp implements Serializable {
    private static final Logger logger = Logger.getLogger(ResolutionParameters.class.getName());
    // todo, ideally, this should fail at the database layer but mysql truncs silently
    public static final int MAX_LENGTH_RES_PARAMETER = 255;

    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) {
        this.soapaction = truncate(soapaction, MAX_LENGTH_RES_PARAMETER, "soapaction");
    }

    private String truncate(String value, int max, String field) {
        if (value == null) return ""; // Oracle doesn't distinguish between "" and NULL
        if (value.length() > max) {
            logger.warning("The " + field + " " + value + " is too long to remember as a resolution parameter.");
            return value.substring(0, max);
        } else
            return value;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = truncate(urn, MAX_LENGTH_RES_PARAMETER, "namespace");
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

    public void setUri(String uri) {
        this.uri = truncate(uri, MAX_LENGTH_RES_PARAMETER, "URI");
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
     * Test whether two resolution parameters are equal. Two resolution
     * parameters are equal if the uri, soap action and the urn are equal
     *
     * @param that the target <code>ResolutionParameters</code>
     * @return true if equal, false oterwise
     */
    public boolean resolutionEquals(ResolutionParameters that) {
        if (uri == null) {
            if (uri != that.getUri()) return false;
        } else if (!uri.equals(that.getUri())) return false;
        if (soapaction == null) {
            if (soapaction != that.getSoapaction()) return false;
        } else if (!soapaction.equals(that.getSoapaction())) return false;
        if (urn == null) {
            if (urn != that.getUrn()) return false;
        } else if (!urn.equals(that.getUrn())) return false;

        return true;
    }

    /**
     * this must be overriden (hibernate requirement for composite id classes)
     */
    public int hashCode() {
        return HashCode.compute(new String[]{urn, soapaction, uri});
    }


    public String toString() {
        return "ResolutionParameters{" +
          "serviceid=" + serviceid +
          ", soapaction='" + soapaction + "'" +
          ", urn='" + urn + "'" +
          ", uri='" + uri + "'" +
          "}";
    }

    private String soapaction;
    private String urn;
    private String uri;
    private long serviceid;

}
