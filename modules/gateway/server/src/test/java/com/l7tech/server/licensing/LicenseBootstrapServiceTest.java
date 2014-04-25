package com.l7tech.server.licensing;

import com.l7tech.gateway.common.Component;
import com.l7tech.server.event.system.Started;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class LicenseBootstrapServiceTest {

    @Test
    public void Test() throws Exception{
        URL license = this.getClass().getResource("bootstrap/ValidLicenseDocument.xml");
        assertNotNull(license);
        File licenseFile = new File(license.getFile());
        SyspropUtil.setProperty("com.l7tech.bootstrap.folder.license",licenseFile.getParent() );
        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        assertEquals(1, docManager.findAll().size());
    }
}
