package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;

/**
 * @author jbufu
 */
public class ShellScriptPatchTask implements PatchTask {
    @Override
    public void runPatch(String resourceDirEntry)
            throws IOException, InterruptedException, PatchException {
        System.out.println("Executing shell script patch task");

        System.out.println("Extracting shell script to run");
        String scriptName = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);

        File resourceTempDir = new File( ConfigFactory.getProperty( PatchMain.RESOURCE_TEMP_PROPERTY ) );
        File tempFile = File.createTempFile("patch_sh_script", "", resourceTempDir);
        tempFile.deleteOnExit();
        InputStream scriptIn = null;
        OutputStream tempOut = null;
        try {
            scriptIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + scriptName);
            tempOut = new FileOutputStream(tempFile);
            IOUtils.copyStream(scriptIn, tempOut);
        }
        finally {
            ResourceUtils.closeQuietly(scriptIn);
            ResourceUtils.closeQuietly(tempOut);
        }
        System.out.println("Done extracting shell script to run");

        System.out.println("Running shell script");
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", tempFile.getAbsolutePath(), resourceTempDir.getAbsolutePath());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        InputStream processStdOut = process.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(processStdOut);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            System.out.flush();
        }
        int processExitCode = process.waitFor();
        if(processExitCode != 0) {
            throw new PatchException("Error executing patch task: non-zero exit code returned from patch install script (returned " + processExitCode + ")");
        }
        System.out.println("Patch returned exit code " + processExitCode);
        ResourceUtils.closeQuietly(processStdOut);
        System.out.println("Done running shell script");

        System.out.println("Done executing shell script patch task");
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.server.processcontroller.patching.PatchException",
            "com.l7tech.util.ArrayUtils",
            "com.l7tech.util.ArrayUtils$1",
            "com.l7tech.util.ArrayUtils$1$1",
            "com.l7tech.util.BufferPool",
            "com.l7tech.util.CausedIOException",
            "com.l7tech.util.Charsets",
            "com.l7tech.util.CollectionUtils",
            "com.l7tech.util.CollectionUtils$1",
            "com.l7tech.util.CollectionUtils$2",
            "com.l7tech.util.CollectionUtils$2$1",
            "com.l7tech.util.CollectionUtils$MapBuilder",
            "com.l7tech.util.Config",
            "com.l7tech.util.ConfigFactory",
            "com.l7tech.util.ConfigFactory$1",
            "com.l7tech.util.ConfigFactory$10",
            "com.l7tech.util.ConfigFactory$2",
            "com.l7tech.util.ConfigFactory$3",
            "com.l7tech.util.ConfigFactory$4",
            "com.l7tech.util.ConfigFactory$5",
            "com.l7tech.util.ConfigFactory$6",
            "com.l7tech.util.ConfigFactory$7",
            "com.l7tech.util.ConfigFactory$8",
            "com.l7tech.util.ConfigFactory$9",
            "com.l7tech.util.ConfigFactory$DefaultConfig$1",
            "com.l7tech.util.ConfigFactory$DefaultConfig$2",
            "com.l7tech.util.ConfigFactory$DefaultConfig$3",
            "com.l7tech.util.ConfigFactory$DefaultConfig$4",
            "com.l7tech.util.ConfigFactory$DefaultConfig$5",
            "com.l7tech.util.ConfigFactory$DefaultConfig$6",
            "com.l7tech.util.ConfigFactory$DefaultConfig$ValidatorFactory",
            "com.l7tech.util.ConfigFactory$DefaultConfig$ValidatorHolder",
            "com.l7tech.util.ConversionUtils",
            "com.l7tech.util.ConversionUtils$1",
            "com.l7tech.util.ConversionUtils$2",
            "com.l7tech.util.ConversionUtils$3",
            "com.l7tech.util.ConversionUtils$4",
            "com.l7tech.util.ConversionUtils$Converter",
            "com.l7tech.util.ConversionUtils$TextConverter",
            "com.l7tech.util.Disposable",
            "com.l7tech.util.Either",
            "com.l7tech.util.FileUtils",
            "com.l7tech.util.FileUtils$ByteSaver",
            "com.l7tech.util.FileUtils$LastModifiedFileInputStream",
            "com.l7tech.util.FileUtils$Saver",
            "com.l7tech.util.Functions",
            "com.l7tech.util.Functions$1",
            "com.l7tech.util.Functions$10",
            "com.l7tech.util.Functions$11",
            "com.l7tech.util.Functions$12",
            "com.l7tech.util.Functions$13",
            "com.l7tech.util.Functions$14",
            "com.l7tech.util.Functions$15",
            "com.l7tech.util.Functions$16",
            "com.l7tech.util.Functions$17",
            "com.l7tech.util.Functions$18",
            "com.l7tech.util.Functions$19",
            "com.l7tech.util.Functions$2",
            "com.l7tech.util.Functions$20",
            "com.l7tech.util.Functions$21",
            "com.l7tech.util.Functions$22",
            "com.l7tech.util.Functions$23",
            "com.l7tech.util.Functions$24",
            "com.l7tech.util.Functions$3",
            "com.l7tech.util.Functions$4",
            "com.l7tech.util.Functions$5",
            "com.l7tech.util.Functions$6",
            "com.l7tech.util.Functions$7",
            "com.l7tech.util.Functions$8",
            "com.l7tech.util.Functions$9",
            "com.l7tech.util.Functions$Binary",
            "com.l7tech.util.Functions$BinaryThrows",
            "com.l7tech.util.Functions$BinaryVoid",
            "com.l7tech.util.Functions$BinaryVoidThrows",
            "com.l7tech.util.Functions$NullaryThrows",
            "com.l7tech.util.Functions$NullaryVoidThrows",
            "com.l7tech.util.Functions$Quaternary",
            "com.l7tech.util.Functions$QuaternaryVoid",
            "com.l7tech.util.Functions$Quinary",
            "com.l7tech.util.Functions$QuinaryVoid",
            "com.l7tech.util.Functions$Sestary",
            "com.l7tech.util.Functions$SestaryVoid",
            "com.l7tech.util.Functions$Ternary",
            "com.l7tech.util.Functions$TernaryThrows",
            "com.l7tech.util.Functions$TernaryVoid",
            "com.l7tech.util.Functions$TernaryVoidThrows",
            "com.l7tech.util.Functions$UnaryThrows",
            "com.l7tech.util.Functions$UnaryVoid",
            "com.l7tech.util.Functions$UnaryVoidThrows",
            "com.l7tech.util.Functions$Variadic",
            "com.l7tech.util.Functions$VariadicVoid",
            "com.l7tech.util.HexUtils",
            "com.l7tech.util.IOUtils$1$1",
            "com.l7tech.util.InetAddressUtil",
            "com.l7tech.util.JdkLoggerConfigurator",
            "com.l7tech.util.JdkLoggerConfigurator$1",
            "com.l7tech.util.JdkLoggerConfigurator$Probe",
            "com.l7tech.util.JdkLoggerConfigurator$ResettableLogManager",
            "com.l7tech.util.Option",
            "com.l7tech.util.Option$1",
            "com.l7tech.util.Option$2",
            "com.l7tech.util.Pair",
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.util.SyspropUtil",
            "com.l7tech.util.SyspropUtil$1",
            "com.l7tech.util.SyspropUtil$2",
            "com.l7tech.util.SyspropUtil$3",
            "com.l7tech.util.TextUtils",
            "com.l7tech.util.TextUtils$1",
            "com.l7tech.util.TextUtils$2",
            "com.l7tech.util.TextUtils$3",
            "com.l7tech.util.TextUtils$4",
            "com.l7tech.util.TextUtils$5",
            "com.l7tech.util.TextUtils$6",
            "com.l7tech.util.TextUtils$7",
            "com.l7tech.util.TextUtils$8",
            "com.l7tech.util.TimeSource",
            "com.l7tech.util.TimeUnit$1",
            "com.l7tech.util.ValidationUtils",
            "com.l7tech.util.ValidationUtils$1",
            "com.l7tech.util.ValidationUtils$2",
            "com.l7tech.util.ValidationUtils$PredicatedValidator",
            "com.l7tech.util.ValidationUtils$ValidationPredicate",
            "com.l7tech.util.ValidationUtils$Validator"
        };
    }

}
