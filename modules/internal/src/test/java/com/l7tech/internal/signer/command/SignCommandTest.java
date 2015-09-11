package com.l7tech.internal.signer.command;

import com.l7tech.util.SyspropUtil;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class SignCommandTest {
    private File tmpDir;
    private SignCommand command;

    @Before
    public void setUp() throws Exception {
        tmpDir = new File(SyspropUtil.getProperty("java.io.tmpdir"));
        command = new SignCommand();
    }

    @Test
    public void testGenerateOutFileName() throws Exception {
        // test skar file as input, case ignored
        doTestOutFileName("test.skar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "skar");
        doTestOutFileName("test.SKAR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "SKAR");
        doTestOutFileName("test.Skar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "Skar");
        doTestOutFileName("test.SKaR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "SKaR");
        doTestOutFileName("test.sKar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "sKar");

        // test AAR file as input, case ignored
        doTestOutFileName("test.aar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "aar");
        doTestOutFileName("test.AAR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "AAR");
        doTestOutFileName("test.Aar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "Aar");
        doTestOutFileName("test.AaR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "AaR");
        doTestOutFileName("test.aAr", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "aAr");

        // test JAR file as input, case ignored
        doTestOutFileName("test.jar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "jar");
        doTestOutFileName("test.JAR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "JAR");
        doTestOutFileName("test.Jar", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "Jar");
        doTestOutFileName("test.JaR", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "JaR");
        doTestOutFileName("test.jAr", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "jAr");

        // test anything else with extension
        doTestOutFileName("test.blah", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "blah");
        doTestOutFileName("test.BLAH", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "BLAH");
        doTestOutFileName("test.1", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "1");
        doTestOutFileName("test.2", "test" + SignCommand.DEFAULT_SIGNED_PREFIX + "2");

        // test without extension
        doTestOutFileName("test.", "test" + SignCommand.DEFAULT_SIGNED_EXT);
        doTestOutFileName("test", "test" + SignCommand.DEFAULT_SIGNED_EXT);
    }

    private void doTestOutFileName(final String inFileName, final String expectedOutFileName) throws Exception {
        Assert.assertThat(inFileName, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(expectedOutFileName, Matchers.not(Matchers.isEmptyOrNullString()));

        final File outFile = command.generateOutFileName(new File(tmpDir, inFileName));
        Assert.assertNotNull(outFile);
        Assert.assertThat(outFile.getCanonicalPath(), Matchers.equalTo(new File(tmpDir, expectedOutFileName).getCanonicalPath()));
    }
}