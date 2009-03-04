package com.l7tech.objectmodel;

import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Common way of identifying entities (externally) for which their EntityHeader's ID doesn't fully identify them
 * (e.g. Users, Policies, or non-entity properties that need to be treated as entities for Migration).
 *
 * @author jbufu
 */
@XmlRootElement
public class ExternalEntityHeader extends EntityHeader implements ValueMappable {

    // value-mapping extra properties
    private static final String VALUE_MAPPING_SELECTION = "valueMapping";
    private final static String VALUE_MAPPING_DATA_TYPE = "valueMappingDataType";
    private final static String DISPLAY_VALUE = "displayValue";
    private final static String MAPPED_VALUE = "mappedValue";

    private String externalId;

    protected Map<String,String> extraProperties = new HashMap<String, String>();

    public ExternalEntityHeader() {
    }

    public ExternalEntityHeader(String externalId, EntityType type, String id, String name, String description, Integer version) {
        super(id, type, name, description, version);
        this.externalId = externalId;
    }

    public ExternalEntityHeader(String externalId, EntityHeader header) {
        this(externalId, header.getType(), header.getStrId(), header.getName(), header.getDescription(), header.getVersion());
    }

    public ExternalEntityHeader(ExternalEntityHeader other) {
        this(other.getExternalId(), other.getType(), other.getStrId(), other.getName(), other.getDescription(), other.getVersion());
        extraProperties = new HashMap<String, String>(other.extraProperties);
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * @return  A key String that is best suited for database lookups.
     */
    public String getMappingKey() {
        return externalId;
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }

    public void setProperty(String name, String value) {
        extraProperties.put(name, value);
    }

    public String getProperty(String name) {
        return extraProperties.get(name);
    }

    public String getDisplayName() {
        return extraProperties != null && extraProperties.containsKey("Display Name") ? extraProperties.get("Display Name") : getName();
    }

    public String getDisplayNameWithScope() {
        return getDisplayName() +
              ((extraProperties != null && extraProperties.containsKey("Scope Name")) ? " [" + extraProperties.get("Scope Name") + "]" : "");
    }

    @Override
    public void setValueMapping(MigrationMappingSelection valueMappingType, ValueType dataType, Object sourceValue) {
        setProperty(VALUE_MAPPING_SELECTION, valueMappingType.name());
        if (valueMappingType != MigrationMappingSelection.NONE) {
            setProperty(VALUE_MAPPING_DATA_TYPE, dataType.name());
            setProperty(DISPLAY_VALUE, dataType.serialize(sourceValue));
        }
    }

    @XmlTransient
    @Override
    public MigrationMappingSelection getValueMapping() {
        if (getProperty(VALUE_MAPPING_SELECTION) == null)
            return MigrationMappingSelection.NONE;
        else
            return MigrationMappingSelection.valueOf(getProperty(VALUE_MAPPING_SELECTION));
    }

    public boolean isValueMappable() {
        return MigrationMappingSelection.NONE != getValueMapping(); 
    }

    @Override
    public void setValueType(ValueType type) {
        setProperty(VALUE_MAPPING_DATA_TYPE, type.name());
    }

    @XmlTransient
    @Override
    public ValueType getValueType() {
        return ValueType.valueOf(getProperty(VALUE_MAPPING_DATA_TYPE));
    }

    @Override
    public void setDisplayValue(String value) {
        setProperty(DISPLAY_VALUE, value);
    }

    @XmlTransient
    @Override
    public String getDisplayValue() {
        return getProperty(DISPLAY_VALUE);
    }

    public boolean hasDisplayValue() {
        return getProperty(DISPLAY_VALUE) != null;
    }

    @Override
    public void setMappedValue(String mappedValue) {
        setProperty(MAPPED_VALUE, mappedValue);
    }

    @XmlTransient
    @Override
    public String getMappedValue() {
        return getProperty(MAPPED_VALUE);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalEntityHeader that = (ExternalEntityHeader) o;

        if (type != that.type) return false;
        if (this.version != null ? !version.equals(that.version) : that.version != null) return false;
        if (externalId != null ? !externalId.equals(that.externalId) : that.externalId != null) return false;

        return true;
    }

    public int hashCode() {
        int result = (externalId != null ? externalId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public enum ValueType {

        TEXT("Plain Text", null, true),
        TEXT_ARRAY("Plain Text", TEXT, true),
        HTTP_URL("HTTP(S) URL", null, true),
        HTTP_URL_ARRAY("URL List", HTTP_URL, true),
        IP_ADDRESS("IP Address", null, true),
        IP_ADDRESS_ARRAY("IP Address List", IP_ADDRESS, true);

        private String name;
        private ValueType arrayBaseType;
        private boolean stringBaseType; // internal use: prevent addition of new types that can't be handled by the initial string (de)serializers

        public String serialize(Object value) {
            if (! stringBaseType) {
                throw new UnsupportedOperationException("Serialization of non string-based types not implemented");
            }
            return isArray() ? serializeStringArray(value) : serializeString(value);
        }

        public Object deserialize(String serialized) {
            if (! stringBaseType) {
                throw new UnsupportedOperationException("Deserialization of non string-based types not implemented");
            }
            return isArray() ? deserializeStringArray(serialized) : deserializeString(serialized);
        }

        private ValueType(String name, ValueType arrayBaseType, boolean stringBaseType) {
            this.name = name;
            this.arrayBaseType = arrayBaseType;
            this.stringBaseType = stringBaseType;
        }

        public String getName() {
            return name;
        }

        public ValueType getArrayBaseType() {
            return arrayBaseType;
        }

        public boolean isArray() {
            return arrayBaseType != null;
        }

        public boolean isStringBaseType() {
            return stringBaseType;
        }

        private static String serializeString(Object value) {
            if (! (value instanceof String) )
                throw new IllegalArgumentException("Invalid value type; expected String, got: " + (value == null ? null : value.getClass())) ;
            return (String) value;
        }

        private static Object deserializeString(String serialized) {
            return serialized;
        }

        private static String serializeStringArray(Object value) {
            if ( value == null ) return null;
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
}
