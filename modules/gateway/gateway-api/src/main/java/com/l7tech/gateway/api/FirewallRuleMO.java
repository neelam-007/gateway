package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * MO representation of firewall rules.
 *
 */
@XmlRootElement(name ="FirewallRule")
@XmlType(name="FirewallRuleType", propOrder={"nameValue","enabledValue","ordinalValue","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="firewallRules")
public class FirewallRuleMO extends AccessibleObject {

    //- PUBLIC

    /**
    /**
     * Get name of the firewall rule (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the firewall rule.
     *
     * @param name The name to use.
     */
    public void setName(final String name) {
        this.name = set(this.name, name);
    }

    /**
     * The firewall rule enabled flag
     *
     * @return True if the firewall rule is enabled.
     */
    public boolean isEnabled() {
        return get(enabled, false);
    }

    /**
     * Set the firewall rule enabled flag. (required)
     *
     * @param enabled True to enable the firewall rule
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * Get the ordinal of the firewall rule
     *
     * @return The ordinal of the firewall rule (may be null)
     */
    public int getOrdinal() {
        return get(ordinal);
    }

    /**
     * Set the ordinal of the firewall rule (required)
     *
     * @param ordinal  The ordinal of this firewall rule
     */
    public void setOrdinal(final int ordinal) {
        this.ordinal = set(this.ordinal,ordinal);
    }

    /**
     * Get the properties for this firewall rule
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this firewall rule.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, String> properties ) {
        this.properties = properties;
    }


    @XmlElement(name="Ordinal", required=true)
    protected AttributeExtensibleInteger getOrdinalValue() {
        return ordinal;
    }

    protected void setOrdinalValue( final AttributeExtensibleInteger ordinal ) {
        this.ordinal = ordinal;
    }

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString value ) {
        this.name = value;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean value ) {
        this.enabled = value;
    }


    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    //- PACKAGE

    FirewallRuleMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private Map<String,String> properties;
    private AttributeExtensibleInteger ordinal;

}
