package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.GatewayState;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.export.PolicyExporterImporterManagerStub;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import com.l7tech.server.transport.ActiveTransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.Injector;
import com.l7tech.test.BugId;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests the mq native dependency processor
 */
public class MQNativeDependencyProcessorTest extends DependencyTestBaseClass {
    AtomicLong idCount = new AtomicLong(1);

    @Before
    public void before() throws KeyStoreException, IOException, FindException {
        super.before();
        final ServerConfigStub serverConfigStub = new ServerConfigStub();
        final GatewayState gatewayState = new GatewayState();
        GenericApplicationContext applicationContext = new GenericApplicationContext();

        applicationContext.getBeanFactory().registerSingleton("serverConfig", serverConfigStub);
        applicationContext.getBeanFactory().registerSingleton("policyExporterImporterManager", new PolicyExporterImporterManagerStub());
        applicationContext.getBeanFactory().registerSingleton("injector", new Injector() {
            @Override
            public void inject(Object target) {
                if(target instanceof MqNativeModule){
                    try {
                        Method setLicenseManagerMethod = ActiveTransportModule.class.getDeclaredMethod("setLicenseManager", LicenseManager.class);
                        setLicenseManagerMethod.setAccessible(true);
                        setLicenseManagerMethod.invoke(target, new TestLicenseManager());

                        Field serverConfigField = MqNativeModule.class.getDeclaredField("serverConfig");
                        serverConfigField.setAccessible(true);
                        serverConfigField.set(target, serverConfigStub);

                        Field gatewayStateField = MqNativeModule.class.getDeclaredField("gatewayState");
                        gatewayStateField.setAccessible(true);
                        gatewayStateField.set(target, gatewayState);

                    } catch (NoSuchMethodException | NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
                        Assert.fail(e.getMessage());
                    }
                }
            }
        });
        applicationContext.getBeanFactory().registerSingleton("ssgActiveConnectorDependencyProcessorRegistry", ssgActiveConnectorDependencyProcessorRegistry);
        applicationContext.getBeanFactory().registerSingleton("applicationEventProxy", new ApplicationEventProxy());
        applicationContext.getBeanFactory().registerSingleton("defaultKey", defaultKey);

        applicationContext.refresh();

        MqNativeModuleLoadListener.onModuleLoaded(applicationContext);
    }

    @After
    public void after(){
        MqNativeModuleLoadListener.onModuleUnloaded();
        super.after();
    }

    @BugId("SSG-10843")
    @Test
    public void testDependencyProcessorRegistered(){
        DependencyProcessor dependencyProcessor = ssgActiveConnectorDependencyProcessorRegistry.get(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        Assert.assertNotNull(dependencyProcessor);
    }

    @BugId("SSG-10835")
    @Test
    public void testDefaultSslKeyGetsReturnedAsDependency() throws FindException, CannotRetrieveDependenciesException {
        SsgActiveConnector ssgActiveConnector = new SsgActiveConnector();
        Goid ssgActiveConnectorGoid = new Goid(0, idCount.getAndIncrement());
        ssgActiveConnector.setGoid(ssgActiveConnectorGoid);
        ssgActiveConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        ssgActiveConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "true");
        ssgActiveConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED, "true");

        EntityHeader ssgActiveConnectorHeader = EntityHeaderUtils.fromEntity(ssgActiveConnector);
        mockEntity(ssgActiveConnector, ssgActiveConnectorHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(ssgActiveConnectorHeader);

        org.junit.Assert.assertNotNull(result);
        org.junit.Assert.assertEquals(ssgActiveConnectorGoid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        org.junit.Assert.assertEquals(EntityType.SSG_ACTIVE_CONNECTOR, result.getDependent().getDependencyType().getEntityType());
        org.junit.Assert.assertNotNull(result.getDependencies());
        org.junit.Assert.assertEquals(defaultSslKey.getId(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        org.junit.Assert.assertEquals(EntityType.SSG_KEY_ENTRY, result.getDependencies().get(0).getDependent().getDependencyType().getEntityType());
    }
}
