package com.l7tech.internal.signer.command;

import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.internal.signer.LazyFileOutputStream;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import static com.l7tech.internal.signer.SignerErrorCodes.*;

/**
 * Command for signing arbitrary file (a Server Module {@code .aar} or {@code .jar} file and/or Solution Kit Archive a.k.a {@code .skar} file).
 */
public class SignCommand extends Command {
    static final String COMMAND_NAME = "sign";
    static final String COMMAND_DESC = "Sign arbitrary file (a Server Module .aar or .jar file or Solution Kit Archive a.k.a .skar file)";
    static final String PLAINTEXT_PASSWORD_WARNING = "NOTE: Use of plaintext passwords is not recommended. Use encoded form for maximum security.";

    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_SIGNED_EXT = ".signed";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final Option storeFileOpt = new OptionBuilder()
            .opt("f")
            .longOpt("storeFile")
            .argName("signer keystore file")
            .desc("Name of keystore file (typically PKCS #12), which holds the signer certificate key pair")
            .required(true)
            .build();
    private static final Option storeTypeOpt = new OptionBuilder()
            .opt("t")
            .longOpt("storeType")
            .argName("keystore type")
            .desc("(Optional) The keystore type if not PKCS #12")
            .optional(true)
            .build();
    private static final Option encStorePassOpt = new OptionBuilder()
            .opt("p")
            .longOpt("storePass")
            .argName("encoded keystore password")
            .desc("Encoded password to access the keystore; use prefix '@file:' to read the password from a file")
            .build();
    private static final Option plaintextStorePassOpt = new OptionBuilder()
            .longOpt("plaintextStorePass")
            .argName("plaintext keystore password")
            .desc("Plaintext password to access the keystore; add prefix '@file:' to read the password from a file. NOTE: Plaintext passwords should be avoided.")
            .build();
    private static final Option keyAliasOpt = new OptionBuilder()
            .opt("a")
            .longOpt("alias")
            .argName("key entry alias")
            .desc("Alias name of the keystore entry to use for signing; optional if keystore contains a single key entry")
            .optional(true)
            .build();
    private static final Option encKeyPassOpt = new OptionBuilder()
            .opt("k")
            .longOpt("keyPass")
            .argName("encoded key entry password")
            .desc("Encoded password to load the keystore entry; add prefix '@file:' to read the password from a file.")
            .optional(true)
            .build();
    private static final Option plaintextKeyPassOpt = new OptionBuilder()
            .longOpt("plaintextKeyPass")
            .argName("plaintext key entry password")
            .desc("Plaintext password to load the keystore entry; add prefix '@file:' to read the password from a file. NOTE: Plaintext passwords should be avoided.")
            .optional(true)
            .build();
    private static final Option fileToSignOpt = new OptionBuilder()
            .opt("s")
            .longOpt("fileToSign")
            .argName("input file")
            .desc("Name of input file to sign")
            .required(true)
            .build();
    private static final Option outFileOpt = new OptionBuilder()
            .opt("o")
            .longOpt("outFile")
            .argName("output file")
            .desc("(Optional) Name of signed output file; if omitted, then output name adds '.signed' to the input name")
            .optional(true)
            .build();
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // read sign arguments
    private File storeFile;
    private String storeType;
    private String storePass;
    private String keyAlias;
    private String keyPass;
    private File fileToSign;
    private File outFile;

    @Override
    protected int init() {
        // shouldn't happen but just in case
        if (!hasOption(storeFileOpt) || !hasOption(fileToSignOpt)) {
            throw new IllegalArgumentException("Missing required options: " + storeFileOpt.getLongOpt() + ", " + fileToSignOpt.getLongOpt());
        }

        storeFile = new File(getOptionValue(storeFileOpt));
        storeType = getOptionValue(storeTypeOpt, DEFAULT_KEYSTORE_TYPE);
        storePass = getPassword(encStorePassOpt, plaintextStorePassOpt);
        keyAlias = getOptionValue(keyAliasOpt);
        keyPass = getPassword(encKeyPassOpt, plaintextKeyPassOpt);

        // verify store file
        if (storeFile.isDirectory() || !storeFile.exists()) {
            throw new IllegalArgumentException("Argument '" + storeFileOpt.getLongOpt() + "' is not a valid file");
        }

        // verify storePass
        if (DEFAULT_KEYSTORE_TYPE.equals(storeType) && StringUtils.isBlank(storePass)) {
            throw new IllegalArgumentException("Argument '" + encStorePassOpt.getLongOpt() + "' is required for PKCS #12");
        }

        fileToSign = new File(getOptionValue(fileToSignOpt));
        if (fileToSign.isDirectory() || !fileToSign.exists()) {
            throw new IllegalArgumentException("Specified fileToSign is not a valid file");
        }

        try {
            outFile = (hasOption(outFileOpt) && StringUtils.isNotBlank(getOptionValue(outFileOpt)))
                    ? new File(getOptionValue(outFileOpt))
                    : new File(fileToSign.getCanonicalPath() + DEFAULT_SIGNED_EXT);
            if (outFile.isDirectory()) {
                throw new IllegalArgumentException("Specified output file cannot be a directory");
            }
            if (outFile.exists() && outFile.getCanonicalPath().equals(fileToSign.getCanonicalPath())) {
                throw new IllegalArgumentException("Specified output file must be different then input file");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        // warn if plaintext password was used
        if ( (!hasOption(encStorePassOpt) && hasOption(plaintextStorePassOpt)) || (!hasOption(encKeyPassOpt) && hasOption(plaintextKeyPassOpt)) ) {
            err.println(PLAINTEXT_PASSWORD_WARNING);
        }

        return SUCCESS;
    }

    @Override
    protected void run() throws CommandException {
        // sign file
        try (
                final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSign));
                final BufferedOutputStream bos = new BufferedOutputStream(new LazyFileOutputStream(outFile))
        ) {
            SignerUtils.signWithKeyStore(
                    storeFile,
                    storeType,
                    StringUtils.isNotBlank(storePass) ? storePass.toCharArray() : null,
                    StringUtils.isNotBlank(keyAlias) ? keyAlias : null,
                    StringUtils.isNotBlank(keyPass) ? keyPass.toCharArray() : null,
                    bis,
                    bos
            );

            System.out.println();
            System.out.println("File successfully signed.");
            System.out.println("Signed File: " + outFile.getCanonicalPath());
        } catch (final IOException e) {
            throw new CommandException(IO_ERROR_WHILE_SIGNING, e);
        } catch (final Exception e) {
            throw new CommandException(ERROR_SIGNING, e);
        }
    }

    @Override
    protected Options getOptions() {
        final Options options = getStandardOptions();
        options.addOption(storeFileOpt);
        options.addOption(storeTypeOpt);
        final OptionGroup storePassGrp = new OptionGroup();
        storePassGrp.addOption(encStorePassOpt);
        storePassGrp.addOption(plaintextStorePassOpt);
        options.addOptionGroup(storePassGrp);
        options.addOption(keyAliasOpt);
        final OptionGroup keyPassGrp = new OptionGroup();
        keyPassGrp.addOption(encKeyPassOpt);
        keyPassGrp.addOption(plaintextKeyPassOpt);
        options.addOptionGroup(keyPassGrp);
        options.addOption(fileToSignOpt);
        options.addOption(outFileOpt);
        return options;
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

    @Override
    protected int getOptionOrdinal(@NotNull final Option opt) {
        // start with parent order
        final int ord = super.getOptionOrdinal(opt);
        if (opt == storeFileOpt) {
            return ord + 1;
        }
        if (opt == storeTypeOpt) {
            return ord + 2;
        }
        if (opt == encStorePassOpt) {
            return ord + 3;
        }
        if (opt == plaintextStorePassOpt) {
            return ord + 4;
        }
        if (opt == keyAliasOpt) {
            return ord + 5;
        }
        if (opt == encKeyPassOpt) {
            return ord + 6;
        }
        if (opt == plaintextKeyPassOpt) {
            return ord + 7;
        }
        if (opt == fileToSignOpt) {
            return ord + 8;
        }
        if (opt == outFileOpt) {
            return ord + 9;
        }

        // shouldn't happen
        return ord;
    }
}
