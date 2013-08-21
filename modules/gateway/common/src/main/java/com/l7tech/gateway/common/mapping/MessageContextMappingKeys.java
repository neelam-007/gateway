package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Key information for a message context mapping.
 * 
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 12, 2008
 */
@Entity
@Proxy(lazy=false)
@Table(name="message_context_mapping_keys")
public class MessageContextMappingKeys extends PersistentEntityImp {
    private static final Logger logger = Logger.getLogger(MessageContextMappingKeys.class.getName());
    private static final int MAX_MAPPING_TYPE_LENGTH = 36;
    private static final int MAX_KEY_LENGTH = 128;

    private String digested;
    private long createTime;
    private MessageContextMapping.MappingType mapping1_type;
    private MessageContextMapping.MappingType mapping2_type;
    private MessageContextMapping.MappingType mapping3_type;
    private MessageContextMapping.MappingType mapping4_type;
    private MessageContextMapping.MappingType mapping5_type;
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

    @Column(name="digested")
    public String getDigested() {
        return digested;
    }

    public void setDigested(String digested) {
        this.digested = digested;
    }

    @Column(name="create_time")
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="mapping1_type", length= MAX_MAPPING_TYPE_LENGTH)
    public MessageContextMapping.MappingType getMapping1_type() {
        return mapping1_type;
    }

    public void setMapping1_type(MessageContextMapping.MappingType mapping1_type) {
        this.mapping1_type = mapping1_type;
    }

    @Column(name="mapping1_key", length= MAX_KEY_LENGTH)
    public String getMapping1_key() {
        return mapping1_key;
    }

    public void setMapping1_key(String mapping1_key) {
        this.mapping1_key = mapping1_key;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="mapping2_type", length= MAX_MAPPING_TYPE_LENGTH)
    public MessageContextMapping.MappingType getMapping2_type() {
        return mapping2_type;
    }

    public void setMapping2_type(MessageContextMapping.MappingType mapping2_type) {
        this.mapping2_type = mapping2_type;
    }

    @Column(name="mapping2_key", length= MAX_KEY_LENGTH)
    public String getMapping2_key() {
        return mapping2_key;
    }

    public void setMapping2_key(String mapping2_key) {
        this.mapping2_key = mapping2_key;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="mapping3_type", length= MAX_MAPPING_TYPE_LENGTH)
    public MessageContextMapping.MappingType getMapping3_type() {
        return mapping3_type;
    }

    public void setMapping3_type(MessageContextMapping.MappingType mapping3_type) {
        this.mapping3_type = mapping3_type;
    }

    @Column(name="mapping3_key", length= MAX_KEY_LENGTH)
    public String getMapping3_key() {
        return mapping3_key;
    }

    public void setMapping3_key(String mapping3_key) {
        this.mapping3_key = mapping3_key;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="mapping4_type", length= MAX_MAPPING_TYPE_LENGTH)
    public MessageContextMapping.MappingType getMapping4_type() {
        return mapping4_type;
    }

    public void setMapping4_type(MessageContextMapping.MappingType mapping4_type) {
        this.mapping4_type = mapping4_type;
    }

    @Column(name="mapping4_key", length= MAX_KEY_LENGTH)
    public String getMapping4_key() {
        return mapping4_key;
    }

    public void setMapping4_key(String mapping4_key) {
        this.mapping4_key = mapping4_key;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="mapping5_type", length= MAX_MAPPING_TYPE_LENGTH)
    public MessageContextMapping.MappingType getMapping5_type() {
        return mapping5_type;
    }

    public void setMapping5_type(MessageContextMapping.MappingType mapping5_type) {
        this.mapping5_type = mapping5_type;
    }

    @Column(name="mapping5_key", length= MAX_KEY_LENGTH)
    public String getMapping5_key() {
        return mapping5_key;
    }

    public void setMapping5_key(String mapping5_key) {
        this.mapping5_key = mapping5_key;
    }

    /**
     * A utility method creates five mappings using the given types, keys, and emtpy values.
     * @return a list of mappings with five elements.
     */
    public List<MessageContextMapping> obtainMappingsWithEmptyValues() {
        MessageContextMapping.MappingType[] types = new MessageContextMapping.MappingType[] {
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

    /**
     * A utility method sets the type and the key of a particaulr mapping.
     * @param idx: an index to identity which mapping will be set.  For example, 0 refers the first mapping,
     *             1 refers the second mappings, etc.
     * @param type: the type of a mapping to change.
     * @param key: the key of a mapping to change.
     */
    public void setTypeAndKey(int idx, MessageContextMapping.MappingType type, String key) {
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

    /**
     * Check if this mappings data is the same as the given mappings.
     *
     * <p>This test ignores object identity.</p>
     *
     * @param keys The keys to test
     * @return true if matches
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean matches( final MessageContextMappingKeys keys ) {
        if (mapping1_key != null ? !mapping1_key.equalsIgnoreCase(keys.mapping1_key) : keys.mapping1_key != null) return false;
        if (mapping1_type != null ? !mapping1_type.equals(keys.mapping1_type) : keys.mapping1_type != null)
            return false;
        if (mapping2_key != null ? !mapping2_key.equalsIgnoreCase(keys.mapping2_key) : keys.mapping2_key != null) return false;
        if (mapping2_type != null ? !mapping2_type.equals(keys.mapping2_type) : keys.mapping2_type != null)
            return false;
        if (mapping3_key != null ? !mapping3_key.equalsIgnoreCase(keys.mapping3_key) : keys.mapping3_key != null) return false;
        if (mapping3_type != null ? !mapping3_type.equals(keys.mapping3_type) : keys.mapping3_type != null)
            return false;
        if (mapping4_key != null ? !mapping4_key.equalsIgnoreCase(keys.mapping4_key) : keys.mapping4_key != null) return false;
        if (mapping4_type != null ? !mapping4_type.equals(keys.mapping4_type) : keys.mapping4_type != null)
            return false;
        if (mapping5_key != null ? !mapping5_key.equalsIgnoreCase(keys.mapping5_key) : keys.mapping5_key != null) return false;
        if (mapping5_type != null ? !mapping5_type.equals(keys.mapping5_type) : keys.mapping5_type != null)
            return false;

        return true;
    }

    /**
     * Create a guid for the set of ordered keys.  The guid is to identify whether the database
     * has had the same set of ordered keys already.
     * @return a guid.
     */
    public String generateDigest() {
        List<MessageContextMapping> mappings = obtainMappingsWithEmptyValues();
        StringBuilder sb = new StringBuilder();

        MessageContextMapping.MappingType type;
        String key;
        for (MessageContextMapping mapping: mappings) {
            type = mapping.getMappingType();
            key = mapping.getKey();
            if (key != null) key = key.toLowerCase();
            sb.append(type).append("#").append(key).append("#");
        }
        String toDigest = sb.toString();
        return digested = HexUtils.hexDump(HexUtils.getMd5Digest(HexUtils.encodeUtf8(toDigest)));
    }
}
