package com.l7tech.gateway.common.transport.firewall;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: kdiep
 * Date: 2/6/13
 * Time: 9:57 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Proxy(lazy=false)
@Table(name="firewall_rule")
public class SsgFirewallRule extends NamedEntityImp {
    protected static final Logger logger = Logger.getLogger(SsgFirewallRule.class.getName());

    private int ordinal;
    private boolean enabled = true;
    private Map<String,String> properties = new HashMap<String,String>();

    public SsgFirewallRule() {
    }

    public SsgFirewallRule(final boolean enabled, final int ordinal, final String name) {
        this.enabled = enabled;
        this.ordinal = ordinal;
        setName(name);
    }

    @Column(name="ordinal")
    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(final int ordinal) {
        checkLocked();
        this.ordinal = ordinal;
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        checkLocked();
        this.enabled = enabled;
    }

    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch= FetchType.EAGER)
    @JoinTable(name="firewall_rule_property", joinColumns=@JoinColumn(name="firewall_rule_oid", referencedColumnName="objectid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    protected Map<String,String> getProperties() {
        return properties;
    }

    protected void setProperties(final Map<String,String> properties) {
        checkLocked();
        this.properties = properties;
    }

    @Transient
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    public String getProperty(final String key){
        return properties.get(key);
    }

    public void putProperty(String key, String value) {
        checkLocked();
        properties.put(key, value);
    }

    public String removeProperty(String propertyName) {
        checkLocked();
        return properties.remove(propertyName);
    }

    @Transient
    public String getProtocol(){
        return properties.get("protocol");
    }

    @Transient
    public String getSource(){
        return properties.get("source");
    }

    @Transient
    public String getDestination(){
        return properties.get("destination");
    }

    @Transient
    public String getPort(){
        return properties.get("destination-port");
    }

    @Transient
    public String getJump(){
        return properties.get("jump");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final SsgFirewallRule that = (SsgFirewallRule) o;

        if (enabled != that.enabled) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}