package com.l7tech.internal.signer.command;

import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileUtils;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.SyspropUtil;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
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
     * TODO: Important make sure these values are matching both {@link SignerUtils#SIGNING_CERT_PROPS} and {@link SignerUtils#SIGNATURE_PROP}
     */
    private static final String SIGNING_CERT_PROPS = "cert";
    private static final String SIGNATURE_PROP = "signature";

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

    @Test
    public void testGetServerModuleFileUtils() throws Exception {
        final ServerModuleFileUtils utils = command.getServerModuleFileUtils();
        Assert.assertNotNull(utils);
        Assert.assertThat(command.getServerModuleFileUtils(), Matchers.sameInstance(utils));
        Assert.assertThat(command.getServerModuleFileUtils(), Matchers.sameInstance(utils));
        Assert.assertThat(command.getServerModuleFileUtils(), Matchers.sameInstance(utils));
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertThat(command.getName(), Matchers.equalTo("genSmfXml"));
    }
}