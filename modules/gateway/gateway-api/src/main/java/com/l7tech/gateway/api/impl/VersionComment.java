package com.l7tech.gateway.api.impl;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * 
 */
@XmlRootElement(name="VersionComment")
@XmlType(name="VersionCommentType", propOrder={"commentValue", "versionNumberValue", "extension", "extensions"})
public class VersionComment extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * The version comment
     *
     * @return The comment (required)
     */
    public String getComment() {
        return get(comment);
    }

    /**
     * Sets the version comment
     *
     */
    public void setComment( final String comment ) {
        set(this.comment,comment);
    }

    /**
     * The version number
     *
     * @return The version number (may be null)
     */
    public Integer getVersionNumber() {
        return get(versionNumber, null );
    }

    /**
     * Sets the version number, null for the active version
     *
     */
    public void setVersionNumber( final Integer versionNumber ) {
        set(this.versionNumber,versionNumber);
    }

    //- PROTECTED

    @XmlElement(name="Comment", required=true)
    protected AttributeExtensibleType.AttributeExtensibleString getCommentValue() {
        return comment;
    }

    protected void setCommentValue( final AttributeExtensibleType.AttributeExtensibleString comment ) {
        this.comment = comment;
    }

    @XmlElement(name="VersionNumber")
    protected AttributeExtensibleType.AttributeExtensibleInteger getVersionNumberValue() {
        return versionNumber;
    }

    protected void setVersionNumberValue( final AttributeExtensibleType.AttributeExtensibleInteger versionNumber ) {
        this.versionNumber = versionNumber;
    }

    //- PRIVATE

    private AttributeExtensibleType.AttributeExtensibleString comment;
    private AttributeExtensibleType.AttributeExtensibleInteger versionNumber;

}
