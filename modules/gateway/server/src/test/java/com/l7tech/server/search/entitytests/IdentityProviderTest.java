package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
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
    public void test() throws FindException, CannotRetrieveDependenciesException {

        SecurityZone securityZone = new SecurityZone();
        Goid securityZoneGoid = nextGoid();
        securityZone.setGoid(securityZoneGoid);
        mockEntity(securityZone, new EntityHeader(securityZoneGoid, EntityType.SECURITY_ZONE, null, null));

        IdentityProviderConfig identityProviderConfig = new IdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        identityProviderConfig.setGoid(identityProviderOid);
        identityProviderConfig.setSecurityZone(securityZone);

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(securityZoneGoid.toHexString(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.SECURITY_ZONE, ((DependentEntity) result.getDependencies().get(0).getDependent()).getDependencyType().getEntityType());
    }

    @Test
    public void testFederated0TrustedCert() throws FindException, CannotRetrieveDependenciesException {

        FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        identityProviderConfig.setGoid(identityProviderOid);

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertEquals(0, result.getDependencies().size());
    }

    @Test
    public void testFederated1TrustedCert() throws FindException, CannotRetrieveDependenciesException {

        TrustedCert trustedCert = new TrustedCert();
        Goid trustedCertOid = nextGoid();
        trustedCert.setGoid(trustedCertOid);
        mockEntity(trustedCert, new EntityHeader(trustedCertOid, EntityType.TRUSTED_CERT, null, null));

        FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        identityProviderConfig.setGoid(identityProviderOid);
        identityProviderConfig.setTrustedCertGoids(new Goid[]{trustedCertOid});

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(trustedCertOid, new Goid(((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.TRUSTED_CERT, ((DependentEntity) result.getDependencies().get(0).getDependent()).getDependencyType().getEntityType());
    }

    private Goid nextGoid() {
        return new Goid(0, idCount.getAndIncrement());
    }

    private TrustedCert nextTrustedCert() throws FindException {
        TrustedCert trustedCert = new TrustedCert();
        trustedCert.setGoid(nextGoid());
        return trustedCert;
    }

    private TrustedCert nextTrustedCert(boolean mocked) throws FindException {
        TrustedCert trustedCert = nextTrustedCert();
        if (mocked) {
            mockEntity(trustedCert, new EntityHeader(trustedCert.getGoid(), EntityType.TRUSTED_CERT, null, null));
        }
        return trustedCert;
    }

    @Test
    public void testFederated3TrustedCert() throws FindException, CannotRetrieveDependenciesException {
        final TrustedCert trustedCert1 = nextTrustedCert(true);
        final TrustedCert trustedCert2 = nextTrustedCert(true);
        final TrustedCert trustedCert3 = nextTrustedCert(true);

        final FederatedIdentityProviderConfig identityProviderConfig = new FederatedIdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        identityProviderConfig.setGoid(identityProviderOid);
        identityProviderConfig.setTrustedCertGoids(new Goid[]{trustedCert1.getGoid(), trustedCert2.getGoid(), trustedCert3.getGoid()});

        final EntityHeader IdentityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);
        mockEntity(identityProviderConfig, IdentityProviderConfigEntityHeader);

        final DependencySearchResults result = dependencyAnalyzer.getDependencies(IdentityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(3, result.getDependencies().size());
    }

    @Test
    public void testUniqueDependencyUsingTrustedCerts() throws FindException, CannotRetrieveDependenciesException {
        final TrustedCert trustedCert1 = nextTrustedCert(true);
        final TrustedCert trustedCert2 = nextTrustedCert(true);
        final TrustedCert trustedCert3 = nextTrustedCert(true);

        DependencySearchResults result;

        //Adding 1 unique and 2 duplicate certificates to the Config object and then getting a count of the unique dependencies
        FederatedIdentityProviderConfig identityProviderConfigWithTwoDuplicates = new FederatedIdentityProviderConfig();
        final Goid identityProviderOid2 = new Goid(0,idCount.getAndIncrement());
        identityProviderConfigWithTwoDuplicates.setGoid(identityProviderOid2);
        identityProviderConfigWithTwoDuplicates.setTrustedCertGoids(new Goid[]{trustedCert1.getGoid(), trustedCert2.getGoid(), trustedCert2.getGoid()});
        final EntityHeader twoDuplicatesConfigEntityHeader = new EntityHeader(identityProviderOid2, EntityType.ID_PROVIDER_CONFIG, null, null);
        mockEntity(identityProviderConfigWithTwoDuplicates, twoDuplicatesConfigEntityHeader);
        //Should get 2 as only two are unique this time
        result = dependencyAnalyzer.getDependencies(twoDuplicatesConfigEntityHeader);
        Assert.assertEquals(2, result.getDependencies().size());

        //Adding 3 duplicate certificates to the Config object and then getting a count of the unique dependencies
        FederatedIdentityProviderConfig identityProviderConfigWithThreeDuplicates = new FederatedIdentityProviderConfig();
        final Goid identityProviderOid3 = new Goid(0,idCount.getAndIncrement());
        identityProviderConfigWithThreeDuplicates.setGoid(identityProviderOid3);
        identityProviderConfigWithThreeDuplicates.setTrustedCertGoids(new Goid[]{trustedCert3.getGoid(), trustedCert3.getGoid(), trustedCert3.getGoid()});
        final EntityHeader threeDuplicatesConfigEntityHeader = new EntityHeader(identityProviderOid3, EntityType.ID_PROVIDER_CONFIG, null, null);
        mockEntity(identityProviderConfigWithThreeDuplicates, threeDuplicatesConfigEntityHeader);
        //Should get 1 as all are duplicates this time
        result = dependencyAnalyzer.getDependencies(threeDuplicatesConfigEntityHeader);
        Assert.assertEquals(1, result.getDependencies().size());
    }

    @Test
    public void simpleLDAP() throws FindException, CannotRetrieveDependenciesException {
        BindOnlyLdapIdentityProviderConfig bindOnlyLdapIdentityProviderConfig = new BindOnlyLdapIdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        bindOnlyLdapIdentityProviderConfig.setGoid(identityProviderOid);

        final EntityHeader identityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(bindOnlyLdapIdentityProviderConfig, identityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(identityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        //Private keys are not returned as keys yet...
    }

    @Test
    public void lDAPtest() throws FindException, CannotRetrieveDependenciesException {
        SecurePassword securePassword = new SecurePassword();
        Goid securePasswordGoid = new Goid(0,idCount.getAndIncrement());
        securePassword.setGoid(securePasswordGoid);
        final String securePasswordName = "pass1";
        securePassword.setName(securePasswordName);
        mockEntity(securePassword, new EntityHeader(securePasswordGoid, EntityType.SECURE_PASSWORD, securePasswordName, null));

        SecurePassword securePassword2 = new SecurePassword();
        Goid securePasswordGoid2 = new Goid(0,idCount.getAndIncrement());
        securePassword2.setGoid(securePasswordGoid2);
        final String securePasswordName2 = "pass2";
        securePassword2.setName(securePasswordName2);
        mockEntity(securePassword2, new EntityHeader(securePasswordGoid2, EntityType.SECURE_PASSWORD, securePasswordName2, null));

        LdapIdentityProviderConfig ldapIdentityProviderConfig = new LdapIdentityProviderConfig();
        final Goid identityProviderOid = new Goid(0,idCount.getAndIncrement());
        ldapIdentityProviderConfig.setGoid(identityProviderOid);
        ldapIdentityProviderConfig.setBindPasswd("${secpass." + securePasswordName + ".plaintext}");
        ldapIdentityProviderConfig.setNtlmAuthenticationProviderProperties(new TreeMap<>(CollectionUtils.MapBuilder.<String, String>builder()
                .put("enabled", "true")
                .put("service.passwordOid", String.valueOf(securePasswordGoid2))
                .map()));

        final EntityHeader identityProviderConfigEntityHeader = new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null);

        mockEntity(ldapIdentityProviderConfig, identityProviderConfigEntityHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(identityProviderConfigEntityHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(identityProviderOid, Goid.parseGoid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.ID_PROVIDER_CONFIG, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertEquals(3, result.getDependencies().size());
    }
}
