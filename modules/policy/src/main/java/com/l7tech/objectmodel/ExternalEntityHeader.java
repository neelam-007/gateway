package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.HashMap;

/**
 * Common way of identifying entities (externally) for which their EntityHeader's ID doesn't fully identify them
 * (e.g. Users, Policies, or non-entity properties that need to be treated as entities for Migration).
 *
 * @author jbufu
 */
@XmlRootElement
public class ExternalEntityHeader extends EntityHeader {

    private String externalId;

    private Map<String,String> extraProperties = new HashMap<String, String>();

    public ExternalEntityHeader() {
    }

    public ExternalEntityHeader(String externalId, EntityType type, String name) {
        super(null, type, name, null);
        this.externalId = externalId;
    }

    public ExternalEntityHeader(String externalId, EntityType type, String id, String name, String description, Integer version) {
        super(id, type, name, description, version);
        this.externalId = externalId;
    }

    public ExternalEntityHeader(String externalId, EntityHeader header) {
        super(header.getStrId(), header.getType(), header.getName(), header.getDescription(), header.getVersion());
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }

    public void addProperty(String name, String value) {
        extraProperties.put(name, value);
    }

    public String getProperty(String name) {
        return extraProperties.get(name);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalEntityHeader that = (ExternalEntityHeader) o;

        return type == that.type && !(externalId != null ? !externalId.equals(that.externalId) : that.externalId != null);

    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
