package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all managed objects.
 */
@XmlType(name="ManagedObjectType")
public abstract class ManagedObject {

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

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlTransient
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PRIVATE

    private String id;
    private Integer version;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
