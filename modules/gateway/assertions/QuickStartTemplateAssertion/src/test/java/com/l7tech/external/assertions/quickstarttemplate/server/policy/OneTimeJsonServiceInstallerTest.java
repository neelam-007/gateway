package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class OneTimeJsonServiceInstallerTest extends JsonServiceInstallerTestBase {

    @Before
    public void setUp() throws Exception {
        super.before();
        Assert.assertThat(serviceInstaller, Matchers.instanceOf(OneTimeJsonServiceInstaller.class));
    }

    @After
    public void tearDown() throws Exception {
        super.after();
    }

    @Override
    protected QuickStartJsonServiceInstaller createJsonServiceInstaller(
            final QuickStartServiceBuilder serviceBuilder,
            final ServiceManager serviceManager,
            final PolicyVersionManager policyVersionManager,
            final QuickStartParser parser
    ) throws Exception {
        Assert.assertNotNull(serviceBuilder);
        Assert.assertNotNull(serviceManager);
        Assert.assertNotNull(policyVersionManager);
        Assert.assertNotNull(parser);
        return new OneTimeJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, parser);
    }

    @Test
    public void installJsonServices_calledOnlyOnce() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(1)).installJsonService(Mockito.any());

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(1)).installJsonService(Mockito.any());

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(1)).installJsonService(Mockito.any());
    }
}