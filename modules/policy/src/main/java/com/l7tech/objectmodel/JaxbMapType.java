package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.XmlElement;
import java.util.*;

/**
 * JAXB Map serialization.
 *
 * @author jbufu
 */
public class JaxbMapType {

    private List<JaxbMapEntryType> entry = new ArrayList<JaxbMapEntryType>();

    public JaxbMapType() {}

    public JaxbMapType(Map map) {
        for(Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
            this.entry.add(new JaxbMapEntryType(entry));
        }
    }

    public Map toMap() {
        Map map = new HashMap();
        for (JaxbMapEntryType jaxbEntry : entry) {
            map.put(jaxbEntry.getKey(), jaxbEntry.getValue());
        }
        return map;
    }

    @XmlElement
    public List<JaxbMapEntryType> getEntry() {
        return entry;
    }

    public void setEntry(List<JaxbMapEntryType> entry) {
        this.entry = entry;
    }

    public static class JaxbMapEntryType<K,V> {

        private K key;
        private V value;

        public JaxbMapEntryType() {}

        public JaxbMapEntryType(Map.Entry<K,V> entry) {
            key = entry.getKey();
            value = entry.getValue();
        }

        @XmlElement
        public K getKey() {
            return key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        @XmlElement
        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    public static class JaxbMapTypeAdapter extends XmlAdapter<JaxbMapType, Map> {

        @Override
        public Map unmarshal(JaxbMapType mapType) throws Exception {
            Map result = null;
            if (mapType != null) {
                result = mapType.toMap();
            }
            return result;
        }

        @Override
        public JaxbMapType marshal(Map map) throws Exception {
            JaxbMapType result = null;
            if (map != null) {
                result = new JaxbMapType(map);
            }
            return result;
        }
    }

}
