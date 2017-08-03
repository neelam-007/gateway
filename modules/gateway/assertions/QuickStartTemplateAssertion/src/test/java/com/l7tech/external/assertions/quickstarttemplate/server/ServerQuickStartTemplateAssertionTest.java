package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilder;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerQuickStartTemplateAssertionTest extends QuickStartTestBase {

    @Mock
    private QuickStartAssertionLocator assertionLocator;

    @Mock
    private QuickStartServiceBuilder builder;

    @Mock
    private PolicyEnforcementContext context;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    FolderManager folderManager;

    @Mock
    EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;

    @Mock
    ServiceManager serviceManager;

    @Mock
    ServiceCache serviceCache;

    @Mock
    PolicyVersionManager policyVersionManager;

    @Mock
    ClusterPropertyManager clusterPropertyManager;

    @Mock
    GatewayState gatewayState;

    PublishedService publishedService;

    private ServerQuickStartTemplateAssertion fixture;

    @Before
    public void setUp() throws PolicyAssertionException, FindException, QuickStartPolicyBuilderException {

        when(applicationContext.getBean("folderManager", FolderManager.class)).thenReturn(folderManager);
        when(applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class)).thenReturn(encapsulatedAssertionConfigManager);
        when(applicationContext.getBean("serviceManager", ServiceManager.class)).thenReturn(serviceManager);
        when(applicationContext.getBean("serviceCache", ServiceCache.class)).thenReturn(serviceCache);
        when(applicationContext.getBean("policyVersionManager", PolicyVersionManager.class)).thenReturn(policyVersionManager);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        when(applicationContext.getBean("gatewayState", GatewayState.class)).thenReturn(gatewayState);
        when(gatewayState.isReadyForMessages()).thenReturn(true);

        publishedService = new PublishedService();

        QuickStartAssertionModuleLifecycle.onModuleLoaded(applicationContext, builder);
        when(builder.createService(any())).thenReturn(publishedService);
        fixture = new ServerQuickStartTemplateAssertion(new QuickStartTemplateAssertion(), applicationContext);

    }


    @Test
    public void checkRequestNoVersion() throws FindException, IOException, PolicyAssertionException, SAXException {

        String json= "{\n" +
                "    \"Service\": {\n" +
                "    \"name\": \"Google Search\",\n" +
                "    \"gatewayUri\": \"/google25\",\n" +
                "    \"httpMethods\": [ \"get\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RouteHttp\" : {\n" +
                "          \"targetUrl\": \"http://www.google.com/search${request.url.query}\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Message msg = makeMessage(ContentTypeHeader.APPLICATION_JSON, json);
        fixture.doCheckRequest(context, msg, "", null);
        assertTrue(!publishedService.getProperty(QuickStartTemplateAssertion.PROPERTY_QS_REGISTRAR_TMS).isEmpty());
    }

    @Test
    public void checkRequestVersion() throws FindException, IOException, PolicyAssertionException, SAXException, NoSuchVariableException {

        String json= "{\n" +
                "    \"Service\": {\n" +
                "    \"name\": \"Google Search\",\n" +
                "    \"gatewayUri\": \"/google25\",\n" +
                "    \"httpMethods\": [ \"get\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RouteHttp\" : {\n" +
                "          \"targetUrl\": \"http://www.google.com/search${request.url.query}\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        String serviceVersion = "22222";
        Message msg = makeMessage(ContentTypeHeader.APPLICATION_JSON, json);
        when(context.getVariable(QuickStartTemplateAssertion.QS_VERSION)).thenReturn(serviceVersion);
        fixture.doCheckRequest(context, msg, "", null);
        assertTrue(publishedService.getProperty(QuickStartTemplateAssertion.PROPERTY_QS_REGISTRAR_TMS).equals(serviceVersion));
    }

    static Message makeMessage(ContentTypeHeader contentType, String body) throws IOException {
        return new Message(new ByteArrayStashManager(), contentType, new ByteArrayInputStream(body.getBytes()));
    }

}