package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;

/**
 * Copies
 * @author jbufu
 */
public class ResourcesPatchTask implements PatchTask {
    @Override
    public void runPatch(String resourceDirEntry)
            throws IOException, PatchException {
        System.out.println("Executing resource extraction patch task");

        String fileNames = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);
        String[] files = fileNames.split("\n");

        File tempDir = new File( ConfigFactory.getProperty( PatchMain.RESOURCE_TEMP_PROPERTY ) );
        InputStream fileIn = null;
        OutputStream fileOut = null;
        OutputStream nullOutputSteam = new NullOutputStream();

        // first make sure we have enough free space to extract the resources from the patch
        System.out.println("Getting file sizes");
        long totalResourceSize = 0;
        for (String file : files) {
            try {
                fileIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + file);
                totalResourceSize += IOUtils.copyStream(fileIn, nullOutputSteam);
            } finally {
                ResourceUtils.closeQuietly(fileIn);
                ResourceUtils.closeQuietly(nullOutputSteam);
            }
        }
        if(tempDir.getUsableSpace() < totalResourceSize) {
            throw new PatchException("Insufficient free space to extract resources in patch. There are " +
                tempDir.getUsableSpace() + " bytes available but " + totalResourceSize + " bytes are required.");
        }

        // now actually extract the resource files to disk
        System.out.println("Extracting resources to " + tempDir.getAbsolutePath());
        for (String file : files) {
            try {
                System.out.println("Extracting " + file);
                fileIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + file);
                File outFile = new File(tempDir, file);
                outFile.deleteOnExit();
                fileOut = new FileOutputStream(outFile);
                IOUtils.copyStream(fileIn, fileOut);
            } finally {
                ResourceUtils.closeQuietly(fileIn);
                ResourceUtils.closeQuietly(fileOut);
            }
        }

        System.out.println("Done executing resource extraction patch task");
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.common.io.NullOutputStream",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.server.processcontroller.patching.PatchException",
            "com.l7tech.util.ArrayUtils",
            "com.l7tech.util.ArrayUtils$1",
            "com.l7tech.util.ArrayUtils$1$1",
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
            "com.l7tech.util.ConfigFactory$ConfigFactoryHolder",
            "com.l7tech.util.ConfigFactory$ConfigProviderSpi",
            "com.l7tech.util.ConfigFactory$ConfigurationListener",
            "com.l7tech.util.ConfigFactory$DefaultConfig",
            "com.l7tech.util.ConfigFactory$DefaultConfig$1",
            "com.l7tech.util.ConfigFactory$DefaultConfig$2",
            "com.l7tech.util.ConfigFactory$DefaultConfig$3",
            "com.l7tech.util.ConfigFactory$DefaultConfig$4",
            "com.l7tech.util.ConfigFactory$DefaultConfig$5",
            "com.l7tech.util.ConfigFactory$DefaultConfig$6",
            "com.l7tech.util.ConfigFactory$DefaultConfig$ValidatorFactory",
            "com.l7tech.util.ConfigFactory$DefaultConfig$ValidatorHolder",
            "com.l7tech.util.ConfigFactory$DefaultConfigProviderSpi",
            "com.l7tech.util.ConfigFactory$DelegatingConfigurationListener",
            "com.l7tech.util.ConfigFactory$SmartConfigurationListener",
            "com.l7tech.util.ConfigFactory$SmartConfigurationListenerSupport",
            "com.l7tech.util.ConversionUtils",
            "com.l7tech.util.ConversionUtils$1",
            "com.l7tech.util.ConversionUtils$2",
            "com.l7tech.util.ConversionUtils$3",
            "com.l7tech.util.ConversionUtils$4",
            "com.l7tech.util.ConversionUtils$Converter",
            "com.l7tech.util.ConversionUtils$TextConverter",
            "com.l7tech.util.Disposable",
            "com.l7tech.util.Either",
            "com.l7tech.util.EnumTranslator",
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
            "com.l7tech.util.Functions$Nullary",
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
            "com.l7tech.util.Functions$Unary",
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
            "com.l7tech.util.TimeUnit",
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
