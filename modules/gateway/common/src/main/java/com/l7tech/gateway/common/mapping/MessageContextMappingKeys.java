package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Version;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */

@Entity
@Table(name="message_context_mapping_keys")
public class MessageContextMappingKeys extends PersistentEntityImp {
    private static final Logger logger = Logger.getLogger(MessageContextMappingKeys.class.getName());
    private static final int MAX_MAPPING_TYPE_LENGTH = 36;
    private static final int MAX_KEY_LENGTH = 128;

    private String guid;
    private long createTime;
    private String mapping1_type;
    private String mapping2_type;
    private String mapping3_type;
    private String mapping4_type;
    private String mapping5_type;
    private String mapping1_key;
    private String mapping2_key;
    private String mapping3_key;
    private String mapping4_key;
    private String mapping5_key;

    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name="guid")
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Column(name="create_time")
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Column(name="mapping1_type", length= MAX_MAPPING_TYPE_LENGTH)
    public String getMapping1_type() {
        return mapping1_type;
    }

    public void setMapping1_type(String mapping1_type) {
        this.mapping1_type = mapping1_type;
    }

    @Column(name="mapping1_key", length= MAX_KEY_LENGTH)
    public String getMapping1_key() {
        return mapping1_key;
    }

    public void setMapping1_key(String mapping1_key) {
        this.mapping1_key = mapping1_key;
    }

    @Column(name="mapping2_type", length= MAX_MAPPING_TYPE_LENGTH)
    public String getMapping2_type() {
        return mapping2_type;
    }

    public void setMapping2_type(String mapping2_type) {
        this.mapping2_type = mapping2_type;
    }

    @Column(name="mapping2_key", length= MAX_KEY_LENGTH)
    public String getMapping2_key() {
        return mapping2_key;
    }

    public void setMapping2_key(String mapping2_key) {
        this.mapping2_key = mapping2_key;
    }

    @Column(name="mapping3_type", length= MAX_MAPPING_TYPE_LENGTH)
    public String getMapping3_type() {
        return mapping3_type;
    }

    public void setMapping3_type(String mapping3_type) {
        this.mapping3_type = mapping3_type;
    }

    @Column(name="mapping3_key", length= MAX_KEY_LENGTH)
    public String getMapping3_key() {
        return mapping3_key;
    }

    public void setMapping3_key(String mapping3_key) {
        this.mapping3_key = mapping3_key;
    }

    @Column(name="mapping4_type", length= MAX_MAPPING_TYPE_LENGTH)
    public String getMapping4_type() {
        return mapping4_type;
    }

    public void setMapping4_type(String mapping4_type) {
        this.mapping4_type = mapping4_type;
    }

    @Column(name="mapping4_key", length= MAX_KEY_LENGTH)
    public String getMapping4_key() {
        return mapping4_key;
    }

    public void setMapping4_key(String mapping4_key) {
        this.mapping4_key = mapping4_key;
    }

    @Column(name="mapping5_type", length= MAX_MAPPING_TYPE_LENGTH)
    public String getMapping5_type() {
        return mapping5_type;
    }

    public void setMapping5_type(String mapping5_type) {
        this.mapping5_type = mapping5_type;
    }

    @Column(name="mapping5_key", length= MAX_KEY_LENGTH)
    public String getMapping5_key() {
        return mapping5_key;
    }

    public void setMapping5_key(String mapping5_key) {
        this.mapping5_key = mapping5_key;
    }

    public List<MessageContextMapping> obtainMappingsWithEmptyValues() {
        String[] types = new String[] {
            mapping1_type,
            mapping2_type,
            mapping3_type,
            mapping4_type,
            mapping5_type
        };

        String[] keys = new String[] {
            mapping1_key,
            mapping2_key,
            mapping3_key,
            mapping4_key,
            mapping5_key
        };

        List<MessageContextMapping> mappings = new ArrayList<MessageContextMapping>(5);
        for (int i = 0; i < types.length; i++) {
            if (types[i] != null) {
                if (keys[i] != null) {
                    mappings.add(new MessageContextMapping(types[i], keys[i], null));
                } else {
                    logger.warning("The key of " + types[i] + " has not been set.");
                    break;
                }
            } else {
                break;
            }
        }
        return mappings;
    }

    public void setTypeAndKey(int idx, String type, String key) {
        switch (idx) {
            case 0:
                setMapping1_type(type);
                setMapping1_key(key);
                break;
            case 1:
                setMapping2_type(type);
                setMapping2_key(key);
                break;
            case 2:
                setMapping3_type(type);
                setMapping3_key(key);
                break;
            case 3:
                setMapping4_type(type);
                setMapping4_key(key);
                break;
            case 4:
                setMapping5_type(type);
                setMapping5_key(key);
                break;
            default :
                break;
        }
    }

    public String generateGuid() {
        List<MessageContextMapping> mappings = obtainMappingsWithEmptyValues();
        StringBuilder sb = new StringBuilder();

        for (MessageContextMapping mapping: mappings) {
            sb.append(mapping.getMappingType()).append("#").append(mapping.getKey()).append("#");
        }
        String uuidName = sb.toString();
        UUID guid = UUID.nameUUIDFromBytes(uuidName.getBytes());
        this.guid = guid.toString();
        return this.guid;
    }
}
