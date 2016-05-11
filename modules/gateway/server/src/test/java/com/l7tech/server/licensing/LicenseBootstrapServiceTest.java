package com.l7tech.server.licensing;

import com.l7tech.gateway.common.Component;
import com.l7tech.server.event.system.Initialized;
import com.l7tech.test.BugId;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

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
        SyspropUtil.clearProperty("com.l7tech.bootstrap.env.license.enable");
        FileUtils.deleteDir(tmpDir);
    }

    @Test
    public void TestLoadLicense() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        File serviceFile = new File(tmpDir,"license.xml");
        FileUtils.copyFile(new File(otherLicense.getFile()),serviceFile);

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.onApplicationEvent(new Initialized(this, Component.GATEWAY, "Test"));
        assertEquals(1, docManager.findAll().size());
    }

    @Test
    @BugId( "SSG-13439" )
    public void TestLoadLicenseFromEnvironment() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        byte[] licenseBytes = IOUtils.slurpUrl( otherLicense );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( InputStream is = new ByteArrayInputStream( licenseBytes ); GZIPOutputStream os = new GZIPOutputStream( baos ) ) {
            IOUtils.copyStream( is, os );
        }
        String ssgLicense = HexUtils.encodeBase64( baos.toByteArray() );

        // Enable load from env var
        SyspropUtil.setProperty( "com.l7tech.bootstrap.env.license.enable", "true" );

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.licenseFromEnv = ssgLicense; // poke license in for test (can't modify env vars, sadly)
        service.onApplicationEvent(new Initialized(this, Component.GATEWAY, "Test"));
        assertEquals( "License in environment must be installed if system property enabled",
                1, docManager.findAll().size() );
    }

    @Test
    @BugId( "SSG-13439" )
    public void TestLoadLicenseFromEnvironment_notEnabled() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        byte[] licenseBytes = IOUtils.slurpUrl( otherLicense );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( InputStream is = new ByteArrayInputStream( licenseBytes ); GZIPOutputStream os = new GZIPOutputStream( baos ) ) {
            IOUtils.copyStream( is, os );
        }
        String ssgLicense = HexUtils.encodeBase64( baos.toByteArray() );

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.licenseFromEnv = ssgLicense; // poke license in for test (can't modify env vars, sadly)
        service.onApplicationEvent(new Initialized(this, Component.GATEWAY, "Test"));
        assertEquals( "License in environment must be ignored if system property not enabled",
                0, docManager.findAll().size() );
    }

    @Test
    public void TestMoreThanOneLicense() throws Exception{
        URL otherLicense = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithoutSignature.xml");
        URL otherLicense2 = LicenseBootstrapServiceTest.class.getResource("ValidLicenseDocumentWithValidSignature.xml");
        FileUtils.copyFile(new File(otherLicense.getFile()), new File(tmpDir,"license.xml"));
        FileUtils.copyFile(new File(otherLicense2.getFile()), new File(tmpDir,"license2.xml"));

        LicenseDocumentManager docManager = new LicenseDocumentManagerStub();
        BootstrapLicenseService service = new BootstrapLicenseService(docManager);
        service.onApplicationEvent(new Initialized(this, Component.GATEWAY, "Test"));
        assertEquals(2, docManager.findAll().size());
    }
}
