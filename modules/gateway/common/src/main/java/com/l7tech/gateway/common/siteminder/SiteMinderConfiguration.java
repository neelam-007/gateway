package com.l7tech.gateway.common.siteminder;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
public class SiteMinderConfiguration extends ZoneableNamedEntityImp implements Comparable {

    private String agentName;
    private String address;
    private String secret;
    private Boolean ipcheck;
    private boolean enabled;
    private String hostname;
    private int fipsmode;
    private boolean nonClusterFailover;
    private int clusterThreshold;
    private String hostConfiguration;
    private String userName;
    private Goid passwordGoid;
    private boolean updateSSOToken;
    private Map<String, String> properties;

    public SiteMinderConfiguration() {
        _name = "";
        agentName = "";
        address = "";
        secret = "";
        ipcheck = false;
        hostname = "";
        fipsmode = 0;
        nonClusterFailover = false;
        clusterThreshold = 50;
        enabled = true;
        hostConfiguration = "";
        userName = "";
        passwordGoid = null;
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
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
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
    @Min(0)
    @Max(4)
    @Column(name="fipsmode",nullable=false)
    public int getFipsmode () {
        return fipsmode;
    }

    public void setFipsmode (int fipsmode ) {
        this.fipsmode = fipsmode;
    }

    @Column(name="noncluster_failover")
    public boolean isNonClusterFailover() {
        return nonClusterFailover;
    }

    public void setNonClusterFailover(boolean nonClusterFailover) {
        this.nonClusterFailover = nonClusterFailover;
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
    public int getClusterThreshold() {
        return clusterThreshold;
    }

    public void setClusterThreshold(int clusterThreshold) {
        this.clusterThreshold = clusterThreshold;
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
    @Column(name="password_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(final Goid passwordGoid) {
        checkLocked();
        this.passwordGoid = passwordGoid;
    }

    /**
     * Get the cluster settings configuration for this SiteMinder configuration
     * @return a Set containing the cluster settings.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name="siteminder_configuration_property",
            joinColumns=@JoinColumn(name="siteminder_configuration_goid", referencedColumnName="goid"))
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

        String originalConfigurationName = getName();
        String comparedConfigurationName = ((SiteMinderConfiguration)o).getName();

        if (originalConfigurationName == null || comparedConfigurationName == null) throw new NullPointerException("SiteMinderConfiguration name must not be null.");
        return originalConfigurationName.compareToIgnoreCase(comparedConfigurationName);
    }

    public void copyFrom(SiteMinderConfiguration other) {
        this.setGoid(other.getGoid());
        this.setName(other.getName());
        this.setAgentName(other.getAgentName());
        this.setAddress(other.getAddress());
        this.setSecret(other.getSecret());
        this.setIpcheck(other.isIpcheck());
        this.setEnabled(other.isEnabled());
        this.setHostname(other.getHostname());
        this.setFipsmode(other.getFipsmode());
        this.setNonClusterFailover(other.isNonClusterFailover());
        this.setClusterThreshold((other.getClusterThreshold()));
        this.setUpdateSSOToken(other.isUpdateSSOToken());
        this.setHostConfiguration(other.getHostConfiguration());
        this.setUserName(other.getUserName());
        this.setPasswordGoid(other.getPasswordGoid());
        this.setSecurityZone(other.getSecurityZone());
        this.setProperties(other.getProperties());
    }
}
