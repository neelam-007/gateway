package com.l7tech.server.service;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.system.Started;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static nu.xom.tests.XOMTestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ServiceTemplateBootstrapServiceTest {

    static final String templateName = "testTemplate";
    private ServiceManager serviceManager;
    private ServiceTemplateBootstrapService bootstrapService;
    private ServiceTemplateManager serviceTemplateManager;
    private ServiceTemplate serviceTemplate;
    private File tmpDir;

    @Before
    public void setUp() throws Exception {
        tmpDir = FileUtils.createTempDirectory("serviceTemplate", null, null, false);
        File serviceFile = new File(tmpDir,templateName);
        FileUtils.touch(serviceFile);
        SyspropUtil.setProperty("com.l7tech.bootstrap.folder.services", serviceFile.getParent());

        ApplicationContext applicationContext =  ApplicationContexts.getTestApplicationContext();
        serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        List<ServiceHeader> headers = CollectionUtils.toList(serviceManager.findAllHeaders());
        for(ServiceHeader header: headers ){
            serviceManager.delete(header.getGoid());
        }
        bootstrapService = applicationContext.getBean("serviceTemplateBootstrapService", ServiceTemplateBootstrapService.class);
        bootstrapService.loadFolder();
        serviceTemplateManager = applicationContext.getBean("serviceTemplateManager", ServiceTemplateManager.class);

        final Assertion allAss = new AllAssertion( Arrays.asList(
                new AuditDetailAssertion("detail")
        ) );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(allAss, outputStream);
        String policyContents =  HexUtils.decodeUtf8(outputStream.toByteArray());

        serviceTemplate = new ServiceTemplate(
                "Test Template Service",
                "/testTemplate/*",
                policyContents,
                ServiceType.OTHER_INTERNAL_SERVICE,
                null);

        serviceTemplateManager.register(serviceTemplate, templateName);
    }

    @After
    public void after() throws Exception{
        FileUtils.deleteDir(tmpDir);
        SyspropUtil.clearProperty("com.l7tech.bootstrap.folder.services");
    }

    @Test
    public void testLoadServiceTemplate() throws Exception {
        bootstrapService.init();
        bootstrapService.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));

        assertEquals(1,serviceManager.findAll().size());
        assertEquals(1, serviceManager.findByRoutingUri(serviceTemplate.getDefaultUriPrefix()).size());
        assertNotNull(serviceManager.findByUniqueName(serviceTemplate.getName()));
    }

    @Test
    public void testNoServiceTemplateLoaded() throws Exception {
        // insert services
        for(PublishedService service: StubDataStore.defaultStore().getPublishedServices().values()){
            serviceManager.save(service);
        }
        assertTrue("Should contain published services", serviceManager.findAll().size()>0);

        bootstrapService.init();
        bootstrapService.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));

        assertEquals(0, serviceManager.findByRoutingUri(serviceTemplate.getDefaultUriPrefix()).size());
        assertNull(serviceManager.findByUniqueName(serviceTemplate.getName()));
    }
}
