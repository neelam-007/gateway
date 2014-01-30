package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: yuri
 * Date: 1/21/14
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Proxy(lazy=false)
@Table(name="jms_endpoint_message_rule")
@XmlRootElement(name = "JmsEndpointMessagePropertyRule")
public class JmsEndpointMessagePropertyRule  extends PersistentEntityImp implements Serializable {

    private String _rulename;
    private boolean _passThru;
    private String _customPattern;

    private JmsEndpoint jmsEndpoint;

    public JmsEndpointMessagePropertyRule() {
    }

    // Ensure version gets persisted
    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="jms_endpoint_goid", nullable=false)
    public JmsEndpoint getJmsEndpoint() {
        return jmsEndpoint;
    }

    public void setJmsEndpoint(JmsEndpoint jmsEndpoint) {
        checkLocked();
        this.jmsEndpoint = jmsEndpoint;
    }

    @Size(max=255)
    @Column(name="rule_name",length=255)
    public String getRuleName() {
        return _rulename;
    }

    public void setRuleName(String name) {
        _rulename = name;
    }

    @Column(name="is_passthrough")
    public boolean isPassThru() {
        return _passThru;
    }

    public void setPassThru(boolean passThru) {
        _passThru = passThru;
    }

    /**
     * @return the custom pattern; may contain context variable symbols;
     *         can be <code>null</code> if pass-thru
     */
    @Size(max=4096)
    @Column(name="custom_pattern", length = 4096)
    public String getCustomPattern() {
        return _customPattern;
    }

    /**
     * Note: Remember to call {@link #setPassThru} with false as appropriate.
     *
     * @param customPattern     the custom pattern; can contain context variable symbols
     */
    public void setCustomPattern(String customPattern) {
        _customPattern = customPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JmsEndpointMessagePropertyRule that = (JmsEndpointMessagePropertyRule) o;

        if (_passThru != that._passThru) return false;
        if (_customPattern != null ? !_customPattern.equals(that._customPattern) : that._customPattern != null)
            return false;
        if (_rulename != null ? !_rulename.equals(that._rulename) : that._rulename != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_rulename != null ? _rulename.hashCode() : 0);
        result = 31 * result + (_passThru ? 1 : 0);
        result = 31 * result + (_customPattern != null ? _customPattern.hashCode() : 0);
        return result;
    }

    public JmsEndpointMessagePropertyRule copy(JmsEndpoint jmsEndpoint) {
        JmsEndpointMessagePropertyRule copy = new JmsEndpointMessagePropertyRule();
        copy.setJmsEndpoint(jmsEndpoint);
        copy.setRuleName(getRuleName());
        copy.setPassThru(isPassThru());
        copy.setCustomPattern(getCustomPattern());
        return copy;
    }
}
