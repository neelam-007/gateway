package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import com.l7tech.util.CollectionUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

/**
 * The StoredPasswordMO managed object represents a stored password.
 *
 * <p>The Accessor for stored passwords supports read and write. Stored
 * passwords can be accessed by identifier or by name.</p>
 *
 * <p>The following properties can be used:
 * <ul>
 *   <li><code>description</code>: An optional description for the password
 *   (string)</li>
 *   <li><code>lastUpdated</code> (read only): The date of the last update
 *   (date)</li>
 *   <li><code>usageFromVariable</code>: True if the password can be referenced
 *   as a context variable (boolean, defaults to false)</li>
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createStoredPassword()
 */
@XmlRootElement(name="StoredPassword")
@XmlType(name="StoredPasswordType", propOrder={"nameValue","passwordValue","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="storedPasswords")
public class StoredPasswordMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    private AttributeExtensibleType.AttributeExtensibleString name;
    private AttributeExtensibleType.AttributeExtensibleString password;
    private Map<String,Object> properties;

    StoredPasswordMO() {
    }

    /**
     * The name for the stored password (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the stored password.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the password for the stored password.
     *
     * @return The password (may be null)
     */
    public String getPassword() {
        return get( password );
    }

    /**
     * Set the password for the stored password.
     *
     * @param password The value to use.
     */
    public void setPassword( final String password ) {
        this.password = set(this.password, password );
    }

    //- PROTECTED

    public void setPassword(final String password, final String bundleKey){
        this.password = new AttributeExtensibleType.AttributeExtensibleString();
        this.password.setValue(password);
        this.password.setAttributeExtensions(CollectionUtils.<QName,Object>mapBuilder().put(new QName("bundleKey"),bundleKey).map());
    }

    public String getPasswordBundleKey(){
        if(this.password.getAttributeExtensions() != null){
            return (String)this.password.getAttributeExtensions().get(new QName("bundleKey"));
        }
        return null;
    }

    /**
     * Get the properties for this stored password.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this stored password.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PACKAGE

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleType.AttributeExtensibleString getNameValue() {
        return name;
    }

    //- PRIVATE

    protected void setNameValue( final AttributeExtensibleType.AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Password")
    protected AttributeExtensibleType.AttributeExtensibleString getPasswordValue() {
        return password;
    }

    protected void setPasswordValue( final AttributeExtensibleType.AttributeExtensibleString value ) {
        this.password = value;
    }
}
