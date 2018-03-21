package com.l7tech.server.messageprocessor.injection;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.extension.registry.processorinjection.ServiceInjectionsRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageProcessorInjectorImplTest {

    @Mock
    PolicyEnforcementContext pec;
    PublishedService publishedService = new PublishedService();

    @Before
    public void before() {
        Mockito.when(pec.getService()).thenReturn(publishedService);
    }

    @Test
    public void testExecuteInjections() {
        ServiceInjectionsRegistry serviceInjectionsRegistry = new ServiceInjectionsRegistry();

        // empty registry
        boolean continueProcessing = MessageProcessorInjectorImpl.executeInjections(serviceInjectionsRegistry, pec);
        Assert.assertTrue(continueProcessing);

        // single item in register
        serviceInjectionsRegistry.register("test1", context -> true, "tag1");
        continueProcessing = MessageProcessorInjectorImpl.executeInjections(serviceInjectionsRegistry, pec);
        Assert.assertTrue(continueProcessing);

        serviceInjectionsRegistry.register("test2", context -> false, "tag2");
        continueProcessing = MessageProcessorInjectorImpl.executeInjections(serviceInjectionsRegistry, pec);
        Assert.assertTrue(continueProcessing);

        publishedService.putProperty("tags", "tag1");
        continueProcessing = MessageProcessorInjectorImpl.executeInjections(serviceInjectionsRegistry, pec);
        Assert.assertTrue(continueProcessing);

        publishedService.putProperty("tags", "tag1,tag2");
        continueProcessing = MessageProcessorInjectorImpl.executeInjections(serviceInjectionsRegistry, pec);
        Assert.assertFalse(continueProcessing);

    }

}