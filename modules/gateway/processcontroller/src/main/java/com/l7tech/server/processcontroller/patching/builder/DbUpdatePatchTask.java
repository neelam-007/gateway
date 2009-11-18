package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.server.util.PropertiesDecryptor;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;

import java.util.Properties;
import java.text.MessageFormat;
import java.io.*;

/**
 * @author jbufu
 */
public class DbUpdatePatchTask implements PatchTask {

    public static final String NODE_PROPERTIES_FORMAT = "/opt/SecureSpan/Gateway/node/{0}/etc/conf/node.properties";
    public static final String MASTER_PASSWORD_FORMAT = "/opt/SecureSpan/Gateway/node/{0}/etc/conf/omp.dat";

    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String[] nodeDbUpdate = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE).split("\n");
        String nodeName = nodeDbUpdate[0];
        String dbUpdateScript = nodeDbUpdate[1];

        File masterPasswordFile = new File(MessageFormat.format(MASTER_PASSWORD_FORMAT, nodeName));
        Properties nodeProperties =  new Properties();
        nodeProperties.load(new FileInputStream(MessageFormat.format(NODE_PROPERTIES_FORMAT, nodeName)));
        new PropertiesDecryptor(new MasterPasswordManager(new DefaultMasterPasswordFinder(masterPasswordFile))).decryptEncryptedPasswords(nodeProperties);

        StringBuilder commandLine = new StringBuilder();
        commandLine.append("/usr/bin/mysql");
        commandLine.append(" -h ").append(nodeProperties.get("node.db.config.main.host"));
        commandLine.append(" -P ").append(nodeProperties.get("node.db.config.main.port"));
        commandLine.append(" -u ").append(nodeProperties.get("node.db.config.main.user"));
        commandLine.append(" -p").append(nodeProperties.get("node.db.config.main.pass"));
        commandLine.append(" ").append(nodeProperties.get("node.db.config.main.name"));

        ProcResult result = ProcUtils.exec(commandLine.toString(), PatchMain.getResourceStream(this.getClass(), resourceDirEntry + dbUpdateScript));
        if(result.getExitStatus() != 0) {
            byte[] output = result.getOutput();
            throw new PatchException("Error applying database update: " + (output == null ? "" : new String(output) ));
        }
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.util.FileUtils",
            "com.l7tech.util.FileUtils$Saver",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.common.io.ProcUtils$1",
            "com.l7tech.common.io.ProcUtils$ByteArrayHolder",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.server.processcontroller.patching.PatchException",
            "com.l7tech.server.util.PropertiesDecryptor",
            "com.l7tech.util.MasterPasswordManager",
            "com.l7tech.util.DefaultMasterPasswordFinder",
            "com.l7tech.util.MasterPasswordManager$MasterPasswordFinder",
            "com.l7tech.util.HexUtils",
            "com.l7tech.util.CausedIOException",
            "org.apache.commons.codec.binary.Base64",
            "org.apache.commons.codec.binary.Hex",
            "org.apache.commons.codec.BinaryDecoder",
            "org.apache.commons.codec.BinaryEncoder",
            "org.apache.commons.codec.Decoder",
            "org.apache.commons.codec.DecoderException",
            "org.apache.commons.codec.Encoder",
            "org.apache.commons.codec.EncoderException",
            "org.apache.commons.codec.StringDecoder",
            "org.apache.commons.codec.StringEncoder",
        };
    }
}
