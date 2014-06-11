package com.l7tech.identity.external;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Identity provider configuration for a policy-backed identity provider.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@DiscriminatorValue("5")
public class PolicyBackedIdentityProviderConfig extends IdentityProviderConfig {

    public PolicyBackedIdentityProviderConfig(IdentityProviderConfig config) {
        super(IdentityProviderType.POLICY_BACKED);
        setVersion(config.getVersion());
        setGoid(config.getGoid());
        copyFrom(config);
    }

    public PolicyBackedIdentityProviderConfig() {
        super(IdentityProviderType.POLICY_BACKED);
    }

    @Transient
    @Dependency(type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPolicyId() {
        return (Goid) getProperty(PROP_POLICY_ID);
    }

    public void setPolicyId(Goid policyId) {
        setProperty(PROP_POLICY_ID, policyId);
    }

    @Transient
    @Dependency(type = Dependency.DependencyType.RBAC_ROLE, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getDefaultRoleId() {
        return (Goid) getProperty(PROP_ROLE_ID);
    }

    public void setDefaultRoleId(Goid roleId) {
        setProperty(PROP_ROLE_ID, roleId);
    }

    @Override
    @Transient
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean canIssueCertificates() {
        return false;
    }

    @Override
    @Transient
    public boolean isCredentialsRequiredForTest() {
        return true;
    }

    private static final String PROP_POLICY_ID = "policyId";
    private static final String PROP_ROLE_ID = "roleId";
}
