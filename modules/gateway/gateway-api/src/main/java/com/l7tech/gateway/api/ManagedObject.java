package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ExtensionSupport;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Abstract base class for all managed objects.
 */
@XmlType(name="ManagedObjectType")
public abstract class ManagedObject extends ExtensionSupport {

    //- PUBLIC

    /**
     * Get the identifier for the managed object.
     *
     * <p>The identifier can have any format.</p>
     *
     * @return The identifier (may be null)
     */
    @XmlAttribute(name="id")
    public final String getId() {
        return id;
    }

    /**
     * Set the identifier for the managed object.
     *
     * <p>The identifier would not be set in normal use.</p>
     *
     * @param id The identifier to use.
     */
    public final void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the version for the managed object.
     *
     * <p>Some types of managed object may not be versioned.</p>
     *
     * @return The version (may be null)
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version for the managed object.
     *
     * <p>The version would not be set in normal use.</p>
     *
     * @param version The version.
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    //- PRIVATE

    private String id;
    private Integer version;
}
