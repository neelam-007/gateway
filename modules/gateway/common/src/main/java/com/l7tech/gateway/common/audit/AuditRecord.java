package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Abstract superclass of all of the different types of audit record.
 *
 * Note that audit records should be treated as immutable, although they still need non-final fields and setters
 * for persistence purposes.
 * Note: instances are mostly treated as immutable apart from Details, which can be added after instance construction.
 *
 * @author alex
 */
public abstract class AuditRecord implements NamedEntity, PersistentEntity, Serializable {
    private static Logger logger = Logger.getLogger(AuditRecord.class.getName());
    private static AtomicLong globalSequenceNumber = new AtomicLong(0L);

    public static final String SERSEP = ":";

    private Goid goid;
    private int version;
    private String signature;
    private String nodeId;
    private String message;
    private Level level;
    private long millis;
    private final long sequenceNumber;

    /** OID of the IdentityProvider that the requesting user, if any, belongs to. */
    protected Goid identityProviderGoid = IdentityProviderConfig.DEFAULT_GOID;
    /** Login or name of the user that is making the request if known, or null otherwise. */
    protected String userName;
    /** Unique ID of the user that is making the request (if known), or null otherwise. */
    protected String userId;
    /** Unique ID for the request if any, or null otherwise */
    protected String requestId;
    /** the IP address of the entity that caused this AuditRecord to be created. */
    protected String ipAddress;
    /** the name of the service or system affected by event that generated the AuditRecord */
    protected String name;
    /** the list of {@link com.l7tech.gateway.common.audit.AuditDetail}s associated with this AuditRecord */
    private Set<AuditDetail> details = new HashSet<AuditDetail>();

    private static final Comparator<AuditDetail> COMPARE_BY_ORDINAL = new Comparator<AuditDetail>() {
        @Override
        public int compare(AuditDetail a, AuditDetail b) {
            int ao = a.getOrdinal();
            int bo = b.getOrdinal();
            return (ao < bo ? -1 : (ao == bo ? 0 : 1));
        }
    };

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected AuditRecord() {
        this.sequenceNumber = globalSequenceNumber.incrementAndGet();
        this.level = Level.FINEST;
        this.millis = System.currentTimeMillis();
    }

    /**
     * Fills in the fields that are common to all types of AuditRecord
     * @param level the {@link Level} of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param ipAddress the IP address of the entity that caused this AuditRecord to be created.  It could be that of a cluster node, an administrative workstation or a web service requestor, or null if unavailable.
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param message a short description of the event that generated the AuditRecord
     * @param identityProviderOid the GOID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_GOID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the GOID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    protected AuditRecord(Level level, String nodeId, String ipAddress, Goid identityProviderOid, String userName, String userId, String name, String message) {
        this.sequenceNumber = globalSequenceNumber.incrementAndGet();
        this.level = level;
        this.millis = System.currentTimeMillis();
        this.message = TextUtils.truncStringMiddle(message, 254);
        this.nodeId = nodeId;
        this.name = TextUtils.truncStringMiddle(name, 254);
        this.ipAddress = ipAddress;
        this.identityProviderGoid = identityProviderOid;
        this.userName = userName;
        this.userId = userId;
    }

    /**
     * Get the logging level of the log. For serialization purposes only.
     * @return String the logging level.
     */
    public String getStrLvl() {
        return getLevel().getName();
    }

    /**
     * Set the logging level of the log. For serialization purposes only.
     * @param arg  the logging level of the log.
     * @deprecated for serialization use only
     */
    @Deprecated
    public void setStrLvl(String arg) {
        setLevel(Level.parse(arg));
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * Set node Id. The node is the one on which this audit was generated.
     *
     * @param nodeId  the node id.
     * @deprecated for serialization use only
     */
    @Deprecated
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getMillis() {
        return millis;
    }

    /**
     * @deprecated for serialization use only
     */
    @Deprecated
    public void setMillis( final long millis ) {
        this.millis = millis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = TextUtils.truncStringMiddle(message, 254);
    }

    @Override
    public String getId() {
        return Goid.toString(goid);
    }

    @Override
    public Goid getGoid() {
        return goid;
    }

    @Override
    public int getVersion() {
        return version;
    }

    /**
     * Gets the IP address of the entity that caused this AuditRecord to be created.  It could be that of a cluster node, an administrative workstation or a web service requestor.
     * @return the IP address of the entity that caused this AuditRecord to be created, or null if there isn't one.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets the name of the service or system affected by event that generated the AuditRecord
     * @return the name of the service or system affected by event that generated the AuditRecord
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the list of {@link AuditDetail} records associated with this audit record.
     * @return the list of {@link AuditDetail} records associated with this audit record.
     */
    public Set<AuditDetail> getDetails() {
        return details;
    }

    /**
     * Gets the list of @{link AuditDetail} records associated with this audit record, ordered by ordinal
     * @return
     */
    public AuditDetail[] getDetailsInOrder() {
        final List<AuditDetail> details = new ArrayList<AuditDetail>(getDetails());
        Collections.sort(details, COMPARE_BY_ORDINAL);
        return details.toArray(new AuditDetail[details.size()]);
    }

    public void setDetails(Set<AuditDetail> details) {
        this.details = details;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setIpAddress( String ipAddress ) {
        this.ipAddress = ipAddress;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setName( String name ) {
        this.name = name;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel( final Level level ) {
        this.level = level;
    }

    /**
     * Gets the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link com.l7tech.identity.IdentityProviderConfig#DEFAULT_GOID} if the request was not authenticated.
     * @return the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link com.l7tech.identity.IdentityProviderConfig#DEFAULT_GOID} if the request was not authenticated.
     */
    public Goid getIdentityProviderGoid() {
        return identityProviderGoid;
    }

    /**
     * Gets the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @return the name or login of the user who was authenticated, or null if the request was not authenticated.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     * @return the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    public String getUserId() {
        return userId;
    }

    @Override
    @Transient
    @XmlTransient
    public boolean isUnsaved() {
        return DEFAULT_GOID.equals(goid);
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    @Override
    public void setGoid(Goid oid) {
        this.goid = oid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setIdentityProviderGoid(Goid identityProviderOid) {
        this.identityProviderGoid = identityProviderOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setUserId( String userId ) {
        this.userId = userId;
    }

    /**
     * Get the id of the request being processed when this log record was generated.
     *
     * @return RequestId the id of the request associated with the log.
     * @see RequestId
     */
    public RequestId getReqId() {
        if (requestId == null || requestId.length() <= 0) return null;
        return new RequestId(requestId);
    }

    /**
     * Get the requestId of the log record. For serialization purposes only.
     * @return String the request Id.
     */
    public String getStrRequestId() {
        return requestId;
    }

    /**
     * Set the requestId of the log record. For serialization purposes only.
     * @param requestId the request Id.
     * @deprecated for serialization use only
     */
    @Deprecated
    public void setStrRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Get the (transient) sequence number for this audit.
     *
     * <p>WARNING: This is not a persistent value, the value for an audit
     * record will change each time the object is recreated. This value IS NOT
     * unique across the cluster and the sequence will restart when the Gateway
     * is restarted.</p>
     *
     * @return The transient sequence number.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Ensure we don't send any Hibernate implementation classes over the wire.
     *
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        details = new LinkedHashSet<AuditDetail>(details);
        out.defaultWriteObject();
    }

    public abstract void serializeOtherProperties(OutputStream out, boolean includeAllOthers, boolean calculatePre80) throws IOException;
    
    // NOTE: AuditExporterImpl must use the same columns and ordering as this method
    public final void serializeSignableProperties(OutputStream out, boolean useOldId) throws IOException {
        outputProperties(out, true, useOldId);
    }

    private void outputProperties(OutputStream out, boolean includeAllOthers, boolean calculatePre80) throws IOException {
        // previous format:
        // objectid:nodeid:time:audit_level:name:message:ip_address:user_name:user_id:provider_oid:
        //
        // from other classes:
        // entity_class:entity_id:action:   objectid:status:request_id:service_oid:operation_name:authenticated:authenticationType:request_length:response_length:request_zipxml:
        // response_zipxml:response_status:routing_latency:objectid:component_id:action:audit_associated_logs


        // Note that object ids cannot be included in the calculation since it changes once saved

        if (nodeId != null) out.write(nodeId.getBytes());
        out.write(SERSEP.getBytes());

        out.write(Long.toString(getMillis()).getBytes());
        out.write(SERSEP.getBytes());

        if (getLevel() != null) out.write(getLevel().toString().getBytes());
        out.write(SERSEP.getBytes());

        if (name != null) out.write(name.getBytes());
        out.write(SERSEP.getBytes());

        if (getMessage() != null) out.write(getMessage().getBytes());
        out.write(SERSEP.getBytes());

        if (ipAddress != null) out.write(ipAddress.getBytes());
        out.write(SERSEP.getBytes());

        if (userName != null) out.write(userName.getBytes());
        out.write(SERSEP.getBytes());

        if (userId != null){
            if(calculatePre80){
                if(ValidationUtils.isValidGoid(userId, false))
                    out.write( Long.toString(Goid.parseGoid(userId).getLow()).getBytes());
                else{
                    out.write(userId.getBytes());
                }
            }else
                out.write(userId.getBytes());
        }
        out.write(SERSEP.getBytes());

        if (identityProviderGoid != null) out.write(calculatePre80?Long.toString(identityProviderGoid.getLow()).getBytes():Goid.toString(identityProviderGoid).getBytes());
        out.write(SERSEP.getBytes());

        // AdminAuditRecord does entity_class:entity_id:action
        // MessageSummaryAuditRecord does status:request_id:service_oid:operation_name:
        //      authenticated:authenticationType:request_length:response_length:request_zipxml:
        //      response_zipxml:response_status:routing_latency
        // SystemAuditRecord component_id:action
        serializeOtherProperties(out, includeAllOthers, calculatePre80);

        if (details != null && details.size() > 0) {
            List<AuditDetail> sorteddetails = new ArrayList<AuditDetail>(details);
            Collections.sort(sorteddetails);

            out.write("[".getBytes());
            for (Iterator itor = sorteddetails.iterator(); itor.hasNext();) {
                AuditDetail ad = (AuditDetail)itor.next();
                ad.serializeSignableProperties(out,calculatePre80);
                if (itor.hasNext()) out.write(",".getBytes());
            }
            out.write("]".getBytes());
        }
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            outputProperties(baos, false, false);
        } catch (IOException e) {
            // should not happen on a byte array; fallback to super
            return super.toString();
        }
        return baos.toString();
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public byte[] computeOldIdSignatureDigest() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("should not happen", e);
        }
        byte[] digestvalue = null;
        try {
            serializeSignableProperties(baos,true);
            digestvalue = digest.digest(baos.toByteArray());
        } catch (IOException e) {
            logger.log(Level.WARNING, "could not serialize audit record", e);
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "error closing stream", e);
            }
        }

        return digestvalue;
    }

    public byte[] computeSignatureDigest() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("should not happen", e);
            }
            byte[] digestvalue = null;
            try {
                serializeSignableProperties(baos,false);
                digestvalue = digest.digest(baos.toByteArray());
            } catch (IOException e) {
                logger.log(Level.WARNING, "could not serialize audit record", e);
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "error closing stream", e);
                }
            }

        return digestvalue;
    }

    /**
     * Check if the two <CODE>AuditRecord</CODE> objects are the same.
     *
     * @param obj the object to be compared with.
     * @return TRUE if the two objects are the same. FALSE otherwise.
     */
    public boolean equals(Object obj) {
        AuditRecord theOtherOne;
        if (obj instanceof AuditRecord) theOtherOne = (AuditRecord)obj;
        else return false;
        if (nodeId != null) {
            if (!nodeId.equals(theOtherOne.getNodeId())) return false;
        }
        if (requestId != null) {
            if (!requestId.equals(theOtherOne.getStrRequestId())) return false;
        }
        //return super.equals(obj);
        return true;
    }

    public int hashCode() {
        int result;
        result = (requestId != null ? requestId.hashCode() : 0);
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        return result;
    }
}
