package com.l7tech.server.management.api;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * 
 */
@XmlRootElement(name="TypedValue", namespace="http://www.layer7tech.com/management")
public class TypedValue {

    //- PUBLIC

    public TypedValue() {
    }

    @SuppressWarnings({"unchecked"})
    public TypedValue( final Object value ) {
        if ( value instanceof Boolean ) {
            type = Type.BOOLEAN;
            booleanValue = (Boolean) value;
        } else if ( value instanceof Integer ) {
            type = Type.INTEGER;
            integerValue = (Integer) value;
        } else if ( value instanceof String ) {
            type = Type.STRING;
            stringValue = (String) value;
        } else if ( value instanceof Set ) {
            type = Type.SET_STRING;
            stringArrayValue = ((Set<String>) value).toArray(new String[((Set<String>) value).size()]);
        } else if ( value instanceof List ) {
            type = Type.LIST_STRING;
            stringArrayValue = ((List<String>) value).toArray(new String[((List<String>) value).size()]);
        } else if ( value instanceof Map) {
            type = Type.MAP_STRING_SET_STRING;
            Map<String,Set<String>> map = (Map<String,Set<String>>) value;
            Collection<NamedStringArray> values = new ArrayList<NamedStringArray>();
            for ( Map.Entry<String,Set<String>> entry : map.entrySet() ) {
                values.add( new NamedStringArray( entry.getKey(), entry.getValue() ) );
            }
            namedStringArrayValue = values.toArray( new NamedStringArray[values.size()] );
        } else {
            throw new IllegalArgumentException( "Type not supported" );
        }
    }

    @XmlAttribute
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @XmlElement
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @XmlElement
    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    @XmlElement
    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @XmlElement
    public String[] getStringArrayValue() {
        return stringArrayValue;
    }

    public void setStringArrayValue(String[] stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
    }

    @XmlElement
    public NamedStringArray[] getNamedStringArrayValue() {
        return namedStringArrayValue;
    }

    public void setNamedStringArrayValue(NamedStringArray[] namedStringArrayValue) {
        this.namedStringArrayValue = namedStringArrayValue;
    }

    public Object value() {
        Object value;

        if ( type == null ) throw new IllegalStateException("type is not defined.");

        switch ( type ) {
            case BOOLEAN:
                value = booleanValue;
                break;
            case INTEGER:
                value = integerValue;
                break;
            case STRING:
                value = stringValue;
                break;
            case SET_STRING:
                if ( stringArrayValue == null ) {
                    value = new LinkedHashSet<String>();
                } else {
                    value = new LinkedHashSet<String>( Arrays.asList( stringArrayValue ) );
                }
                break;
            case LIST_STRING:
                if ( stringArrayValue == null ) {
                    value = new ArrayList<String>();
                } else {
                    value = new ArrayList<String>( Arrays.asList( stringArrayValue ) );
                }
                break;
            case MAP_STRING_SET_STRING:
                Map<String,Set<String>> valueMap = new LinkedHashMap<String,Set<String>>();
                if ( namedStringArrayValue != null ) {
                    for ( NamedStringArray nsa : namedStringArrayValue ) {
                        String[] values = nsa.getValues();
                        if ( values == null ) {
                            valueMap.put( nsa.getName(), new LinkedHashSet<String>() );
                        } else {
                            valueMap.put( nsa.getName(), new LinkedHashSet<String>( Arrays.asList( values ) ) );
                        }
                    }
                }
                value = valueMap;
                break;
            default:
                throw new IllegalArgumentException("Unknown type : " + type);
        }

        return value;
    }

    public static enum Type { BOOLEAN, INTEGER, STRING, SET_STRING, LIST_STRING, MAP_STRING_SET_STRING }

    @XmlRootElement
    public static final class NamedStringArray {
        private String name;
        private String[] values;

        public NamedStringArray() {
        }

        public NamedStringArray( String name, Collection<String> values ) {
            this.name = name;
            this.values = values == null ? new String[0] : values.toArray( new String[values.size()] );
        }

        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement
        public String[] getValues() {
            return values;
        }

        public void setValues(String[] values) {
            this.values = values;
        }
    }

    //- PRIVATE

    private Type type;
    private Boolean booleanValue;
    private Integer integerValue;
    private String stringValue;
    private String[] stringArrayValue;
    private NamedStringArray[] namedStringArrayValue;
}
