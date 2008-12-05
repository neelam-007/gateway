package com.l7tech.server.management.api;

import com.l7tech.server.management.api.node.ReportApi;

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
            stringArrayValue =  new NamedStringArray((Set<String>) value);
        } else if ( value instanceof List ) {
            type = Type.LIST_STRING;
            stringArrayValue =  new NamedStringArray((List<String>) value);
        }else if( value instanceof LinkedHashMap){
            LinkedHashMap<String, List<ReportApi.FilterPair>>
                    keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>) value;
            type = Type.LINKED_HASH_MAP_TO_LIST_FILTER_PAIR;

            namedStringArrayValue = new NamedStringArray[keysToFilterPairs.size()];

            int index = 0;
            for(Map.Entry<String, List<ReportApi.FilterPair>> me: keysToFilterPairs.entrySet()){
                List<String> filterPairDisplayStrings = new ArrayList<String>();
                for(ReportApi.FilterPair fp: me.getValue()){
                    filterPairDisplayStrings.add(fp.getDisplayValue());
                }
                namedStringArrayValue[index++] = new NamedStringArray(me.getKey(), filterPairDisplayStrings);
            }
            
        }
        else if ( value instanceof Map) {
            Map testMap = (Map) value;
            Object testVal = testMap.values().iterator().next();
            if(testVal instanceof Collection){
                type = Type.MAP_STRING_SET_STRING;
                Map<String,Set<String>> map = (Map<String,Set<String>>) value;
                Collection<NamedStringArray> values = new ArrayList<NamedStringArray>();
                for ( Map.Entry<String,Set<String>> entry : map.entrySet() ) {
                    values.add( new NamedStringArray( entry.getKey(), entry.getValue() ) );
                }
                namedStringArrayValue = values.toArray( new NamedStringArray[values.size()] );
            }else if(testVal instanceof String){
                type = Type.MAP_STRING_STRING;
                Map<String, String> map = (Map<String, String>) value;
                Collection<NamedStringArray> values = new ArrayList<NamedStringArray>();
                for ( Map.Entry<String,String> entry : map.entrySet() ) {
                    List<String> oneString = new ArrayList<String>();
                    oneString.add(entry.getValue());
                    values.add( new NamedStringArray( entry.getKey(), oneString) );
                }
                namedStringArrayValue = values.toArray( new NamedStringArray[values.size()] );
            }else{
                throw new IllegalArgumentException( "Type not supported" );
            }
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
    public NamedStringArray getStringArrayValue() {
        return stringArrayValue;
    }

    public void setStringArrayValue(NamedStringArray stringArrayValue) {
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
                    value = new LinkedHashSet<String>( Arrays.asList( stringArrayValue.asStringArray() ) );
                }
                break;
            case LIST_STRING:
                if ( stringArrayValue == null ) {
                    value = new ArrayList<String>();
                } else {
                    value = new ArrayList<String>( Arrays.asList( stringArrayValue.asStringArray() ) );
                }
                break;
            case MAP_STRING_SET_STRING:
                Map<String,Set<String>> valueMap = new LinkedHashMap<String,Set<String>>();
                if ( namedStringArrayValue != null ) {
                    for ( NamedStringArray nsa : namedStringArrayValue ) {
                        String[] values = nsa.asStringArray();
                        if ( values == null ) {
                            valueMap.put( nsa.getName(), new LinkedHashSet<String>() );
                        } else {
                            valueMap.put( nsa.getName(), new LinkedHashSet<String>( Arrays.asList( values ) ) );
                        }
                    }
                }
                value = valueMap;
                break;
            case LINKED_HASH_MAP_TO_LIST_FILTER_PAIR:
                LinkedHashMap<String, List<ReportApi.FilterPair>>
                        keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();

                if ( namedStringArrayValue != null ) {
                    for ( NamedStringArray nsa : namedStringArrayValue ) {
                        String key = nsa.getName();
                        String[] values = nsa.asStringArray();
                        //values should never be null as even an empty FilterPair() will have "" internally
                        List<ReportApi.FilterPair> fpL = new ArrayList<ReportApi.FilterPair>();
                        for(String s: values){
                            if(s.equals("")){
                                fpL.add(new ReportApi.FilterPair());
                            }else{
                                fpL.add(new ReportApi.FilterPair(s));
                            }
                        }
                        keysToFilterPairs.put(key, fpL);
                    }
                }
                value = keysToFilterPairs;
                break;
            case MAP_STRING_STRING:
                Map<String,String> valueMapString = new LinkedHashMap<String,String>();
                if ( namedStringArrayValue != null ) {
                    for ( NamedStringArray nsa : namedStringArrayValue ) {
                        String[] values = nsa.asStringArray();
                        if ( values == null ) {
                            valueMapString.put( nsa.getName(), null);
                        } else {
                            valueMapString.put( nsa.getName(), values[0] );
                        }
                    }
                }
                value = valueMapString;
                break;
            default:
                throw new IllegalArgumentException("Unknown type : " + type);
        }

        return value;
    }

    @Override
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypedValue that = (TypedValue) o;

        if (booleanValue != null ? !booleanValue.equals(that.booleanValue) : that.booleanValue != null) return false;
        if (integerValue != null ? !integerValue.equals(that.integerValue) : that.integerValue != null) return false;
        if (!Arrays.equals(namedStringArrayValue, that.namedStringArrayValue)) return false;
        if (stringArrayValue != null ? !stringArrayValue.equals(that.stringArrayValue) : that.stringArrayValue != null)
            return false;
        if (stringValue != null ? !stringValue.equals(that.stringValue) : that.stringValue != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (booleanValue != null ? booleanValue.hashCode() : 0);
        result = 31 * result + (integerValue != null ? integerValue.hashCode() : 0);
        result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
        result = 31 * result + (stringArrayValue != null ? stringArrayValue.hashCode() : 0);
        result = 31 * result + (namedStringArrayValue != null ? Arrays.hashCode(namedStringArrayValue) : 0);
        return result;
    }

    public static enum Type { BOOLEAN, INTEGER, STRING, SET_STRING, LIST_STRING, MAP_STRING_SET_STRING,
        MAP_STRING_STRING, LINKED_HASH_MAP_TO_LIST_FILTER_PAIR }

    @XmlRootElement
    public static final class NamedStringArray {
        private String name;
        private NullableString[] values = new NullableString[0];

        public NamedStringArray() {
        }

        public NamedStringArray( Collection<String> values ) {
            this( null, values );            
        }

        public NamedStringArray( String name, Collection<String> values ) {
            this.name = name;
            this.values = values == null ? null : toArray( values );
        }

        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement
        public NullableString[] getValues() {
            return values;
        }

        public void setValues(NullableString[] values) {
            this.values = values;
        }

        public String[] asStringArray() {
            String[] stringArray;

            if ( values != null ) {
                stringArray = new String[ values.length ];
                for ( int i=0; i<values.length; i++ ) {
                    stringArray[i] = values[i].getValue();
                }
            } else {
                stringArray = new String[0];
            }

            return stringArray;            
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NamedStringArray that = (NamedStringArray) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (!Arrays.equals(values, that.values)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 31 * result + (values != null ? Arrays.hashCode(values) : 0);
            return result;
        }

        private NullableString[] toArray( Collection<String> values ) {
            String[] stringArray = values.toArray( new String[values.size()] );
            NullableString[] nullableStringArray = new NullableString[stringArray.length];
            for ( int i=0; i<stringArray.length; i++ ) {
                nullableStringArray[i] = new NullableString(stringArray[i]);
            }
            return nullableStringArray;
        }
    }

    @XmlRootElement
    public static final class NullableString {
        private String value;

        public NullableString() {
        }

        public NullableString( final String value ) {
            this.value = value;
        }

        @XmlAttribute
        public String getValue() {
            return value;
        }

        public void setValue( final String value ) {
            this.value = value;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NullableString that = (NullableString) o;

            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (value != null ? value.hashCode() : 0);
        }
    }

    //- PRIVATE

    private Type type;
    private Boolean booleanValue;
    private Integer integerValue;
    private String stringValue;
    private NamedStringArray stringArrayValue;
    private NamedStringArray[] namedStringArrayValue;

}
