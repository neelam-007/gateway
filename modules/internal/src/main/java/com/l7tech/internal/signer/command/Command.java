package com.l7tech.internal.signer.command;

import com.l7tech.common.password.PasswordEncoder;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import static com.l7tech.internal.signer.SignerErrorCodes.*;

/**
 * Base command containing a help option and hide progress option
 */
public abstract class Command implements Closeable {
    private static final String FILE = "@file:";

    public static final int HELP_WIDTH = 120;
    public static final int HELP_LEFT_PAD = HelpFormatter.DEFAULT_LEFT_PAD;
    public static final int HELP_DESC_PAD = HelpFormatter.DEFAULT_DESC_PAD;


    @NotNull
    private final Timer progressTimer = new Timer("Progress timer for command: " + this.getClass().getName());

    /**
     * Help option
     */
    protected static final Option helpOpt = new OptionBuilder()
            .opt("h")
            .longOpt("help")
            .desc("(Optional) Displays this help text")
            .optional(true)
            .build();
    /**
     * Hide progress option
     */
    protected static final Option hideProgressOpt = new OptionBuilder()
            .opt("i")
            .longOpt("hideProgress")
            .desc("(Optional) Hides progress indicator")
            .optional(true)
            .build();

    /**
     * The command line field holds the command line arguments for this command
     */
    @NotNull private CommandLine commandLine;

    @NotNull protected PrintStream out;
    @NotNull protected PrintStream err;
    @NotNull protected ByteArrayOutputStream baos;

    private boolean hideProgress;

    protected Command() {
        err = System.err;
        baos = new ByteArrayOutputStream();
        out = new PrintStream(baos);
    }

    /**
     * Gather help and hide progress options.
     */
    protected Options getStandardOptions() {
        final Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(hideProgressOpt);
        return options;
    }

    /**
     * Gather full list of {@code Command}'s options.
     */
    protected abstract Options getOptions();

    /**
     * Initialize the command using command line args.
     */
    public final int init(@NotNull final String[] args) {
        final Options helpOptions = new Options();
        helpOptions.addOption(helpOpt);

        final CommandLineParser parser = new BasicParser();

        try {
            // first parse help
            commandLine = parser.parse(helpOptions, args);

            // check if help was requested
            if (hasOption(helpOpt)) {
                printHelp(null, getOptions());
                return PRINT_HELP;
            }
        } catch (final ParseException | IllegalArgumentException e) {
            // ignore
        }

        // now parse the rest of the commands
        final Options options = getOptions();
        try {
            commandLine = parser.parse(options, args);
        } catch (final ParseException | IllegalArgumentException e) {
            printHelp("Parsing failed.  Reason: " + ExceptionUtils.getMessage(e), options);
            return PARSING_ERROR;
        }
        hideProgress = hasOption(hideProgressOpt);
        try {
            return init();
        } catch (final IllegalArgumentException e) {
            printHelp("Invalid Argument: " + ExceptionUtils.getMessage(e), options);
            return INVALID_ARG;
        }
    }

    /**
     * Wraps progress timer (only if hide progress option is not set) around command execution.
     *
     * @throws CommandException if an error happens while executing the command.
     */
    public void runCommand() throws CommandException {
        try {
            System.out.println("Running Command: " + getName());

            startProgressTimer();

            run();

            if (!hideProgress) {
                System.out.println();
            }

            baos.writeTo(System.out);
        } catch (final IOException e) {
            err.println("Error writing to System.out: " + ExceptionUtils.getMessageWithCause(e));
        } finally {
            endProgressTimer();
        }
    }

    private void startProgressTimer() {
        if (hideProgress) {
            return;
        }
        progressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.print(".");
            }
        }, 0, 500);
    }

    private void endProgressTimer() {
        if (hideProgress) {
            return;
        }
        progressTimer.purge();
        progressTimer.cancel();
    }


    /**
     * Prints this help
     */
    private void printHelp(@Nullable final String message, @NotNull final Options options) {
        if (message != null) {
            err.println(message);
            err.println();
        }
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(getOptionComparator());

        try (final PrintWriter pw = new PrintWriter(out, true)) {
            formatter.printHelp(
                    pw,
                    HELP_WIDTH,
                    getName(),
                    System.lineSeparator() + getDesc() + System.lineSeparator() + "Options:",
                    options,
                    HELP_LEFT_PAD,
                    HELP_DESC_PAD,
                    null,
                    true
            );
        }
        try {
            baos.writeTo(System.out);
        } catch (IOException e) {
           err.println("Unable to write to standard out: " + ExceptionUtils.getMessageWithCause(e));
        }
    }

    /**
     * Checks whether this {@code option} is specified on the commandline.
     *
     * @param option The option to look for.
     * @return {@code true} if this option is specified on the {@link #commandLine}, {@code false} otherwise.
     */
    protected boolean hasOption(@NotNull final Option option) {
        return commandLine.hasOption(option.getLongOpt());
    }

    /**
     * Returns the value for the given {@code option}. The option is looked for in the {@link #commandLine}.
     *
     * @param option The option who's value to find
     * @return The value of the {@code option} or {@code null} if the option is not specified or does not have a value.
     */
    protected String getOptionValue(@NotNull final Option option) {
        return getOptionValue(option, null);
    }

    /**
     * Returns the value for the given {@code option}. The option is looked for in {@link #commandLine}.
     *
     * @param option       The option who's value to find
     * @param defaultValue The default value for the option if it cannot be found on the command line or in the
     *                     properties
     * @return The value of the {@code option} or {@code defaultValue} if the option is not specified or does not have a value.
     */
    protected String getOptionValue(@NotNull final Option option, @Nullable final String defaultValue) {
        return StringUtils.trim(
                commandLine.hasOption(option.getLongOpt())
                        ? commandLine.getOptionValue(option.getLongOpt())
                        : defaultValue
        );
    }


    protected void setErr(@NotNull final PrintStream err) {
        this.err = err;
    }

    protected void setOut(@NotNull final PrintStream out) {
        this.out = out;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /**
     * Initialize the subclass command using the {@link #commandLine}.
     */
    protected abstract int init();

    /**
     * Execute the command
     *
     * @throws CommandException if an error happens while command execution.
     */
    protected abstract void run() throws CommandException;

    /**
     * @return This {@code Command}'s name
     */
    @NotNull
    public abstract String getName();

    /**
     * @return This {@code Command}'s description
     */
    public abstract String getDesc();

    /**
     * Comparator used to sort the options when they output in help text.
     * <p>
     *     Note: For {@code CLI v1.2} default options sorting is case-insensitive alphabetical sorting by option key.<br/>
     *     Deprecate this method (or return {@code null}) once {@code CLI} is upgraded to {@code v1.3} or higher,
     *     where default sorting order is the order options were declared.
     * </p>
     */
    protected final Comparator<Option> getOptionComparator() {
        return new Comparator<Option>() {
            @Override
            public int compare(final Option o1, final Option o2) {
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    return getOptionOrdinal(o1) - getOptionOrdinal(o2);
                }
            }
        };
    }

    /**
     * Get the order of the specified option.
     *
     * @param opt    the {@code Option} instance.
     * @return {@code 1} for {@link #helpOpt}, {@code 2} for {@link #hideProgressOpt} and {@code 3} for anything else
     * (indicating this option should appear after these ones).
     */
    protected int getOptionOrdinal(@NotNull final Option opt) {
        if (opt == helpOpt) {
            return 1;
        }
        if (opt == hideProgressOpt) {
            return 2;
        }
        // not mine command
        return 3;
    }

    /**
     * Get a password value, decoding if necessary.
     *
     * @param encodedOption      the option specifying the encoded password.
     * @param plaintextOption    the option specifying the plaintext password.
     * @return the password, decrypted if necessary
     * @throws IllegalArgumentException if unable to decode an encoded password or read the password from a file.
     */
    protected String getPassword(final Option encodedOption, final Option plaintextOption) {
        // password is encodedOption or plaintextOption or none, in which case it is null
        String password = hasOption(encodedOption) ? getOptionValue(encodedOption) : getOptionValue(plaintextOption);
        // if password is specified via FILE; then read the file content
        if (password != null && password.startsWith(FILE)) {
            final String passFile = password.substring(FILE.length(), password.length());
            try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(passFile))) {
                final byte[] bytes = IOUtils.slurpStream(bis);
                password = new String(bytes, Charsets.UTF8).trim();
            } catch (final IOException e) {
                throw new IllegalArgumentException("Error reading password file: " + ExceptionUtils.getMessage(e), e);
            }
        }
        // if its encoded option; then decrypt password
        if (password != null && hasOption(encodedOption)) {
            try {
                password = new String(PasswordEncoder.decodePassword(password), Charsets.UTF8);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Invalid encoded password", e);
            }
        }
        // finally return password
        return password;
    }
}
