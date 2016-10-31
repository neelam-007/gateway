package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The PasswordPolicyMO managed object represents Password Rules (Tasks -> Users and Authentication -> Manage Password Policy )
 *
 * <p>The Accessor for Password Rules supports read and write.
 * Password Rules can be accessed by identifier.</p>
 *
 * @see ManagedObjectFactory#createPasswordPolicy()
 */
@XmlRootElement(name="PasswordPolicy")
@XmlType(name="PasswordPolicyType", propOrder={"properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="passwordPolicy")
public class PasswordPolicyMO extends ElementExtendableAccessibleObject {

    //- PUBLIC
    private Map<String,Object> properties;

    PasswordPolicyMO() {
    }

    /**
     * Get the properties for this password policy.
     *
     * @return The properties
     */
    @XmlElement(name="properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this password policy.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }
}
