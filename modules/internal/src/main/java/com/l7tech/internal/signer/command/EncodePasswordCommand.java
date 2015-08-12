package com.l7tech.internal.signer.command;

import com.l7tech.common.password.PasswordEncoder;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import static com.l7tech.internal.signer.SignerErrorCodes.*;

/**
 * Command which can encodes a given password from args or user input.
 */
public class EncodePasswordCommand extends Command {
    private static final String PASSWORD = "password";
    static final String COMMAND_NAME = "encodePassword";
    static final String COMMAND_DESC = "Encode a given password to be used for " + SignCommand.COMMAND_NAME + " command";

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final Option passOpt = new OptionBuilder()
            .opt("x")
            .longOpt(PASSWORD)
            .argName(PASSWORD)
            .desc("(Optional) Password to encode; leave blank to be prompted for password")
            .optional(true)
            .build();
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // don't forget to update getOptions() and getOptionOrdinal() when adding new options
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull private final BufferedReader reader;
    private String password;

    /**
     * Default construct using {@code System.in}.
     */
    public EncodePasswordCommand() {
        this(System.in);
    }

    /**
     * Construct the command with the specified {@code inputStream}
     *
     * @param inputStream the {@code InputStream} to use when reading user input.
     */
    public EncodePasswordCommand(final InputStream inputStream) {
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    protected int init() {
        final String passOptValue = getOptionValue(passOpt);
        if (passOptValue != null) {
            password = passOptValue;
        } else {
            // prompt
            out.println("Enter the password to encode:");
            try {
                baos.writeTo(System.out);
                baos.reset();
            } catch (final IOException e) {
                throw new IllegalArgumentException("Unable to write to standard out: " + ExceptionUtils.getMessage(e));
            }

            // hide input if possible
            final Console console = System.console();
            if (console != null) {
                password = new String(console.readPassword());
            } else {
                try {
                    password = reader.readLine();
                } catch (final IOException e) {
                    throw new IllegalArgumentException("Error reading password to encode: " + ExceptionUtils.getMessage(e));
                }
            }
        }

        return SUCCESS;
    }

    @Override
    protected void run() throws CommandException {
        // print the password out
        try {
            out.println();
            out.println(PasswordEncoder.encodePassword(password.getBytes("UTF-8")));
            out.println();
        } catch (final IOException e) {
            throw new CommandException(ERROR_ENCODING_PASSWORD, e);
        }
    }

    @Override
    protected Options getOptions() {
        final Options options = getStandardOptions();
        options.addOption(passOpt);
        return options;
    }

    @Override
    protected int getOptionOrdinal(@NotNull final Option opt) {
        // start with parent order
        final int ord = super.getOptionOrdinal(opt);
        if (opt == passOpt) {
            return ord + 1;
        }

        // shouldn't happen
        return ord;
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
