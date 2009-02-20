package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author jbufu
 */
@XmlRootElement
public class ValueReferenceEntityHeader extends ExternalEntityHeader {

    public enum Type {
        HTTP_URL {
            @Override
            public String serialize(Object value) {
                return serializeString(value);
            }
            @Override
            public Object deserialize(String serialized) {
                return deserializeString(serialized);
            }
        },

        HTTP_URL_ARRAY {
            @Override
            public String serialize(Object value) {
                return serializeStringArray(value);
            }
            @Override
            public Object deserialize(String serialized) {
                return deserializeStringArray(serialized);
            }
        },

        IP_ADDRESS {
            @Override
            public String serialize(Object value) {
                return serializeString(value);
            }
            @Override
            public Object deserialize(String serialized) {
                return deserializeString(serialized);
            }
        },

        IP_ADDRESS_ARRAY {
            @Override
            public String serialize(Object value) {
                return serializeStringArray(value);
            }
            @Override
            public Object deserialize(String serialized) {
                return deserializeStringArray(serialized);
            }
        },

        TEXT {
            @Override
            public String serialize(Object value) {
                return serializeString(value);
            }
            @Override
            public Object deserialize(String serialized) {
                return deserializeString(serialized);
            }
        };

        public abstract String serialize(Object value);
        public abstract Object deserialize(String serialized);

        private static String serializeString(Object value) {
            if (! (value instanceof String) )
                throw new IllegalArgumentException("Invalid value type; expected String, got: " + (value == null ? null : value.getClass())) ;
            return (String) value;
        }

        private static Object deserializeString(String serialized) {
            return serialized;
        }
        
        private static String serializeStringArray(Object value) {
            if (value == null) return null;
            if ( value == null || ! String.class.isAssignableFrom(value.getClass().getComponentType()) )
                throw new IllegalArgumentException("Invalid value type; expected String[], got: " + (value == null ? null : value.getClass())) ;
            StringBuilder serialized = new StringBuilder(Arrays.toString((String[])value));
            if (serialized.charAt(0) == '[') serialized.deleteCharAt(0);
            if (serialized.charAt(serialized.length()-1) == ']') serialized.deleteCharAt(serialized.length()-1);
            return serialized.toString();
        }

        private static Object deserializeStringArray(String serialized) {
            if (serialized == null) return null;
            if (serialized.length()==0) return new String[0];
            String[] tokens = serialized.split(",");
            for(int i=0; i<tokens.length; i++)
                tokens[i] = tokens[i].trim();
            return tokens;
        }
    }

    private final static String OWNER_TYPE = "ownerType";
    private final static String VALUE_TYPE = "valueType";
    private final static String DISPLAY_VALUE = "displayValue";
    private final static String MAPPED_VALUE = "mappedValue";

    public ValueReferenceEntityHeader() {
    }

    public ValueReferenceEntityHeader(ExternalEntityHeader owner, String propertyName, Type valueType, String displayValue) {
        super(HexUtils.encodeBase64(propertyName.getBytes(Charset.forName("UTF-8"))) + ":" + owner.getExternalId(), owner);
        setName(getName() + " : " + propertyName);
        setProperty(OWNER_TYPE, owner.getType().name());
        setProperty(VALUE_TYPE, valueType.name());
        setProperty(DISPLAY_VALUE, displayValue);
        this.type = EntityType.VALUE_REFERENCE;
    }

    public ValueReferenceEntityHeader(ExternalEntityHeader other) {
        super(other.getExternalId(), other);
        setExtraProperties(new HashMap<String, String>(other.getExtraProperties()));
    }

    @XmlTransient
    public EntityType getOwnerType() {
        return EntityType.valueOf(getProperty(OWNER_TYPE));
    }

    @XmlTransient
    public String getOwnerId() {
        return getExternalId().substring(getExternalId().indexOf(":")+1);
    }

    @XmlTransient
    public String getPropertyName() {
        return HexUtils.decodeUtf8(HexUtils.decodeBase64(getExternalId().substring(0, getExternalId().indexOf(":"))));
    }

    @XmlTransient
    public ExternalEntityHeader getOwnerHeader() {
        return new ExternalEntityHeader(getOwnerId(), getOwnerType(), getStrId(), getName(), getDescription(), getVersion());
    }

    public void setValueType(Type type) {
        setProperty(VALUE_TYPE, type.name());
    }

    @XmlTransient
    public Type getValueType() {
        return Type.valueOf(getProperty(VALUE_TYPE));
    }

    public void setDisplayValue(String value) {
        setProperty(DISPLAY_VALUE, value);
    }

    @XmlTransient
    public String getDisplayValue() {
        return getProperty(DISPLAY_VALUE);
    }

    public void setMappedValue(String mappedValue) {
        setProperty(MAPPED_VALUE, mappedValue);
    }

    @XmlTransient
    public String getMappedValue() {
        return getProperty(MAPPED_VALUE);
    }

}
