package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;

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
public class MessageContextMappingValues extends GoidEntityImp {
    private static final int MAX_VALUE_LENGTH = 255;

    private Goid mappingKeysGoid;
    private long createTime;
    private String digested;

    // special case mappings
    private Goid authUserProviderId;
    private String authUserId; // this is not part of the identity
    private String authUserUniqueId;
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

    @Column(name="mapping_keys_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMappingKeysGoid() {
        return mappingKeysGoid;
    }

    public void setMappingKeysGoid(Goid mappingKeysGoid) {
        this.mappingKeysGoid = mappingKeysGoid;
    }

    @Column(name="create_time")
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Column(name="auth_user_provider_id")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getAuthUserProviderId() {
        return authUserProviderId;
    }

    public void setAuthUserProviderId(Goid authUserProviderId) {
        this.authUserProviderId = authUserProviderId;
    }

    @Column(name="auth_user_id", length=255)
    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    @Column(name="auth_user_unique_id", length=255)
    public String getAuthUserUniqueId() {
        return authUserUniqueId;
    }

    public void setAuthUserUniqueId(String authUserUniqueId) {
        this.authUserUniqueId = authUserUniqueId;
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
        this.mapping1_value = mapping1_value != null? mapping1_value.trim() : null;
    }

    @Column(name="mapping2_value", length= MAX_VALUE_LENGTH)
    public String getMapping2_value() {
        return mapping2_value;
    }

    public void setMapping2_value(String mapping2_value) {
        this.mapping2_value = mapping2_value != null? mapping2_value.trim() : null;
    }

    @Column(name="mapping3_value", length= MAX_VALUE_LENGTH)
    public String getMapping3_value() {
        return mapping3_value;
    }

    public void setMapping3_value(String mapping3_value) {
        this.mapping3_value = mapping3_value != null? mapping3_value.trim() : null;
    }

    @Column(name="mapping4_value", length= MAX_VALUE_LENGTH)
    public String getMapping4_value() {
        return mapping4_value;
    }

    public void setMapping4_value(String mapping4_value) {
        this.mapping4_value = mapping4_value != null? mapping4_value.trim() : null;
    }

    @Column(name="mapping5_value", length= MAX_VALUE_LENGTH)
    public String getMapping5_value() {
        return mapping5_value;
    }

    public void setMapping5_value(String mapping5_value) {
        this.mapping5_value = mapping5_value != null? mapping5_value.trim() : null;
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
    @JoinColumn(name="mapping_keys_goid", insertable=false, updatable=false)
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
        if (!Goid.equals(mappingKeysGoid, values.mappingKeysGoid))
            return false;
        if (serviceOperation != null ? !serviceOperation.equals(values.serviceOperation) : values.serviceOperation != null)
            return false;
        if (authUserUniqueId != null ? !authUserUniqueId.equals(values.authUserUniqueId) : values.authUserUniqueId != null) return false;
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
        sb.append(mappingKeysGoid);
        sb.append(serviceOperation);
        sb.append(authUserUniqueId);
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
