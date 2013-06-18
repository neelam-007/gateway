package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This was created: 6/12/13 as 4:55 PM
 *
 * @author Victor Kazakov
 */
public class IdentityProviderTest extends DependencyTestBaseClass {

    AtomicLong idCount = new AtomicLong(1);

    @Test
    public void test() throws FindException {

        SecurityZone securityZone = new SecurityZone();
        long securityZoneOid = idCount.getAndIncrement();
        securityZone.setOid(securityZoneOid);
        mockEntity(securityZone, new EntityHeader(securityZoneOid, EntityType.SECURITY_ZONE, null, null));

        IdentityProviderConfig identityProviderConfig = new IdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        identityProviderConfig.setOid(identityProviderOid);
        identityProviderConfig.setSecurityZone(securityZone);

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(securityZoneOid, Long.parseLong(((DependentEntity) result.getDependencies().get(0).getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.SECURITY_ZONE, ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityType());
    }

    @Test
    public void testFederated0TrustedCert() throws FindException {

        FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        identityProviderConfig.setOid(identityProviderOid);

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertEquals(0, result.getDependencies().size());
    }

    @Test
    public void testFederated1TrustedCert() throws FindException {

        TrustedCert trustedCert = new TrustedCert();
        long trustedCertOid = idCount.getAndIncrement();
        trustedCert.setOid(trustedCertOid);
        mockEntity(trustedCert, new EntityHeader(trustedCertOid, EntityType.TRUSTED_CERT, null, null));

        FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        identityProviderConfig.setOid(identityProviderOid);
        identityProviderConfig.setTrustedCertOids(new long[]{trustedCertOid});

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(trustedCertOid, Long.parseLong(((DependentEntity) result.getDependencies().get(0).getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.TRUSTED_CERT, ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityType());
    }

    @Test
    public void testFederated3TrustedCert() throws FindException {

        TrustedCert trustedCert = new TrustedCert();
        long trustedCertOid = idCount.getAndIncrement();
        trustedCert.setOid(trustedCertOid);
        mockEntity(trustedCert, new EntityHeader(trustedCertOid, EntityType.TRUSTED_CERT, null, null));

        TrustedCert trustedCert2 = new TrustedCert();
        long trustedCert2Oid = idCount.getAndIncrement();
        trustedCert.setOid(trustedCert2Oid);
        mockEntity(trustedCert2, new EntityHeader(trustedCert2Oid, EntityType.TRUSTED_CERT, null, null));

        TrustedCert trustedCert3 = new TrustedCert();
        long trustedCert3Oid = idCount.getAndIncrement();
        trustedCert.setOid(trustedCert3Oid);
        mockEntity(trustedCert3, new EntityHeader(trustedCert3Oid, EntityType.TRUSTED_CERT, null, null));

        FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        identityProviderConfig.setOid(identityProviderOid);
        identityProviderConfig.setTrustedCertOids(new long[]{trustedCertOid, trustedCert2Oid, trustedCert3Oid});

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(3, result.getDependencies().size());
    }

    @Test
    public void simpleLDAP() throws FindException {
        BindOnlyLdapIdentityProviderConfig bindOnlyLdapIdentityProviderConfig = new BindOnlyLdapIdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        bindOnlyLdapIdentityProviderConfig.setOid(identityProviderOid);

        final EntityHeader identityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(bindOnlyLdapIdentityProviderConfig, identityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(identityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        //Private keys are not returned as keys yet...
    }

    @Test
    public void lDAPtest() throws FindException {
        SecurePassword securePassword = new SecurePassword();
        long securePasswordOid = idCount.getAndIncrement();
        securePassword.setOid(securePasswordOid);
        final String securePasswordName = "pass1";
        securePassword.setName(securePasswordName);
        mockEntity(securePassword, new EntityHeader(securePasswordOid, EntityType.SECURE_PASSWORD, securePasswordName, null));

        SecurePassword securePassword2 = new SecurePassword();
        long securePasswordOid2 = idCount.getAndIncrement();
        securePassword2.setOid(securePasswordOid2);
        final String securePasswordName2 = "pass2";
        securePassword2.setName(securePasswordName2);
        mockEntity(securePassword2, new EntityHeader(securePasswordOid2, EntityType.SECURE_PASSWORD, securePasswordName2, null));

        LdapIdentityProviderConfig ldapIdentityProviderConfig = new LdapIdentityProviderConfig();
        final long identityProviderOid = idCount.getAndIncrement();
        ldapIdentityProviderConfig.setOid(identityProviderOid);
        ldapIdentityProviderConfig.setBindPasswd("${secpass." + securePasswordName + ".plaintext}");
        ldapIdentityProviderConfig.setNtlmAuthenticationProviderProperties(new TreeMap<>(CollectionUtils.MapBuilder.<String, String>builder()
                .put("enabled", "true")
                .put("service.passwordOid", String.valueOf(securePasswordOid2))
                .map()));

        final EntityHeader identityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(ldapIdentityProviderConfig, identityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(identityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Long.parseLong(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertEquals(2, result.getDependencies().size());
    }
}
