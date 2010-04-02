package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.PolicyDetail;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.Map;

/**
 * Support methods and types for attribute extensible properties.
 */
public class AttributeExtensibleType {

    //- PUBLIC

    /**
     * Get the value of the given attribute.
     *
     * @param attribute The target attribute (may be null)
     * @return The value (may be null)
     */
    public static <T> T get( final AttributeExtensible<T> attribute ) {
        return get( attribute, null );
    }

    /**
     * Get the value of the given attribute with default fallback.
     *
     * @param attribute The target attribute (may be null)
     * @param defaultValue The value to use if the attribute value is null (may be null)
     * @return The attribute or default value (may be null if defaultValue is null)
     */
    public static <T> T get( final AttributeExtensible<T> attribute, T defaultValue ) {
        T value = null;

        if ( attribute != null ) {
            value = attribute.getValue();            
        }

        if ( value == null ) {
            value = defaultValue;            
        }

        return value;
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleString set( final AttributeExtensibleString currentAttribute,
                                                 final String value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleString() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleInteger set( final AttributeExtensibleInteger currentAttribute,
                                                  final Integer value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleInteger() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleLong set( final AttributeExtensibleLong currentAttribute,
                                               final Long value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleLong() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleBoolean set( final AttributeExtensibleBoolean currentAttribute,
                                                  final Boolean value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleBoolean() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleBigInteger set( final AttributeExtensibleBigInteger currentAttribute,
                                                     final BigInteger value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleBigInteger() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensibleByteArray set( final AttributeExtensibleByteArray currentAttribute,
                                                    final byte[] value ) {
        return doSet( currentAttribute==null ? new AttributeExtensibleByteArray() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute, create a new attribute if necessary.
     *
     * @param currentAttribute The target attribute (may be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or a new attribute if currentAttribute was null (null if value is null)
     */
    public static AttributeExtensiblePolicyType set( final AttributeExtensiblePolicyType currentAttribute,
                                                     final PolicyDetail.PolicyType value ) {
        return doSet( currentAttribute==null ? new AttributeExtensiblePolicyType() : currentAttribute, value );
    }

    /**
     * Set the value of the given attribute.
     *
     * @param currentAttribute The target attribue (must not be null)
     * @param value The value to set (may be null)
     * @return The currentAttribute or null if value was null.
     */
    public static <T, AE extends AttributeExtensible<T>> AE setNonNull( final AE currentAttribute,
                                                                        final T value ) {
        return doSet( currentAttribute, value );
    }

    /**
     * Base class for attribute extensible properties.
     */
    @XmlTransient
    public static abstract class AttributeExtensible<T> {
        private Map<QName,Object> attributeExtensions;

        /**
         * Get the value of the attribute.
         *
         * @return the value (may be null)
         */
        public abstract T getValue();

        /**
         * Set the value of the attribute.
         *
         * @param value The value to use (should not be null)
         */
        public abstract void setValue(T value);

        @XmlAnyAttribute
        public Map<QName, Object> getAttributeExtensions() {
            return attributeExtensions;
        }

        public void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
            this.attributeExtensions = attributeExtensions;
        }
    }

    /**
     * AttributeExtensible extension for String properties.
     */
    @XmlType(name="StringPropertyType")
    public static class AttributeExtensibleString extends AttributeExtensible<String> {
        private String value;

        @Override
        @XmlValue
        public String getValue() {
            return value;
        }

        @Override
        public void setValue( final String value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for Integer properties.
     */
    @XmlType(name="IntegerPropertyType")
    public static class AttributeExtensibleInteger extends AttributeExtensible<Integer> {
        private Integer value;

        @Override
        @XmlValue
        public Integer getValue() {
            return value;
        }

        @Override
        public void setValue( final Integer value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for Long properties.
     */
    @XmlType(name="LongPropertyType")
    public static class AttributeExtensibleLong extends AttributeExtensible<Long> {
        private Long value;

        @Override
        @XmlValue
        public Long getValue() {
            return value;
        }

        @Override
        public void setValue( final Long value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for Boolean properties.
     */
    @XmlType(name="BooleanPropertyType")
    public static class AttributeExtensibleBoolean extends AttributeExtensible<Boolean> {
        private Boolean value;

        public AttributeExtensibleBoolean() {
            this(null);
        }

        public AttributeExtensibleBoolean( final Boolean value ) {
            this.value = value;
        }

        @Override
        @XmlValue
        public Boolean getValue() {
            return value;
        }

        @Override
        public void setValue( final Boolean value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for byte[] properties.
     */
    @XmlType(name="BinaryPropertyType")
    public static class AttributeExtensibleByteArray extends AttributeExtensible<byte[]> {
        private byte[] value;

        @Override
        @XmlValue
        public byte[] getValue() {
            return value;
        }

        @Override
        public void setValue( final byte[] value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for BigInteger properties.
     */
    @XmlType(name="BigIntegerPropertyType")
    public static class AttributeExtensibleBigInteger extends AttributeExtensible<BigInteger> {
        private BigInteger value;

        @Override
        @XmlValue
        public BigInteger getValue() {
            return value;
        }

        @Override
        public void setValue( final BigInteger value ) {
            this.value = value;
        }
    }

    /**
     * AttributeExtensible extension for PolicyType properties.
     */
    @XmlType(name="PolicyTypePropertyType")
    public static class AttributeExtensiblePolicyType extends AttributeExtensible<PolicyDetail.PolicyType> {
        private PolicyDetail.PolicyType value;

        @Override
        @XmlValue
        public PolicyDetail.PolicyType getValue() {
            return value;
        }

        @Override
        public void setValue( final PolicyDetail.PolicyType value ) {
            this.value = value;
        }
    }

    //- PRIVATE

    private static <T, AE extends AttributeExtensible<T>> AE doSet( final AE attribute, final T value ) {
        AE result = null;

        if ( value != null ) {
            attribute.setValue( value );
            result = attribute;
        }
        
        return result;
    }       
}
