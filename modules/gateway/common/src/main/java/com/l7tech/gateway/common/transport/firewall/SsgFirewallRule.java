package com.l7tech.gateway.common.transport.firewall;

import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.util.BeanUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;
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
public class SsgFirewallRule extends NamedGoidEntityImp {
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
    @JoinTable(name="firewall_rule_property", joinColumns=@JoinColumn(name="firewall_rule_goid", referencedColumnName="goid"))
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
    public String getPort(){
        return properties.get("destination-port");
    }

    @Transient
    public String getJump(){
        return properties.get("jump");
    }

    @Transient
    public String getInInterface(){
        String i = properties.get("in-interface");
        return i == null ? "(ALL)" : i;
    }

    @Transient
    public SsgFirewallRule getCopy(){
        try {
            SsgFirewallRule copy = new SsgFirewallRule();
            BeanUtils.copyProperties(this, copy, BeanUtils.omitProperties(BeanUtils.getProperties(getClass()), "properties"));
            copy.setProperties(new HashMap<String, String>(getProperties()));
            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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