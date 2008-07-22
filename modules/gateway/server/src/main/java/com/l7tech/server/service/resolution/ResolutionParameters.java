package com.l7tech.server.service.resolution;

import com.l7tech.util.HashCode;
import com.l7tech.util.HexUtils;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
public class ResolutionParameters extends PersistentEntityImp implements Serializable {
    private static final Logger logger = Logger.getLogger(ResolutionParameters.class.getName());
    // todo, ideally, this should fail at the database layer but mysql truncs silently
    //public static final int MAX_LENGTH_RES_PARAMETER = 255;

    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) {
        this.soapaction = soapaction;
        resetHash();
    }

    public String getDigested() {
        return digested;
    }

    public void setDigested(String digested) {
        this.digested = digested;
    }

    private void resetHash() {
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "cannot happen", e);
            throw new RuntimeException(e);
        }
        String toDigest = soapaction + urn + uri;
        digested = HexUtils.hexDump(digester.digest(toDigest.getBytes()));
    }

    /*private String truncate(String value, int max, String field) {
        if (value == null) return ""; // Oracle doesn't distinguish between "" and NULL
        if (value.length() > max) {
            logger.warning("The " + field + " " + value + " is too long to remember as a resolution parameter.");
            return value.substring(0, max);
        } else
            return value;
    }*/

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
        resetHash();
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
        this.uri = uri;
        resetHash();
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
    private String digested;

}
