package com.l7tech.server.search.entitytests;

import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This was created: 6/12/13 as 4:55 PM
 *
 * @author Victor Kazakov
 */
public class TrustedCertTest extends DependencyTestBaseClass {

    AtomicLong idCount = new AtomicLong(1);

    @Test
    public void test() throws FindException {

        SecurityZone securityZone = new SecurityZone();
        Goid securityZoneGoid = new Goid(0,idCount.getAndIncrement());
        securityZone.setGoid(securityZoneGoid);
        mockEntity(securityZone, new EntityHeader(securityZoneGoid, EntityType.SECURITY_ZONE, null, null));

        TrustedCert trustedCert = new TrustedCert();
        final Goid trustedCertOid = new Goid(0, idCount.getAndIncrement());
        trustedCert.setGoid(trustedCertOid);
        trustedCert.setSecurityZone(securityZone);

        final EntityHeader trustedCertHeader = new EntityHeader(trustedCertOid, EntityType.TRUSTED_CERT, null, null);

        mockEntity(trustedCert, trustedCertHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(trustedCertHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(trustedCertOid, new Goid(((DependentEntity) result.getDependent()).getInternalID()));
        Assert.assertEquals(EntityType.TRUSTED_CERT, ((DependentEntity) result.getDependent()).getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(securityZoneGoid.toHexString(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getInternalID());
        Assert.assertEquals(EntityType.SECURITY_ZONE, ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityType());
    }
}
