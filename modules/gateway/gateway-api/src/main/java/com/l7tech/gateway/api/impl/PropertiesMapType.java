package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * 
 */
@XmlType(name="PropertiesMapType", propOrder={"entry","extensions"})
public class PropertiesMapType {

    //- PUBLIC

    public PropertiesMapType() {}

    public PropertiesMapType( final Map<String,Object> map ) {
       for(Map.Entry<String,Object> entry : map.entrySet()) {
            this.entry.add(new PropertiesMapEntryType(entry));
        }
    }

    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<String,Object>();
        for (PropertiesMapEntryType jaxbEntry : entry) {
            map.put(jaxbEntry.getKey(), getValue(jaxbEntry));
        }
        return map;
    }

    private Object getValue(PropertiesMapEntryType propertyMapEntryType) {
        //if it has attribute extensions convert it to an AttributeExtensible object
        if(propertyMapEntryType.getPropertyValue().getAttributeExtensions() != null && !propertyMapEntryType.getPropertyValue().getAttributeExtensions().isEmpty() && propertyMapEntryType.getPropertyValue() instanceof StringValue){
            AttributeExtensibleType.AttributeExtensibleString attributeExtensibleString = new AttributeExtensibleType.AttributeExtensibleString();
            attributeExtensibleString.setValue((String)propertyMapEntryType.getPropertyValue().getValue());
            attributeExtensibleString.setAttributeExtensions(propertyMapEntryType.getPropertyValue().getAttributeExtensions());
            return attributeExtensibleString;
        }
        return propertyMapEntryType.getValue();
    }

    @XmlElement(name="Property")
    public List<PropertiesMapEntryType> getEntry() {
        return entry;
    }

    public void setEntry( final List<PropertiesMapEntryType> entry ) {
        this.entry = entry;
    }

    @XmlType(name="PropertiesMapEntryType", propOrder={"propertyValue", "extensions"})
    public static class PropertiesMapEntryType {
        private String key;
        private PropertyValue<?> propertyValue;
        private List<Object> extensions;
        private Map<QName,Object> attributeExtensions;

        public PropertiesMapEntryType() {}

        public PropertiesMapEntryType( final Map.Entry<String,Object> entry ) {
            key = entry.getKey();
            propertyValue = newPropertyValue(entry.getValue());
        }

        @XmlAttribute
        public String getKey() {
            return key;
        }

        public void setKey( final String key ) {
            this.key = key;
        }

        public Object getValue() {
            return propertyValue == null ? null : propertyValue.getValue();
        }

        public void setValue( final Object value ) {
            this.propertyValue = newPropertyValue(value);
        }

        @XmlElementRefs({
            @XmlElementRef(type=ObjectValue.class),       
            @XmlElementRef(type=StringValue.class),
            @XmlElementRef(type=BooleanValue.class),
            @XmlElementRef(type=IntegerValue.class),
            @XmlElementRef(type=LongValue.class),
            @XmlElementRef(type=DateValue.class)
        })
        public PropertyValue getPropertyValue() {
            return propertyValue;
        }

        public void setPropertyValue( final PropertyValue propertyValue ) {
            this.propertyValue = propertyValue;
        }

        @XmlAnyElement(lax=true)
        protected List<Object> getExtensions() {
            return extensions;
        }

        protected void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }

        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }
    }


    @XmlTransient
    public static class PropertyValue<T> {
        private T value;
        private Map<QName,Object> attributeExtensions;

        public PropertyValue() {
        }

        public PropertyValue( T value ) {
            setValue( value );
        }

        @XmlElement(name="Item", nillable=true)
        public T getValue(){
            return value;
        }

        public void setValue( T value ) {
            this.value = value;
        }

        @XmlAnyAttribute
        protected Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }
    }

    @XmlRootElement(name="Value")
    @XmlType(name="ValueType", propOrder={"value","extensions"})
    public static class ObjectValue<T> extends PropertyValue<T> {
        private List<Object> extensions;

        public ObjectValue() {
        }

        public ObjectValue( final T value ) {
            super( value );
        }

        @XmlAnyElement(lax=true)
        protected List<Object> getExtensions() {
            return extensions;
        }

        protected void setExtensions( final List<Object> extensions ) {
            this.extensions = extensions;
        }
    }

    @XmlRootElement(name="StringValue")
    @XmlType(name="StringValueType")
    public static class StringValue extends PropertyValue<String>{
        public StringValue() {
        }

        public StringValue( final String value ) {
            super( value );
        }

        @XmlValue
        @Override
        public String getValue() {
            return super.getValue();
        }

        @Override
        public void setValue( final String value ) {
            super.setValue( value );
        }
    }

    @XmlRootElement(name="BooleanValue")
    @XmlType(name="BooleanValueType")
    public static class BooleanValue extends PropertyValue<Boolean>{
        public BooleanValue() {
        }

        public BooleanValue( final Boolean value ) {
            super( value );
        }

        @XmlValue
        @Override
        public Boolean getValue() {
            return super.getValue();
        }

        @Override
        public void setValue( final Boolean value ) {
            super.setValue( value );
        }
    }

    @XmlRootElement(name="IntegerValue")
    @XmlType(name="IntegerValueType")
    public static class IntegerValue extends PropertyValue<Integer>{
        public IntegerValue() {
        }

        public IntegerValue( final Integer value ) {
            super( value );
        }

        @XmlValue
        @Override
        public Integer getValue() {
            return super.getValue();
        }

        @Override
        public void setValue( final Integer value ) {
            super.setValue( value );
        }
    }

    @XmlRootElement(name="LongValue")
    @XmlType(name="LongValueType")
    public static class LongValue extends PropertyValue<Long>{
        public LongValue() {
        }

        public LongValue( final Long value ) {
            super( value );
        }

        @XmlValue
        @Override
        public Long getValue() {
            return super.getValue();
        }

        @Override
        public void setValue( final Long value ) {
            super.setValue( value );    
        }
    }

    @XmlRootElement(name="DateValue")
    @XmlType(name="DateValueType")
    public static class DateValue extends PropertyValue<Date>{
        public DateValue() {
        }

        public DateValue( final Date value ) {
            super( value );
        }

        @XmlValue
        @Override
        public Date getValue() {
            return super.getValue();
        }

        @Override
        public void setValue( final Date value ) {
            super.setValue( value );
        }
    }

    public static class PropertiesMapTypeAdapter extends XmlAdapter<PropertiesMapType, Map<String,Object>> {
        @Override
        public Map<String,Object> unmarshal( final PropertiesMapType mapType ) throws Exception {
            return mapType.toMap();
        }

        @Override
        public PropertiesMapType marshal( final Map<String,Object> map ) throws Exception {
            return map == null ? null : new PropertiesMapType(map);
        }
    }

    //- PROTECTED

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    //- PRIVATE

    private List<PropertiesMapEntryType> entry = new ArrayList<PropertiesMapEntryType>();
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;

    @SuppressWarnings({ "unchecked", "RedundantCast" })
    private static <T> PropertyValue<T> newPropertyValue( final T value ) {
        PropertyValue<T> propertyValue;

        if ( value instanceof Boolean ) {
            propertyValue = (PropertyValue<T>)new BooleanValue( (Boolean) value );
        } else if ( value instanceof String ) {
            propertyValue = (PropertyValue<T>)new StringValue( (String) value );
        } else if ( value instanceof Integer ) {
            propertyValue = (PropertyValue<T>)new IntegerValue( (Integer) value );
        } else if ( value instanceof Long ) {
            propertyValue = (PropertyValue<T>)new LongValue( (Long) value );
        } else if ( value instanceof Date ) {
            propertyValue = (PropertyValue<T>)new DateValue( (Date) value );
        } else if ( value instanceof AttributeExtensibleType.AttributeExtensibleString) {
            propertyValue = (PropertyValue<T>)new StringValue( ((AttributeExtensibleType.AttributeExtensibleString) value).getValue() );
            propertyValue.setAttributeExtensions(((AttributeExtensibleType.AttributeExtensibleString) value).getAttributeExtensions());
        } else {
            propertyValue = new ObjectValue<>( value );
        }

        return propertyValue;
    }
}