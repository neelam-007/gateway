package com.l7tech.gateway.common.siteminder;

import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/19/13
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="siteminder_configuration")
public class SiteMinderConfiguration extends ZoneableNamedGoidEntityImp implements Comparable {

    private String agent_name;
    private String address;
    private String secret;
    private Boolean ipcheck;
    private boolean enabled;
    private String hostname;
    private int fipsmode;
    private boolean noncluster_failover;
    private int cluster_threshold;
    private String hostConfiguration;
    private String userName;
    private Long passwordOid;
    private boolean updateSSOToken;
    private Map<String, String> properties;

    public SiteMinderConfiguration() {
        _name = "";
        agent_name = "";
        address = "";
        secret = "";
        ipcheck = false;
        hostname = "";
        fipsmode = 0;
        noncluster_failover = false;
        cluster_threshold = 50;
        enabled = true;
        hostConfiguration = "";
        userName = "";
        passwordOid = 0L;
        updateSSOToken = false;
    }

    @Size(min=1,max=128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Size(min=1,max=256)
    @Column(name="agent_name",nullable=false)
    public String getAgent_name() {
        return agent_name;
    }

    public void setAgent_name(String agent_name) {
        this.agent_name = agent_name;
    }

    @NotNull
    @Size(min=1,max=128)
    @Column(name="address",nullable=false)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @NotNull
    @Size(min=1,max=4096)
    @Column(name="secret",nullable=false)
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Column(name="ipcheck")
    public boolean isIpcheck() {
        return ipcheck;
    }

    public void setIpcheck(boolean ipcheck) {
        this.ipcheck = ipcheck;
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NotNull
    @Size(min=1,max=255)
    @Column(name="hostname",nullable=false)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @NotNull
    @Column(name="fipsmode",nullable=false)
    public int getFipsmode () {
        return fipsmode;
    }

    public void setFipsmode (int fipsmode ) {
        this.fipsmode = fipsmode;
    }

    @Column(name="noncluster_failover")
    public boolean isNoncluster_failover() {
        return noncluster_failover;
    }

    public void setNoncluster_failover(boolean noncluster_failover) {
        this.noncluster_failover = noncluster_failover;
    }

    /**
     * Get a list of all extra properties set on this SiteMinder configuration.
     *
     * @return a List of Strings.  May be empty, but never null.
     */
    @Transient
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    /**
     * Set an arbitrary cluster settings property.  Set a property to null to remove it.
     *
     * @param key  the name of the property to set
     * @param value the value to set it to, or null to remove the property
     */
    public void putProperty(String key, String value) {
        checkLocked();

        properties.put(key, value);
    }

    /**
     * Remove some property associated with the name,  ${propertyName} from the property list.
     * @param propertyName the property name whose property will be removed from the list.
     */
    public void removeProperty(String propertyName) {
        checkLocked();

        properties.remove(propertyName);
    }

    @Column(name="cluster_threshold")
    public int getCluster_threshold() {
        return cluster_threshold;
    }

    public void setCluster_threshold(int cluster_threshold) {
        this.cluster_threshold = cluster_threshold;
    }

    @Size(min=1,max=256)
    @Column(name="host_configuration")
    public String getHostConfiguration() {
        return hostConfiguration;
    }

    public void setHostConfiguration(String hostConfiguration) {
        this.hostConfiguration = hostConfiguration;
    }

    @Column(name="update_sso_token")
    public boolean isUpdateSSOToken() {
        return updateSSOToken;
    }

    public void setUpdateSSOToken(boolean updateSSOToken) {
        this.updateSSOToken = updateSSOToken;
    }

    @Size(min=1,max=256)
    @Column(name="user_name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Get the identifier of the related secure password.
     *
     * @return The identifier or null.
     */
    @Column(name="password_oid")
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.OID)
    public Long getPasswordOid() {
        return passwordOid;
    }

    public void setPasswordOid( final Long passwordOid ) {
        checkLocked();
        this.passwordOid = passwordOid;
    }

    /**
     * Get the cluster settings configuration for this SiteMinder configuration
     * @return a Set containing the cluster settings.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name="siteminder_configuration_property",
            joinColumns=@JoinColumn(name="goid", referencedColumnName="goid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    public Map<String, String> getProperties(){
        return properties;
    }

    /**
     * Set the cluster settings properties for this SiteMinder configuration.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    public void setProperties(Map<String,String> properties) {
        checkLocked();

        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.properties = properties;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || ! (o instanceof SiteMinderConfiguration)) throw new IllegalArgumentException("The compared object must be a SiteMinderConfiguration.");

        String originalConnectionName = getName();
        String comparedConnectionName = ((SiteMinderConfiguration)o).getName();

        if (originalConnectionName == null || comparedConnectionName == null) throw new NullPointerException("SiteMinderConfiguration name must not be null.");
        return originalConnectionName.compareToIgnoreCase(comparedConnectionName);
    }

    public void copyFrom(SiteMinderConfiguration other) {
        this.setGoid(other.getGoid());
        this.setName(other.getName());
        this.setAgent_name(other.getAgent_name());
        this.setAddress(other.getAddress());
        this.setSecret(other.getSecret());
        this.setIpcheck(other.isIpcheck());
        this.setEnabled(other.isEnabled());
        this.setHostname(other.getHostname());
        this.setFipsmode(other.getFipsmode());
        this.setNoncluster_failover(other.isNoncluster_failover());
        this.setCluster_threshold((other.getCluster_threshold()));
        this.setUpdateSSOToken(other.isUpdateSSOToken());
        this.setHostConfiguration(other.getHostConfiguration());
        this.setUserName(other.getUserName());
        this.setPasswordOid(other.getPasswordOid());
        this.setSecurityZone(other.getSecurityZone());
        this.setProperties(other.getProperties());
    }

    public boolean equalsConfiguration(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiteMinderConfiguration)) return false;

        SiteMinderConfiguration that = (SiteMinderConfiguration) o;

        if (cluster_threshold != that.cluster_threshold) return false;
        if (enabled != that.enabled) return false;
        if (fipsmode != that.fipsmode) return false;
        if (noncluster_failover != that.noncluster_failover) return false;
        if (updateSSOToken != that.updateSSOToken) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (agent_name != null ? !agent_name.equals(that.agent_name) : that.agent_name != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (ipcheck != null ? !ipcheck.equals(that.ipcheck) : that.ipcheck != null) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;

        return true;
    }
}
