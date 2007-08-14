/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.common.util.HexUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Abstract superclass of all of the different types of audit record.
 *
 * Note that audit records should be treated as immutable, although they still need non-final fields and setters
 * for persistence purposes.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord extends SSGLogRecord implements NamedEntity, PersistentEntity {
    private long oid;
    private int version;
    private String signature;

    /** OID of the IdentityProvider that the requesting user, if any, belongs to.  -1 indicates unknown. */
    protected long identityProviderOid = IdentityProviderConfig.DEFAULT_OID;
    /** Login or name of the user that is making the request if known, or null otherwise. */
    protected String userName;
    /** Unique ID of the user that is making the request (if known), or null otherwise. */
    protected String userId;

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected AuditRecord() {
    }

    /**
     * Fills in the fields that are common to all types of AuditRecord
     * @param level the {@link Level} of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param ipAddress the IP address of the entity that caused this AuditRecord to be created.  It could be that of a cluster node, an administrative workstation or a web service requestor, or null if unavailable.
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param message a short description of the event that generated the AuditRecord
     * @param identityProviderOid the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    protected AuditRecord(Level level, String nodeId, String ipAddress, long identityProviderOid, String userName, String userId, String name, String message) {
        super(level, nodeId, HexUtils.truncStringMiddle(message, 254));
        this.name = HexUtils.truncStringMiddle(name, 254);
        this.ipAddress = ipAddress;
        this.identityProviderOid = identityProviderOid;
        this.userName = userName;
        this.userId = userId;
    }

    public void setMessage(String message) {
        message = HexUtils.truncStringMiddle(message, 254);
        super.setMessage(message);
    }

    public String getId() {
        return Long.toString(oid);
    }

    public long getOid() {
        return oid;
    }

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

    public void setDetails(Set<AuditDetail> details) {
        this.details = details;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setIpAddress( String ipAddress ) {
        this.ipAddress = ipAddress;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setName( String name ) {
        this.name = name;
    }

    /** the IP address of the entity that caused this AuditRecord to be created. */
    protected String ipAddress;

    /** the name of the service or system affected by event that generated the AuditRecord */
    protected String name;

    /** the list of {@link com.l7tech.common.audit.AuditDetail}s associated with this AuditRecord */
    private Set<AuditDetail> details = new HashSet<AuditDetail>();

    /**
     * Gets the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link com.l7tech.identity.IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @return the OID of the {@link com.l7tech.identity.IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link com.l7tech.identity.IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     */
    public long getIdentityProviderOid() {
        return identityProviderOid;
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

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setOid(long oid) {
        this.oid = oid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setVersion(int version) {
        this.version = version;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setIdentityProviderOid( long identityProviderOid ) {
        this.identityProviderOid = identityProviderOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setUserId( String userId ) {
        this.userId = userId;
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

    public abstract void serializeOtherProperties(OutputStream out) throws IOException;

    public static final String SERSEP = ":";

    public final void serializeSignableProperties(OutputStream out) throws IOException {
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

        if (userId != null) out.write(userId.getBytes());
        out.write(SERSEP.getBytes());

        out.write(Long.toString(identityProviderOid).getBytes());
        out.write(SERSEP.getBytes());

        // AdminAuditRecord does entity_class:entity_id:action
        // MessageSummaryAuditRecord does status:request_id:service_oid:operation_name:
        //      authenticated:authenticationType:request_length:response_length:request_zipxml:
        //      response_zipxml:response_status:routing_latency
        // SystemAuditRecord component_id:action
        serializeOtherProperties(out);
        out.write(SERSEP.getBytes());

        for (AuditDetail ad : details) {
            ad.serializeSignableProperties(out);
            out.write(SERSEP.getBytes());
        }
    }


    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
