package com.l7tech.internal.signer.command;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.module.*;
import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.internal.signer.LazyFileOutputStream;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.transform.dom.DOMResult;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static com.l7tech.internal.signer.SignerErrorCodes.*;

/**
 * Generates an XML out of signed ServerModuleFile.
 */
public class GenerateServerModuleFileCommand extends Command {
    private static final String COMMAND_NAME = "genSmfXml";
    private static final String COMMAND_DESC = "Generates an XML file (RESTMAN entity xml) out of input signed ServerModuleFile";

    protected static final String DEFAULT_XML_EXT = ".xml";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final Option nameOpt = new OptionBuilder()
            .opt("n")
            .longOpt("name")
            .argName("module name")
            .desc("Name of the Server Module .aar or .jar file")
            .required(true)
            .build();
    private static final Option smfFileOpt = new OptionBuilder()
            .opt("f")
            .longOpt("smfFile")
            .argName("input Server Module File")
            .desc("Name of signed input Server Module File")
            .required(true)
            .build();
    private static final Option outFileOpt = new OptionBuilder()
            .opt("o")
            .longOpt("outFile")
            .argName("output file")
            .desc("(Optional) Name of output xml file; if omitted, then output name adds '.xml' to the input name")
            .optional(true)
            .build();
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // read sign arguments
    private File smfFile;
    private String moduleName;
    private File outFile;
    
    @Override
    protected Options getOptions() {
        final Options options = getStandardOptions();
        options.addOption(nameOpt);
        options.addOption(smfFileOpt);
        options.addOption(outFileOpt);
        return options;
    }


    @Override
    protected int getOptionOrdinal(@NotNull final Option opt) {
        // start with parent order
        final int ord = super.getOptionOrdinal(opt);
        if (opt == nameOpt) {
            return ord + 1;
        }
        if (opt == smfFileOpt) {
            return ord + 2;
        }
        if (opt == outFileOpt) {
            return ord + 3;
        }

        // shouldn't happen
        return ord;
    }

    @Override
    protected int init() {
        // shouldn't happen but just in case
        if (!hasOption(nameOpt) || !hasOption(smfFileOpt)) {
            throw new IllegalArgumentException("Missing required options: " + nameOpt.getLongOpt() + ", " + smfFileOpt.getLongOpt());
        }

        moduleName = getOptionValue(nameOpt);
        if (StringUtils.isBlank(moduleName)) {
            throw new IllegalArgumentException("Specified name is blank");
        }

        smfFile = new File(getOptionValue(smfFileOpt));
        if (smfFile.isDirectory() || !smfFile.exists()) {
            throw new IllegalArgumentException("Specified smfFile is not a valid file");
        }

        try {
            outFile = (hasOption(outFileOpt) && StringUtils.isNotBlank(getOptionValue(outFileOpt)))
                    ? new File(getOptionValue(outFileOpt))
                    : generateOutFileName(smfFile);
            if (outFile.isDirectory()) {
                throw new IllegalArgumentException("Specified output file cannot be a directory");
            }
            if (outFile.exists() && outFile.getCanonicalPath().equals(smfFile.getCanonicalPath())) {
                throw new IllegalArgumentException("Specified output file must be different then input file");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return SUCCESS;
    }

    @NotNull
    protected File generateOutFileName(@NotNull final File inFileName) throws IOException {
        // get the canonical path
        final String inFilePath = inFileName.getCanonicalPath();
        // extract extension
        final String inFileExt = FilenameUtils.getExtension(inFilePath);
        // if the file has extension
        if (StringUtils.isNotBlank(inFileExt)) {
            return new File(inFilePath + DEFAULT_XML_EXT);
        }
        // otherwise simply append DEFAULT_SIGNED_EXT
        return new File(FilenameUtils.removeExtension(inFilePath) + DEFAULT_XML_EXT);
    }

    @Override
    protected void run() throws CommandException {
        // sign file
        try (
                final InputStream is = new BufferedInputStream(new FileInputStream(smfFile));
                final OutputStream os = new BufferedOutputStream(new LazyFileOutputStream(outFile))
        ) {
            // generate the xml
            generateXmlFromServerModuleFile(is, os);

            System.out.println();
            System.out.println("XML File successfully generated.");
            System.out.println("XML File: " + outFile.getCanonicalPath());
        } catch (final IOException e) {
            throw new CommandException(IO_ERROR_WHILE_RUNNING, e);
        } catch (final Exception e) {
            throw new CommandException(ERROR_GENERATION_SMF_XML, e);
        }
    }

    /**
     * This is a very very hacky way of reading a signed zip file, however I rather have this hack than making
     * {@link SignerUtils.SignedZip#readSignedZip(java.io.InputStream, com.l7tech.gateway.common.security.signer.InnerPayloadFactory)}
     * with public access.
     *
     * @param signedZipStream    signed zip {@code InputStream}.  Required and cannot be {@code null}.
     * @param fileName           module filename.  Required and cannot be {@code null}.
     * @return {@link ServerModuleFile} contained within the signed zip payload.
     */
    @NotNull
    static ServerModuleFile loadServerModuleFileFromSignedZip(
            @NotNull final InputStream signedZipStream,
            @NotNull final String fileName
    ) throws IOException {
        try {
            final Method method = SignerUtils.class.getDeclaredMethod("readSignedZip", InputStream.class, InnerPayloadFactory.class);
            if (method == null) {
                throw new RuntimeException("method readSignedZip either missing from class SignerUtils or its deceleration has been modified");
            }
            method.setAccessible(true);
            final Object ret = method.invoke(
                    null,
                    signedZipStream,
                    new ServerModuleFilePayloadFactory(
                            new CustomAssertionsScannerHelper("custom_assertions.properties"),
                            new ModularAssertionsScannerHelper("ModularAssertion-List"),
                            fileName
                    )
            );
            if (!(ret instanceof ServerModuleFilePayload)) {
                throw new RuntimeException("method readSignedZip return unexpected payload");
            }
            try (final ServerModuleFilePayload payload = (ServerModuleFilePayload)ret) {
                // we cannot check whether issuer is trusted, but we can validate signature here
                SignerUtils.verifySignature(payload.getDataStream(), payload.getSignaturePropertiesString());
                return payload.create();
            } catch (final IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(MessageFormat.format("Failed to verify signature for file: '{0}'.\nCause: {1}", ServerModuleFilePayload.trimFileName(fileName), ExceptionUtils.getMessage(e)), e);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(MessageFormat.format("Failed to read signed zip '{0}' content.\nCause: {1}", ServerModuleFilePayload.trimFileName(fileName), ExceptionUtils.getMessage(e)), e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            throw new RuntimeException(MessageFormat.format("Failed to process signed zip '{0}' content.\nCause: {1}", ServerModuleFilePayload.trimFileName(fileName), ExceptionUtils.getMessage(e)), e);
        }
    }

    /**
     * Execute Command Task.
     * Generate RESTMAN entity XML from {@code smfStream} into {@code outputStream}.
     *
     * @param smfStream       signed module file stream.  Required and cannot be {@code null}.
     * @param outputStream    RESTMAN entity XML output file stream.  Required and cannot be {@code null}.
     * @throws Exception
     */
    private void generateXmlFromServerModuleFile(
            @NotNull final InputStream smfStream,
            @NotNull final OutputStream outputStream
    ) throws Exception {
        // create a ServerModuleFile from the stream.
        final ServerModuleFile moduleFile = loadServerModuleFileFromSignedZip(smfStream, smfFile.getName());
        moduleFile.setName(moduleName);

        // create ServerModuleFile managed object
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setName(moduleFile.getName());
        moduleFileMO.setModuleType(convertModuleType(moduleFile.getModuleType()));
        moduleFileMO.setModuleSha256(moduleFile.getModuleSha256());
        moduleFileMO.setProperties(gatherProperties(moduleFile));
        moduleFileMO.setModuleData(moduleFile.getData().getDataBytes());
        moduleFileMO.setSignatureProperties(gatherSignatureProperties(moduleFile.getData().getSignatureProperties()));

        // marshal ServerModuleFile mo
        final DOMResult result = new DOMResult();
        MarshallingUtils.marshal(moduleFileMO, result, false);

        // save the resulting node into output stream
        XmlUtil.nodeToFormattedOutputStream(result.getNode(), outputStream);
    }

    // TODO: At some point we should move the following 3 methods (i.e. convertModuleType, gatherProperties and gatherSignatureProperties) in a shared utility class,
    // TODO: accessible by both gateway-api (ServerModuleFileMO), gateway-common (ServerModuleFile) and GatewayManagementAssertion (ServerModuleFileTransformer)

    /**
     * Utility method for converting between {@link ServerModuleFile} and {@link ServerModuleFileMO} module type.
     *
     * @param moduleType    Input {@link com.l7tech.gateway.common.module.ModuleType} for converting.  Required and cannot be {@code null}
     * @return A {@link com.l7tech.gateway.api.ServerModuleFileMO.ServerModuleFileModuleType ServerModuleFileModuleType} from the specified {@code moduleType}, never {@code null}.
     * @throws IOException if the specified {@code moduleType} is not recognized.
     */
    @NotNull
    public static ServerModuleFileMO.ServerModuleFileModuleType convertModuleType(@NotNull final ModuleType moduleType) throws IOException {
        switch (moduleType) {
            case MODULAR_ASSERTION:
                return ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION;
            case CUSTOM_ASSERTION:
                return ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION;
            default:
                throw new IOException("Unknown Module Type '" + moduleType + "'.");
        }
    }

    /**
     * Utility method for converting {@link com.l7tech.gateway.common.module.ServerModuleFile#getXmlProperties()} entity properties}
     * into {@link ServerModuleFileMO#properties MO properties}.
     *
     * @param moduleFile    the {@code ServerModuleFile} entity to gather properties from.  Required and cannot be {@code null}.
     * @return read-only {@code Map} of all specified {@code moduleFile} properties, or {@code null} if the specified
     * {@code moduleFile} does not contain any known property keys.
     */
    @Nullable
    public static Map<String, String> gatherProperties(@NotNull final ServerModuleFile moduleFile) {
        final Map<String, String> props = new TreeMap<>();
        for (final String key : ServerModuleFile.getPropertyKeys()) {
            final String value = moduleFile.getProperty(key);
            if (value != null) {
                props.put(key, value);
            }
        }
        return props.isEmpty() ? null : Collections.unmodifiableMap(props);
    }


    /**
     * Convenient method for gathering signature properties into a {@code Map} used by {@code ServerModuleFileMO}.
     *
     * @param signatureProperties    A {@code String} holding module signature Properties.  Optional and can be {@code null}.
     * @return read-only {@code Map} of signature properties from the specified {@code signatureProperties},
     * or {@code null} if the specified {@code signatureProperties} is blank or it doesn't contain any known property keys.
     * @throws IOException if an IO error happens while reading from the reader.
     */
    @Nullable
    public static Map<String, String> gatherSignatureProperties(@Nullable final String signatureProperties) throws IOException {
        if (StringUtils.isBlank(signatureProperties)) {
            return null;
        }
        assert signatureProperties != null; // intellij intellisense doesn't know how isBlank works
        // read the signature information
        try (final StringReader reader = new StringReader(signatureProperties)) {
            final Properties props = new Properties();
            props.load(reader);

            final Map<String, String> ret = new TreeMap<>();
            for (final String key : SignerUtils.ALL_SIGNING_PROPERTIES) {
                final String value = (String) props.get(key);
                if (value != null) {
                    ret.put(key, value);
                }
            }
            return ret.isEmpty() ? null : Collections.unmodifiableMap(ret);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public String getDesc() {
        return COMMAND_DESC;
    }

}
