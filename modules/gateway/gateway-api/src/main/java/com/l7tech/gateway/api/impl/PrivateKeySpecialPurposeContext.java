package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleString;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleStringBuilder;
import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleStringList;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.unwrap;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.wrap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Private key special purpose context.
 *
 * <p>The private key special purpose context encapsulates a request to assign
 * special purposes to a private key. Assigning special purposes will also
 * remove any existing assignments for those purposes from other keys.</p>
 */
@XmlRootElement(name="PrivateKeySpecialPurposeContext")
@XmlType(name="PrivateKeySpecialPurposeContextType", propOrder={"specialPurposeValues","properties", "extension", "extensions"})
public class PrivateKeySpecialPurposeContext extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * Get the special purposes for this request.
     *
     * @return The list (never null)
     */
    public List<String> getSpecialPurposes() {
        return unwrap(get( specialPurposes, new ArrayList<AttributeExtensibleString>() ));
    }

    /**
     * Set the special purposes for this request.
     *
     * @param specialPurposes The special purposes to use.
     */
    public void setSpecialPurposes( final List<String> specialPurposes ) {
        this.specialPurposes = set( this.specialPurposes, wrap(specialPurposes,AttributeExtensibleStringBuilder) );
    }

    /**
     * Get the properties for the request.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the request.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="SpecialPurposes", required=true)
    protected AttributeExtensibleStringList getSpecialPurposeValues() {
        return specialPurposes;
    }

    protected void setSpecialPurposeValues( final AttributeExtensibleStringList specialPurposes ) {
        this.specialPurposes = specialPurposes;
    }

    //- PRIVATE

    private AttributeExtensibleStringList specialPurposes;
    private Map<String,Object> properties;
}
