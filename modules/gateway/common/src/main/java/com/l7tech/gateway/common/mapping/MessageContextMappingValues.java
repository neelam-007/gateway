package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
@Entity
@Table(name="message_context_mapping_values")
public class MessageContextMappingValues extends PersistentEntityImp {
    private long mappingKeysOid;
    private long createTime;
    private String mapping1_value;
    private String mapping2_value;
    private String mapping3_value;
    private String mapping4_value;
    private String mapping5_value;

    private MessageContextMappingKeys mappingKeysEntity;

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

    @Column(name="mapping1_value")
    @Lob
    public String getMapping1_value() {
        return mapping1_value;
    }

    public void setMapping1_value(String mapping1_value) {
        this.mapping1_value = mapping1_value;
    }

    @Column(name="mapping2_value")
    @Lob
    public String getMapping2_value() {
        return mapping2_value;
    }

    public void setMapping2_value(String mapping2_value) {
        this.mapping2_value = mapping2_value;
    }

    @Column(name="mapping3_value")
    @Lob
    public String getMapping3_value() {
        return mapping3_value;
    }

    public void setMapping3_value(String mapping3_value) {
        this.mapping3_value = mapping3_value;
    }

    @Column(name="mapping4_value")
    @Lob
    public String getMapping4_value() {
        return mapping4_value;
    }

    public void setMapping4_value(String mapping4_value) {
        this.mapping4_value = mapping4_value;
    }

    @Column(name="mapping5_value")
    @Lob
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
}
