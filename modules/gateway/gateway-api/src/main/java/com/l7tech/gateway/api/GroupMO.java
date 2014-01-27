package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Group")
@XmlType(name = "GroupType", propOrder = {"name", "description", "extensions"})
@AccessorSupport.AccessibleResource(name = "groups")
public class GroupMO extends AccessibleObject {

    //- PUBLIC

    /**
     * The name for the group (case insensitive, required)
     *
     * @return The name (may be null)
     */
    @XmlElement(name = "Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the group.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * The description for the group. Case sensitive.
     *
     * @return The description (may be null).
     */
    @XmlElement(name = "Description", required=false)
    public String getDescription() {
        return description;
    }

    /**
     * Set the description for the group.
     *
     * @param description The description to use.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The the identity provider id for the group.
     *
     * @return The identity provider id.
     */
    @XmlAttribute
    public String getProviderId() {
        return providerId;
    }

    /**
     * Set the identity provider id for the group (required).
     *
     * @param providerId The identity provider id.
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    //- PACKAGE

    GroupMO() {
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    //- PRIVATE

    private String name;
    private String description;
    private String providerId;

}
