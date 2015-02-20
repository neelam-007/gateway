package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by vkazakov on 2/18/2015.
 */
public class HttpConfigurationDependencyProcessorTest extends DependencyTestBaseClass {
    AtomicLong idCount = new AtomicLong(1);

    @BugId("SSG-10835")
    @Test
    public void defaultSSLPrivateKey() throws FindException, CannotRetrieveDependenciesException, IOException, KeyStoreException {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        Goid httpConfigurationGoid = new Goid(0, idCount.getAndIncrement());
        httpConfiguration.setGoid(httpConfigurationGoid);
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.DEFAULT);
        EntityHeader httpConfigHeader = EntityHeaderUtils.fromEntity(httpConfiguration);
        mockEntity(httpConfiguration, httpConfigHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(httpConfigHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(httpConfigurationGoid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.HTTP_CONFIGURATION, result.getDependent().getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(defaultSslKey.getId(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY, result.getDependencies().get(0).getDependent().getDependencyType().getEntityType());
    }

    @Test
    public void noPrivateKey() throws FindException, CannotRetrieveDependenciesException, IOException, KeyStoreException {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        Goid httpConfigurationGoid = new Goid(0, idCount.getAndIncrement());
        httpConfiguration.setGoid(httpConfigurationGoid);
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.NONE);
        EntityHeader httpConfigHeader = EntityHeaderUtils.fromEntity(httpConfiguration);
        mockEntity(httpConfiguration, httpConfigHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(httpConfigHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(httpConfigurationGoid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.HTTP_CONFIGURATION, result.getDependent().getDependencyType().getEntityType());
        Assert.assertEquals(0, result.getDependencies().size());
    }

    @Test
    public void customPrivateKey() throws FindException, CannotRetrieveDependenciesException, IOException, KeyStoreException {
        final SsgKeyEntry customKey = SsgKeyEntry.createDummyEntityForAuditing(defaultKeystoreId, "myCustomAlias");

        Mockito.when(defaultKey.getSslInfo()).thenReturn(customKey);

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        Goid httpConfigurationGoid = new Goid(0, idCount.getAndIncrement());
        httpConfiguration.setGoid(httpConfigurationGoid);
        httpConfiguration.setTlsKeyUse(HttpConfiguration.Option.CUSTOM);
        httpConfiguration.setTlsKeystoreGoid(customKey.getKeystoreId());
        httpConfiguration.setTlsKeystoreAlias(customKey.getAlias());
        EntityHeader httpConfigHeader = EntityHeaderUtils.fromEntity(httpConfiguration);
        mockEntity(httpConfiguration, httpConfigHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(httpConfigHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(httpConfigurationGoid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.HTTP_CONFIGURATION, result.getDependent().getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(customKey.getId(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY, result.getDependencies().get(0).getDependent().getDependencyType().getEntityType());
    }
}
