package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.server.GatewayState;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.export.PolicyExporterImporterManagerStub;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import com.l7tech.server.transport.ActiveTransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.Injector;
import com.l7tech.test.BugId;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Tests the mq native dependency processor
 */
public class MQNativeDependencyProcessorTest extends DependencyTestBaseClass {

    @Spy
    GenericApplicationContext applicationContext = new GenericApplicationContext();

    @Before
    public void before() {
        final ServerConfigStub serverConfigStub = new ServerConfigStub();
        final GatewayState gatewayState = new GatewayState();
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

        applicationContext.refresh();

        MqNativeModuleLoadListener.onModuleLoaded(applicationContext);
    }

    @BugId("SSG-10843")
    @Test
    public void testDependencyProcessorRegistered(){
        DependencyProcessor dependencyProcessor = ssgActiveConnectorDependencyProcessorRegistry.get(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        Assert.assertNotNull(dependencyProcessor);
    }
}
