package com.l7tech.internal.signer;

import com.l7tech.internal.signer.command.Command;
import com.l7tech.internal.signer.command.CommandException;
import com.l7tech.internal.signer.command.CommandRegistry;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.util.Arrays;

import static com.l7tech.internal.signer.SignerErrorCodes.PRINT_HELP;
import static com.l7tech.internal.signer.SignerErrorCodes.SUCCESS;

/**
 * Command line utility to sign arbitrary file (a {@code ServerModuleFile} and/or Solution Kit Archive a.k.a SKAR file).
 */
public class SkarSignerMain {
    /**
     * Signing tool main
     */
    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("No command given. Printing available commands...");
            System.err.println();
            printCommands();
            System.exit(PRINT_HELP);
        }

        // skip the command arg
        final String[] cmdArgs = new String[args.length - 1];
        System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);

        try (final Command command = CommandRegistry.getCommand(args[0])) {
            if (command != null) {
                try {
                    final int errCode = command.init(cmdArgs);
                    if (SUCCESS == errCode) {
                        command.runCommand();
                    }
                    System.exit(errCode);
                } catch (final CommandException e) {
                    System.err.println();
                    //System.err.println("SkarSigner Error: " + ExceptionUtils.getMessage(e));
                    e.printStackTrace(System.err);
                    System.exit(e.getExitCode());
                }
            } else {
                System.err.println("Invalid command: " + args[0]);
                System.err.println();
                printCommands();
                System.exit(SUCCESS);
            }
        }

        System.exit(SUCCESS);
    }

    /**
     * List the different commands
     */
    private static void printCommands() {
        try (final PrintWriter pw = new PrintWriter(System.err, true)) {
            pw.println("Commands:");

            // get the longest command
            int max = 0;
            for (final Command command : CommandRegistry.getCommands()) {
                max = Math.max(command.getName().length(), max);
            }

            // print each command
            String pref = "";
            for (final Command command : CommandRegistry.getCommands()) {
                pw.print(pref);
                String cmdStr = command.getName();
                if (cmdStr.length() < max) {
                    cmdStr += createPadding(max - cmdStr.length());
                }
                cmdStr += createPadding(Command.HELP_DESC_PAD);
                final int nextLineTabStop = max + Command.HELP_DESC_PAD;

                final String cmdDesc = command.getDesc();
                if (StringUtils.isNotBlank(cmdDesc)) {
                    cmdStr += cmdDesc;
                }

                printWrappedText(pw, Command.HELP_WIDTH, nextLineTabStop, cmdStr);

                pref = System.lineSeparator();
            }
            pw.println();
        }
    }

    /**
     * Print the specified {@code text} wrapped within the specified {@code width}.
     *
     * @param pw                 The {@code PrintWriter} to write the help to.
     * @param width              The number of characters to display per line.
     * @param nextLineTabStop    The position on the next line for the first tab.
     * @param text               The text to be written to the PrintWriter.
     */
    private static void printWrappedText(
            @NotNull final PrintWriter pw,
            final int width,
            final int nextLineTabStop,
            @NotNull String text
    ) {
        if (text.length() < width) {
            pw.print(text);
        } else {
            // all following lines must be padded with nextLineTabStop space characters
            final String padding = createPadding(nextLineTabStop);
            pw.println(text.substring(0, width));
            while (true) {
                text = padding + text.substring(width).trim();
                if (text.length() < width) {
                    pw.print(text);
                    break;
                } else {
                    pw.println(text.substring(0, width));
                }
            }
        }
    }

    /**
     * Return a String of padding of length {@code len}.
     *
     * @param len    The length of the String of padding to create.
     * @return The {@code String} of padding
     */
    private static String createPadding(final int len) {
        final char[] padding = new char[len];
        Arrays.fill(padding, ' ');
        return new String(padding);
    }
}
