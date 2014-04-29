package com.l7tech.server.licensing;

import com.l7tech.gateway.common.Component;
import com.l7tech.server.event.system.Started;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static junit.framework.Assert.assertEquals;

public class LicenseBootstrapServiceTest {

    private File tmpDir;

    @Before
    public void setUp() throws Exception {
        tmpDir = FileUtils.createTempDirectory("bootstrapLicense", null, null, false);
        SyspropUtil.setProperty("com.l7tech.bootstrap.folder.license", tmpDir.getPath());
    }

    @After
    public void after() throws Exception {
        SyspropUtil.clearProperty("com.l7tech.bootstrap.folder.license");
        FileUtils.deleteDir(tmpDir);
    }

    @Test
    public void TestLoadLicense() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        File serviceFile = new File(tmpDir,"license.xml");
        FileUtils.copyFile(new File(otherLicense.getFile()),serviceFile);

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        assertEquals(1, docManager.findAll().size());
    }

    @Test
    public void TestMoreThanOneLicense() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        FileUtils.copyFile(new File(otherLicense.getFile()), new File(tmpDir,"license.xml"));
        FileUtils.copyFile(new File(otherLicense.getFile()), new File(tmpDir,"license2.xml"));

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        assertEquals(0, docManager.findAll().size());
    }
}
