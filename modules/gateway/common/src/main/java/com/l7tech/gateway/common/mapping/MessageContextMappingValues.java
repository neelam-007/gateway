package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.util.HexUtils;

import javax.persistence.*;

import org.hibernate.annotations.Proxy;

/**
 * Value information for a message context mapping.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
@Entity
@Proxy(lazy=false)
@Table(name="message_context_mapping_values")
public class MessageContextMappingValues extends PersistentEntityImp {
    private static final int MAX_VALUE_LENGTH = 255;

    private long mappingKeysOid;
    private long createTime;
    private String digested;

    // special case mappings
    private Long authUserProviderId;
    private String authUserId;
    private String authUserDescription; // this is not part of the identity
    private String serviceOperation;

    // general purpose mappings
    private String mapping1_value;
    private String mapping2_value;
    private String mapping3_value;
    private String mapping4_value;
    private String mapping5_value;

    private MessageContextMappingKeys mappingKeysEntity;

    @Column(name="digested")
    public String getDigested() {
        return digested;
    }

    public void setDigested(String digested) {
        this.digested = digested;
    }

    @Column(name="mapping_keys_oid")
    public long getMappingKeysOid() {
        return mappingKeysOid;
    }

    public void setMappingKeysOid(long mappingKeysOid) {
        this.mappingKeysOid = mappingKeysOid;
    }

    @Column(name="create_time")
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Column(name="auth_user_provider_id")
    public Long getAuthUserProviderId() {
        return authUserProviderId;
    }

    public void setAuthUserProviderId(Long authUserProviderId) {
        this.authUserProviderId = authUserProviderId;
    }

    @Column(name="auth_user_id", length=255)
    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    @Column(name="auth_user_description", length=255)
    public String getAuthUserDescription() {
        return authUserDescription;
    }

    public void setAuthUserDescription(String authUserDescription) {
        this.authUserDescription = authUserDescription;
    }

    @Column(name="service_operation", length=255)
    public String getServiceOperation() {
        return serviceOperation;
    }

    public void setServiceOperation(String serviceOperation) {
        this.serviceOperation = serviceOperation;
    }

    @Column(name="mapping1_value", length= MAX_VALUE_LENGTH)
    public String getMapping1_value() {
        return mapping1_value;
    }

    public void setMapping1_value(String mapping1_value) {
        this.mapping1_value = mapping1_value;
    }

    @Column(name="mapping2_value", length= MAX_VALUE_LENGTH)
    public String getMapping2_value() {
        return mapping2_value;
    }

    public void setMapping2_value(String mapping2_value) {
        this.mapping2_value = mapping2_value;
    }

    @Column(name="mapping3_value", length= MAX_VALUE_LENGTH)
    public String getMapping3_value() {
        return mapping3_value;
    }

    public void setMapping3_value(String mapping3_value) {
        this.mapping3_value = mapping3_value;
    }

    @Column(name="mapping4_value", length= MAX_VALUE_LENGTH)
    public String getMapping4_value() {
        return mapping4_value;
    }

    public void setMapping4_value(String mapping4_value) {
        this.mapping4_value = mapping4_value;
    }

    @Column(name="mapping5_value", length= MAX_VALUE_LENGTH)
    public String getMapping5_value() {
        return mapping5_value;
    }

    public void setMapping5_value(String mapping5_value) {
        this.mapping5_value = mapping5_value;
    }

    public String[] obtainValues() {
        return new String[] {
            mapping1_value,
            mapping2_value,
            mapping3_value,
            mapping4_value,
            mapping5_value
        };
    }

    public void setValue(int idx, String value) {
        switch (idx) {
            case 0:
                setMapping1_value(value);
                break;
            case 1:
                setMapping2_value(value);
                break;
            case 2:
                setMapping3_value(value);
                break;
            case 3:
                setMapping4_value(value);
                break;
            case 4:
                setMapping5_value(value);
                break;
            default :
                break;
        }
    }

    @ManyToOne
    @JoinColumn(name="mapping_keys_oid", insertable=false, updatable=false)
    public MessageContextMappingKeys getMappingKeysEntity() {
        return mappingKeysEntity;
    }

    public void setMappingKeysEntity(MessageContextMappingKeys mappingKeysEntity) {
        this.mappingKeysEntity = mappingKeysEntity;
    }

    /**
     * Check if the values for this object match those of the given object.
     *
     * <p>This test ignores object identity.</p>
     *
     * @param values The values to check
     * @return true if matches
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean matches( final MessageContextMappingValues values ) {
        if (mappingKeysOid != values.mappingKeysOid) return false;
        if (serviceOperation != null ? !serviceOperation.equals(values.serviceOperation) : values.serviceOperation != null)
            return false;
        if (authUserId != null ? !authUserId.equals(values.authUserId) : values.authUserId != null) return false;
        if (authUserProviderId != null ? !authUserProviderId.equals(values.authUserProviderId) : values.authUserProviderId != null)
            return false;
        if (mapping1_value != null ? !mapping1_value.equals(values.mapping1_value) : values.mapping1_value != null)
            return false;
        if (mapping2_value != null ? !mapping2_value.equals(values.mapping2_value) : values.mapping2_value != null)
            return false;
        if (mapping3_value != null ? !mapping3_value.equals(values.mapping3_value) : values.mapping3_value != null)
            return false;
        if (mapping4_value != null ? !mapping4_value.equals(values.mapping4_value) : values.mapping4_value != null)
            return false;
        if (mapping5_value != null ? !mapping5_value.equals(values.mapping5_value) : values.mapping5_value != null)
            return false;

        return true;
    }

    /**
     * Create a guid for the set of ordered values.  The guid is to identify whether the database
     * has had the same set of ordered values already.
     * @return a guid.
     */
    public String generateDigest() {
        StringBuilder sb = new StringBuilder();
        sb.append(mappingKeysOid);
        sb.append(serviceOperation);
        sb.append(authUserId);
        sb.append(authUserProviderId);
        sb.append(mapping1_value);
        sb.append(mapping2_value);
        sb.append(mapping3_value);
        sb.append(mapping4_value);
        sb.append(mapping5_value);

        String toDigest = sb.toString();
        return digested = HexUtils.hexDump(HexUtils.getMd5Digest(HexUtils.encodeUtf8(toDigest)));
    }
}
