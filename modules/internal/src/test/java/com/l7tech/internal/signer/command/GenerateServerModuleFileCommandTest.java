package com.l7tech.internal.signer.command;

import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.test.util.TestUtils;
import com.l7tech.util.SyspropUtil;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class GenerateServerModuleFileCommandTest {
    private File tmpDir;
    private GenerateServerModuleFileCommand command;


    @Before
    public void setUp() throws Exception {
        tmpDir = new File(SyspropUtil.getProperty("java.io.tmpdir"));
        command = new GenerateServerModuleFileCommand();
    }

    @Test
    public void testGenerateOutFileName() throws Exception {
        File outFile = command.generateOutFileName(new File(tmpDir, "test.blah"));
        Assert.assertNotNull(outFile);
        Assert.assertThat(outFile.getCanonicalPath(), Matchers.equalTo(new File(tmpDir, "test.blah.xml").getCanonicalPath()));

        outFile = command.generateOutFileName(new File(tmpDir, "test.1"));
        Assert.assertNotNull(outFile);
        Assert.assertThat(outFile.getCanonicalPath(), Matchers.equalTo(new File(tmpDir, "test.1.xml").getCanonicalPath()));

        outFile = command.generateOutFileName(new File(tmpDir, "test."));
        Assert.assertNotNull(outFile);
        Assert.assertThat(outFile.getCanonicalPath(), Matchers.equalTo(new File(tmpDir, "test.xml").getCanonicalPath()));

        outFile = command.generateOutFileName(new File(tmpDir, "test"));
        Assert.assertNotNull(outFile);
        Assert.assertThat(outFile.getCanonicalPath(), Matchers.equalTo(new File(tmpDir, "test.xml").getCanonicalPath()));
    }

    @Test
    public void testConvertModuleType() throws Exception {
        Assert.assertThat(GenerateServerModuleFileCommand.convertModuleType(ModuleType.MODULAR_ASSERTION), Matchers.equalTo(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION));
        Assert.assertThat(GenerateServerModuleFileCommand.convertModuleType(ModuleType.CUSTOM_ASSERTION), Matchers.equalTo(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION));

        try {
            //noinspection ConstantConditions
            GenerateServerModuleFileCommand.convertModuleType(null);
            Assert.fail("convertModuleType mustn't accept null params");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testGatherProperties() throws Exception {
        ServerModuleFile moduleFile = new ServerModuleFile();
        for (final String prop : ServerModuleFile.getPropertyKeys()) {
            moduleFile.setProperty(prop, prop + "_value");
        }
        Map<String, String> props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(ServerModuleFile.getPropertyKeys().length));
        for (final String prop : ServerModuleFile.getPropertyKeys()) {
            Assert.assertThat(props, Matchers.hasEntry(prop, prop + "_value"));
        }

        moduleFile = new ServerModuleFile();
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "name1");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "size1");
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "assertion1");
        moduleFile.setProperty("blah", "blah1");
        props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(3));
        Assert.assertThat(props, Matchers.hasEntry(ServerModuleFile.PROP_FILE_NAME, "name1"));
        Assert.assertThat(props, Matchers.hasEntry(ServerModuleFile.PROP_SIZE, "size1"));
        Assert.assertThat(props, Matchers.hasEntry(ServerModuleFile.PROP_ASSERTIONS, "assertion1"));
        Assert.assertThat(props, Matchers.not(Matchers.hasKey("blah")));

        moduleFile = new ServerModuleFile();
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "name1");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, null);
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, null);
        props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(1));
        Assert.assertThat(props, Matchers.hasEntry(ServerModuleFile.PROP_FILE_NAME, "name1"));
        Assert.assertThat(props, Matchers.not(Matchers.hasKey(ServerModuleFile.PROP_SIZE)));
        Assert.assertThat(props, Matchers.not(Matchers.hasKey(ServerModuleFile.PROP_ASSERTIONS)));

        moduleFile = new ServerModuleFile();
        moduleFile.setProperty("blah", "blah1");
        props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNull(props);

        moduleFile = new ServerModuleFile();
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, null);
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, null);
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, null);
        props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNull(props);

        moduleFile = new ServerModuleFile();
        props = GenerateServerModuleFileCommand.gatherProperties(moduleFile);
        Assert.assertNull(props);
    }


    /**
     * IMPORTANT: Revisit this code if renaming fields:
     * <ul>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNING_CERT_PROPS}</li>
     *     <li>{@link com.l7tech.gateway.common.security.signer.SignerUtils#SIGNATURE_PROP}</li>
     * </ul>
     */
    private static final String SIGNING_CERT_PROPS = TestUtils.getFieldValue(SignerUtils.class, "SIGNING_CERT_PROPS", String.class);
    private static final String SIGNATURE_PROP = TestUtils.getFieldValue(SignerUtils.class, "SIGNATURE_PROP", String.class);

    @Test
    public void testGatherSignatureProperties() throws Exception {
        Properties sigProps = new Properties();
        for (final String prop : SignerUtils.ALL_SIGNING_PROPERTIES) {
            sigProps.setProperty(prop, prop + "_value");
        }
        StringWriter writer = new StringWriter();
        sigProps.store(writer, "test signature");
        writer.flush();
        Map<String, String> props = GenerateServerModuleFileCommand.gatherSignatureProperties(writer.toString());
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(SignerUtils.ALL_SIGNING_PROPERTIES.length));
        for (final String prop : SignerUtils.ALL_SIGNING_PROPERTIES) {
            Assert.assertThat(props, Matchers.hasEntry(prop, prop + "_value"));
        }

        sigProps = new Properties();
        sigProps.setProperty(SIGNING_CERT_PROPS, "cert1");
        sigProps.setProperty(SIGNATURE_PROP, "signature1");
        sigProps.setProperty("blah", "blah1");
        writer = new StringWriter();
        sigProps.store(writer, "test signature");
        writer.flush();
        props = GenerateServerModuleFileCommand.gatherSignatureProperties(writer.toString());
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(2));
        Assert.assertThat(props, Matchers.hasEntry(SIGNING_CERT_PROPS, "cert1"));
        Assert.assertThat(props, Matchers.hasEntry(SIGNATURE_PROP, "signature1"));
        Assert.assertThat(props, Matchers.not(Matchers.hasEntry("blah", "blah1")));

        sigProps = new Properties();
        sigProps.setProperty(SIGNING_CERT_PROPS, "cert1");
        sigProps.setProperty(SIGNATURE_PROP, "");
        writer = new StringWriter();
        sigProps.store(writer, "test signature");
        writer.flush();
        props = GenerateServerModuleFileCommand.gatherSignatureProperties(writer.toString());
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(2));
        Assert.assertThat(props, Matchers.hasEntry(SIGNING_CERT_PROPS, "cert1"));
        Assert.assertThat(props, Matchers.hasEntry(SIGNATURE_PROP, ""));

        sigProps = new Properties();
        sigProps.setProperty(SIGNING_CERT_PROPS, "");
        sigProps.setProperty(SIGNATURE_PROP, "signature1");
        writer = new StringWriter();
        sigProps.store(writer, "test signature");
        writer.flush();
        props = GenerateServerModuleFileCommand.gatherSignatureProperties(writer.toString());
        Assert.assertNotNull(props);
        Assert.assertThat(props.size(), Matchers.equalTo(2));
        Assert.assertThat(props, Matchers.hasEntry(SIGNING_CERT_PROPS, ""));
        Assert.assertThat(props, Matchers.hasEntry(SIGNATURE_PROP, "signature1"));

        sigProps = new Properties();
        sigProps.setProperty("blah", "blah1");
        writer = new StringWriter();
        sigProps.store(writer, "test signature");
        writer.flush();
        props = GenerateServerModuleFileCommand.gatherSignatureProperties(writer.toString());
        Assert.assertNull(props);

        props = GenerateServerModuleFileCommand.gatherSignatureProperties(null);
        Assert.assertNull(props);

        props = GenerateServerModuleFileCommand.gatherSignatureProperties("");
        Assert.assertNull(props);

        props = GenerateServerModuleFileCommand.gatherSignatureProperties("    ");
        Assert.assertNull(props);

        props = GenerateServerModuleFileCommand.gatherSignatureProperties("blah blah");
        Assert.assertNull(props);

        props = GenerateServerModuleFileCommand.gatherSignatureProperties("blah;blah;blah");
        Assert.assertNull(props);
    }

    private static final String CUSTOM_ASSERTION_SJAR = "com/l7tech/internal/signer/com.l7tech.DynamicCustomAssertion.sjar";
    private static final String MODULAR_ASSERTION_SAAR = "com/l7tech/internal/signer/com.l7tech.XmlSecurityModularAssertion.saar";

    @Test
    public void testLoadServerModuleFileFromSignedZip() throws Exception {
        // test custom assertion
        try (final InputStream is = getResourceStream(CUSTOM_ASSERTION_SJAR)) {
            final ServerModuleFile module = GenerateServerModuleFileCommand.loadServerModuleFileFromSignedZip(is, "com.l7tech.DynamicCustomAssertion.sjar");
            Assert.assertNotNull(module);
            Assert.assertThat(module.getModuleType(), Matchers.is(ModuleType.CUSTOM_ASSERTION));
            Assert.assertNotNull(module.getData());
            Assert.assertNotNull(module.getData().getDataBytes());
            Assert.assertThat(module.getData().getDataBytes().length, Matchers.is(3437));
            Assert.assertNotNull(module.getData().getSignatureProperties());
            Assert.assertThat(module.getData().getSignatureProperties(), Matchers.equalToIgnoringWhiteSpace("#Signature\n" +
                    "#Wed Sep 16 17:00:07 PDT 2015\n" +
                    "signature=MEYCIQDLDtcM3ChY60l2nuhB7aJYIcGozGrT4/Wj5YPI86Xt4AIhAPkB6+gn+EPVLDxKysknuOuP4/+1zSoy4YGIJzmL1Dx4\n" +
                    "cert=MIIBdjCCAR6gAwIBAgIJAOC+DREEgh+1MAkGByqGSM49BAEwGzEZMBcGA1UEAxMQcm9vdC5hcGltLmNhLmNvbTAeFw0xNTA4MDYyMzM5MTVaFw0zNTA4MDYyMzM5MTVaMCUxIzAhBgNVBAMTGnNpZ25lci5nYXRld2F5LmFwaW0uY2EuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWR6rsgeDYY4nCkqjy12QBZW/U/dkWbM0NSEJnWwwDeKMD8rKgPygMKfZ3Gwbr134cYoqRtuE8rnYo4Oyxnm+NKNCMEAwHQYDVR0OBBYEFA/F83obUYIYLHnnXI5fObo/EMDIMB8GA1UdIwQYMBaAFOv2T4eG9aq4dDj8dGqF6Vj340LwMAkGByqGSM49BAEDRwAwRAIgIQrtZLIEZ6IXxPleal0AnnGE0wgq2MHzaWHAOm72KIgCIAWbYPKedEjKKcxv5vHuvbjAdUp1JhrnvnTCEWM7Zus7"));
            Assert.assertThat(module.getModuleSha256(), Matchers.is("c0316c6dcb60f61bd9dc511d36d72963c9fb15a246c4e050e322ce8098f38609"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.is("com.l7tech.DynamicCustomAssertion.sjar"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.is("DynamicCustomAssertionCustomAssertion"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_SIZE), Matchers.is("3437"));
        }

        // test modular assertion
        try (final InputStream is = getResourceStream(MODULAR_ASSERTION_SAAR)) {
            final ServerModuleFile module = GenerateServerModuleFileCommand.loadServerModuleFileFromSignedZip(is, "com.l7tech.XmlSecurityModularAssertion.saar");
            Assert.assertNotNull(module);
            Assert.assertThat(module.getModuleType(), Matchers.is(ModuleType.MODULAR_ASSERTION));
            Assert.assertNotNull(module.getData());
            Assert.assertNotNull(module.getData().getDataBytes());
            Assert.assertThat(module.getData().getDataBytes().length, Matchers.is(130096));
            Assert.assertNotNull(module.getData().getSignatureProperties());
            Assert.assertThat(module.getData().getSignatureProperties(), Matchers.equalToIgnoringWhiteSpace("#Signature\n" +
                    "#Mon Oct 19 15:59:12 PDT 2015\n" +
                    "signature=MEUCIDNAu8OxG1/io/GWslmlr8aKzc2ZN3+FHGfJD6yAqWFHAiEA657jFu3LWmmy+0YfwEX01odchoBu7XbutwMA58TBUL0\\=\n" +
                    "cert=MIIBdjCCAR6gAwIBAgIJAOC+DREEgh+1MAkGByqGSM49BAEwGzEZMBcGA1UEAxMQcm9vdC5hcGltLmNhLmNvbTAeFw0xNTA4MDYyMzM5MTVaFw0zNTA4MDYyMzM5MTVaMCUxIzAhBgNVBAMTGnNpZ25lci5nYXRld2F5LmFwaW0uY2EuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWR6rsgeDYY4nCkqjy12QBZW/U/dkWbM0NSEJnWwwDeKMD8rKgPygMKfZ3Gwbr134cYoqRtuE8rnYo4Oyxnm+NKNCMEAwHQYDVR0OBBYEFA/F83obUYIYLHnnXI5fObo/EMDIMB8GA1UdIwQYMBaAFOv2T4eG9aq4dDj8dGqF6Vj340LwMAkGByqGSM49BAEDRwAwRAIgIQrtZLIEZ6IXxPleal0AnnGE0wgq2MHzaWHAOm72KIgCIAWbYPKedEjKKcxv5vHuvbjAdUp1JhrnvnTCEWM7Zus7"));
            Assert.assertThat(module.getModuleSha256(), Matchers.is("7ad42f77e4527e7dafa99410d7c403e421c63fd8b9f03adb8fb06128690162b9"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.is("com.l7tech.XmlSecurityModularAssertion.saar"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalToIgnoringWhiteSpace("IndexLookupByItemAssertion,ItemLookupByIndexAssertion,NonSoapCheckVerifyResultsAssertion,NonSoapDecryptElementAssertion,NonSoapEncryptElementAssertion,NonSoapSignElementAssertion,NonSoapVerifyElementAssertion,SelectElementAssertion,VariableCredentialSourceAssertion"));
            Assert.assertThat(module.getProperty(ServerModuleFile.PROP_SIZE), Matchers.is("130096"));
        }
    }

    private InputStream getResourceStream(final String filePath) throws Exception {
        Assert.assertThat(filePath, Matchers.not(Matchers.isEmptyOrNullString()));
        final URL url = GenerateServerModuleFileCommandTest.class.getClassLoader().getResource(filePath);
        Assert.assertNotNull(url);
        final InputStream is = url.openStream();
        Assert.assertNotNull(is);
        return is;
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertThat(command.getName(), Matchers.equalTo("genSmfXml"));
    }
}