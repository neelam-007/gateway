package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * The CustomKeyValueStoreMO object represents a custom key value.
 */
@XmlRootElement(name = "CustomKeyValueStore")
@XmlType(name = "CustomKeyValueStoreType", propOrder = {"keyValue", "valueValue", "extensions"})
@AccessorSupport.AccessibleResource(name = "customKeyValueStores")
public class CustomKeyValueStoreMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Gets the key.
     *
     * @return The key
     */
    public String getKey() {
        return get(key);
    }

    /**
     * Sets the key.
     *
     * @param key The key to use
     */
    public void setKey( final String key ) {
        this.key = set(this.key, key);
    }

    /**
     * Gets the value.
     *
     * @return The value
     */
    public byte[] getValue() {
        return get(value);
    }

    /**
     * Sets the value.
     *
     * @param value The value to use.
     */
    public void setValue(byte[] value) {
        this.value = set(this.value, value);
    }

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlElement(name = "Key", required=true)
    protected AttributeExtensibleString getKeyValue() {
        return key;
    }

    protected void setKeyValue(final AttributeExtensibleString key) {
        this.key = key;
    }

    @XmlElement(name = "Value", required = true)
    protected AttributeExtensibleByteArray getValueValue() {
        return value;
    }

    protected void setValueValue(final AttributeExtensibleByteArray value) {
        this.value = value;
    }

    //- PACKAGE

    CustomKeyValueStoreMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString key;
    private AttributeExtensibleByteArray value;
}