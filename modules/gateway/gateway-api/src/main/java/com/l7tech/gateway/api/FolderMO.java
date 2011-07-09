package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The FolderMO managed object represents an organizational folder.
 *
 * <p>Folders are displayed in the SecureSpan Manager and can be used to group
 * services and policies.</p>
 *
 * <p>The Accessor for folders is read only. Folders can be accessed by
 * identifier only.</p>
 *
 * @see ManagedObjectFactory#createFolder()
 */
@XmlRootElement(name="Folder")
@XmlType(name="FolderType", propOrder={"nameValue", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="folders")
public class FolderMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the folder (case insensitive, required)
     *
     * <p>Folder names are unique within their containing folder.</p>
     *
     * @return The name of the folder (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the folder.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the identifier for the parent folder.
     *
     * <p>A null parent folder means the folder is a root folder.</p>
     *
     * @return The containing folder identifier (may be null)
     */
    @XmlAttribute
    public String getFolderId() {
        return folderId;
    }

    /**
     * Set the folder identifier.
     *
     * @param folderId The containing folder identifier (null for the root folder)
     */
    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    /**
     * Get the properties for the folder.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the folder.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    //- PACKAGE

    FolderMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private String folderId;
    private Map<String,Object> properties;
}
