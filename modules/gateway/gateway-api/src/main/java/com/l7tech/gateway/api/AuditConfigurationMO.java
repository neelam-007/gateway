package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The AuditConfigurationMO managed object represents an audit configuration.
 *
 * <p>There can be only one audit configuration</p>
 *
 * @see ManagedObjectFactory#createAuditConfiguration)
 */
@XmlRootElement(name="AuditConfiguration")
@XmlType(name="AuditConfigurationType", propOrder={"sinkPolicyReference", "lookupPolicyReference", "alwaysSaveInternal", "ftpConfig", "properties", "extensions"})
@AccessorSupport.AccessibleResource(name ="auditConfiguration")
public class AuditConfigurationMO extends ManagedObject {

    //- PUBLIC

    /**
     * Get the audit sink policy reference
     *
     * @return The reference (may be null)
     */
    @XmlElement(name="SinkPolicyReference", required=false)
    public ManagedObjectReference getSinkPolicyReference() {
        return sinkPolicyReference;
    }

    /**
     * Set the audit sink policy reference. Must be an internal policy with tag 'audit-sink'
     * @param sinkPolicyReference  The audit sink policy to use
     */
    public void setSinkPolicyReference(ManagedObjectReference sinkPolicyReference) {
        this.sinkPolicyReference = sinkPolicyReference;
    }

    /**
     * Get the audit lookup policy reference
     *
     * @return The reference (may be null)
     */
    @XmlElement(name="LookupPolicyReference", required=false)
    public ManagedObjectReference getLookupPolicyReference() {
        return lookupPolicyReference;
    }

    /**
     * Set the audit lookup policy reference. Must be an internal policy with tag 'audit-lookup'
     * @param lookupPolicyReference  The audit sink policy to use
     */
    public void setLookupPolicyReference(ManagedObjectReference lookupPolicyReference) {
        this.lookupPolicyReference = lookupPolicyReference;
    }

    /**
     * Get if audit records will always be saved to the internal database
     *
     * @return whether the audit records will always be saved to the internal database
     */
    @XmlElement(name="AlwaysSaveInternal", required=false)
    public Boolean getAlwaysSaveInternal() {
        return alwaysSaveInternal;
    }

    /**
     * Set if audit records will always be saved to the internal database
     * @param alwaysSaveInternal If audit records will always be saved to the internal database
     */
    public void setAlwaysSaveInternal(Boolean alwaysSaveInternal) {
        this.alwaysSaveInternal = alwaysSaveInternal;
    }

    /**
     * Get the FTP configuration where the archiver backs up the audit logs
     *
     * @return The FTP configuration (may be null)
     */
    @XmlElement(name="FtpConfig", required=false)
    public AuditFtpConfig getFtpConfig() {
        return ftpConfig;
    }

    /**
     * Set the FTP configuration where the archiver backs up the audit logs
     *
     * @param ftpConfig The FTP configuration
     */
    public void setFtpConfig(AuditFtpConfig ftpConfig) {
        this.ftpConfig = ftpConfig;
    }

    /**
     * Get the properties for this audit configuration.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties", required=false)
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this audit configuration.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }
    
    //- PACKAGE

    AuditConfigurationMO() {
    }

    //- PRIVATE

    private ManagedObjectReference sinkPolicyReference;
    private ManagedObjectReference lookupPolicyReference;
    private Boolean alwaysSaveInternal = true; // default value from the cluster property
    private AuditFtpConfig ftpConfig;
    private Map<String,Object> properties;

}
