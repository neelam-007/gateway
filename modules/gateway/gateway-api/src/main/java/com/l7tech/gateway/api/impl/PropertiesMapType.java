package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.*;

/**
 * TODO [steve] type extensiblity and value typing
 */
@XmlType(name="PropertiesMapType", propOrder={"entry"})
public class PropertiesMapType {

    private List<PropertiesMapEntryType> entry = new ArrayList<PropertiesMapEntryType>();

    public PropertiesMapType() {}

    public PropertiesMapType( final Map<String,Object> map ) {
       for(Map.Entry<String,Object> entry : map.entrySet()) {
            this.entry.add(new PropertiesMapEntryType(entry));
        }
    }

    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<String,Object>();
        for (PropertiesMapEntryType jaxbEntry : entry) {
            map.put(jaxbEntry.getKey(), jaxbEntry.getValue());
        }
        return map;
    }

    @XmlElement(name="Property")
    public List<PropertiesMapEntryType> getEntry() {
        return entry;
    }

    public void setEntry( final List<PropertiesMapEntryType> entry ) {
        this.entry = entry;
    }

    @XmlType(name="PropertiesMapEntryType", propOrder={"value"})
    public static class PropertiesMapEntryType {
        private String key;
        private Object value;

        public PropertiesMapEntryType() {}

        public PropertiesMapEntryType( final Map.Entry<String,Object> entry ) {
            key = entry.getKey();
            value = entry.getValue();
        }

        @XmlAttribute
        public String getKey() {
            return key;
        }

        public void setKey( final String key ) {
            this.key = key;
        }

        @XmlElement(name="Value")
        public Object getValue() {
            return value;
        }

        public void setValue( final Object value ) {
            this.value = value;
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

}