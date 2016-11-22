package com.l7tech.identity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

public class GroupBean implements Group, Serializable {
    public GroupBean(Goid providerId, String _name) {
        this.providerId = providerId;
        this.name = _name;
    }

    public GroupBean() {
    }

    public String getId() {
        return uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        uniqueId = uid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion( int version ) {
        this.version = version;
    }

    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.ID_PROVIDER_CONFIG)
    public Goid getProviderId() {
        return providerId;
    }

    public boolean isEquivalentId(Object thatId) {
        return uniqueId != null && uniqueId.equals(thatId);
    }

    public Map<String, String> getProperties() {
        if ( properties == null ) properties = new HashMap<String, String>();
        return properties;
    }

    public void setProperties( Map<String, String> properties ) {
        this.properties = properties;
    }


    /**
     * {@link Group} implementations that delegate their bean properties to {@link GroupBean} <b>must</b> override
     * {@link #equals} and {@link #hashCode} to include their own identity information!
     *
     * NOTE: if you regenerate this method, make sure the {@link #uniqueId} property is NOT included!
     * Particular {@link Group} implementations have their own logic for identity equality.
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupBean groupBean = (GroupBean) o;

        if (providerId != null ? !providerId.equals(groupBean.providerId) : groupBean.providerId != null) return false;
        if (description != null ? !description.equals(groupBean.description) : groupBean.description != null)
            return false;
        if (name != null ? !name.equals(groupBean.name) : groupBean.name != null) return false;
        if (properties != null ? !properties.equals(groupBean.properties) : groupBean.properties != null) return false;

        return true;
    }

    /**
     * {@link Group} implementations that delegate their bean properties to {@link GroupBean} <b>must</b> override
     * {@link #equals} and {@link #hashCode} to include their own identity information!
     *
     * NOTE: if you regenerate this method, make sure the {@link #uniqueId} property is NOT included!
     * Particular {@link Group} implementations have their own logic for identity equality.
     */
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    public void setProviderId( Goid providerId ) {
        this.providerId = providerId;
    }

    private static final long serialVersionUID = -2260828785148311161L;

    private String uniqueId;
    private String name;
    private String description;
    private Goid providerId;
    private Map<String, String> properties;
    private int version;
}
