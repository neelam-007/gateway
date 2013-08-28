package com.l7tech.common.io;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for easily invoking subprocesses such as Unix commands and exchanging byte arrays with them via
 * their stdin/stdout.
 */
public class ProcUtils {
    protected static final Logger logger = Logger.getLogger(ProcUtils.class.getName());

    /**
     * Flatten an argument list of mixed String and String[] instances into a single String[] array.
     *
     * @param args an array of String or String[] instances.  For other types, this will invoke toString().
     *        Must not be null and must not contain nulls.
     * @return a flattened array of String instances.  Never null, but may be empty if args is empty.
     *         Will not contain nulls unless at least one null-containing String[] was passed as an argument.
     * @throws NullPointerException if args is null or contains a null.
     */
    public static String[] args(Object... args) {
        List<String> ret = new ArrayList<String>();
        for (Object arg : args) {
            if (arg instanceof String[]) {
                String[] strings = (String[])arg;
                ret.addAll(Arrays.asList(strings));
            } else
                ret.add(arg.toString());
        }

        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Runs the command line given as a string array. The first element is the command, the rest are its arguments.
     *
     * @see #exec(java.io.File, String[])
     */
    public static ProcResult exec(String[] commandLine) throws IOException {
        return exec(commandLine, (byte[]) null);
    }

    public static ProcResult exec(String[] commandLine, byte[] stdin) throws IOException {
        return exec(commandLine, stdin != null ? new ByteArrayInputStream(stdin) : null);
    }

    public static ProcResult exec(String[] commandLine, InputStream stdin) throws IOException {
        int length = commandLine.length;
        String[] args = length > 1 ? Arrays.asList(commandLine).subList(1, length).toArray(new String[length-1]) : new String[0];
        return exec(null, new File(commandLine[0]), args, stdin, true);
    }

    /**
     * Executes the given command line. The space character is the delimeter between the command and its arguments.
     *
     * @see #exec(java.io.File, String[])
     */
    public static ProcResult exec(String commandLine) throws IOException {
        return exec(commandLine, (byte[]) null);
    }

    public static ProcResult exec(String commandLine, byte[] stdin) throws IOException {
        return exec(commandLine, stdin != null ? new ByteArrayInputStream(stdin) : null);
    }

    public static ProcResult exec(String commandLine, InputStream stdin) throws IOException {
        return exec(commandLine.split(" "), stdin);
    }

    /**
     * Run the specified command with the no arguments and no input and return its output and exit status if
     * the process exits zero.
     * <p/>
     * The program will be run in a subprocess that inherits the current Java process' current working directory.
     *
     * @param program  the program to run.  Required.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File program) throws IOException {
        return exec(null, program, new String[0], (byte[])null, true);
    }

    /**
     * Run the specified command with the no arguments and no input and return its output and exit status if
     * the process exits zero.
     * <p/>
     * The program will be run in a subprocess that is first chdir'ed to the specified current working directory.
     *
     * @param cwd the current working directory in which to run the program, or null to inherit the current cwd.
     *            This is not necessarily the directory in which the program can be found -- just the cwd the
     *            subprocess shall be in before the program is invoked.
     * @param program  the program to run.  Required.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File cwd, File program) throws IOException {
        return exec(cwd, program, new String[0], (byte[])null, true);
    }

    /**
     * Run the specified command with the specified arguments and no input, and return its output and exit status
     * if the process exits zero.
     * <p/>
     * The program will be run in a subprocess that inherits the current Java process' current working directory.
     *
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File program, String[] args) throws IOException {
        return exec(null, program, args, (byte[])null, true);
    }

    /**
     * Run the specified command with the specified arguments and no input, and return its output and exit status
     * if the process exits zero.
     * <p/>
     * The program will be run in a subprocess that is first chdir'ed to the specified current working directory.
     *
     * @param cwd the current working directory in which to run the program, or null to inherit the current cwd.
     *            This is not necessarily the directory in which the program can be found -- just the cwd the
     *            subprocess shall be in before the program is invoked.
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File cwd, File program, String[] args) throws IOException {
        return exec(cwd, program, args, (byte[])null, true);
    }

    /**
     * Run the specified command with the specified arguments, optionally piping the specified byte array
     * into the program's standard input, and return its output and exit status if the process exits zero.
     * <p/>
     * The program will be run in a subprocess that inherits the current Java process' current working directory.
     *
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @param stdin  a byte array to pass into the program's stdin, or null to provide no input.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File program, String[] args, byte[] stdin) throws IOException {
        return exec(null, program, args, stdin, true);
    }

    /**
     * Run the specified command with the specified arguments, optionally piping the specified byte array
     * into the program's standard input, and return its output and exit status if the process exits zero.
     * <p/>
     * The program will be run in a subprocess that is first chdir'ed to the specified current working directory.
     *
     * @param cwd the current working directory in which to run the program, or null to inherit the current cwd.
     *            This is not necessarily the directory in which the program can be found -- just the cwd the
     *            subprocess shall be in before the program is invoked.
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @param stdin  a byte array to pass into the program's stdin, or null to provide no input.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File cwd, File program, String[] args, byte[] stdin) throws IOException {
        return exec(cwd, program, args, stdin, true);
    }

    /**
     * Run the specified command with the specified arguments, optionally piping the specified byte array
     * into the program's standard input, and optionally throwing an IOException if the subprocess exits
     * with any exit code other than zero, and return its output and exit status.
     * <p/>
     * The program will be run in a subprocess that inherits the current Java process' current working directory.
     *
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @param stdin  a byte array to pass into the program's stdin, or null to provide no input.
     * @param allowNonzeroExit  if true, this will return a result even if the program exits nonzero.
     *                          if false, IOException will be thrown if the program exits nonzero.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File program, String[] args, byte[] stdin, boolean allowNonzeroExit) throws IOException {
        return exec(null, program, args, stdin, allowNonzeroExit);
    }

    /**
     * Run the specified command with the specified arguments, optionally piping the specified byte array
     * into the program's standard input, and optionally throwing an IOException if the subprocess exits
     * with any exit code other than zero, and return its output and exit status.
     * <p/>
     * The program will be run in a subprocess that is first chdir'ed to the specified current working directory.
     *
     * @param cwd the current working directory in which to run the program, or null to inherit the current cwd.
     *            This is not necessarily the directory in which the program can be found -- just the cwd the
     *            subprocess shall be in before the program is invoked.
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @param stdin  a byte array to pass into the program's stdin, or null to provide no input.
     * @param allowNonzeroExit  if true, this will return a result even if the program exits nonzero.
     *                          if false, IOException will be thrown if the program exits nonzero.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     */
    public static ProcResult exec(File cwd, File program, String[] args, byte[] stdin, boolean allowNonzeroExit) throws IOException {
        if (stdin != null && logger.isLoggable(Level.FINEST))
            logger.finest("Sending " + stdin.length + " bytes of input into program: " + program.getName());
        return exec(cwd, program, args, stdin != null ? new ByteArrayInputStream(stdin) : null, allowNonzeroExit);
    }

    public static ProcResult exec(File cwd, File program, String[] args, InputStream stdin, boolean allowNonzeroExit) throws IOException {
        try {
            return doExec(cwd, program, args, stdin, allowNonzeroExit);
        } catch (IOException | ExecutionException e) {
            throw new CausedIOException("Unable to invoke " + program.getName() + " program: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted during invocation of program " + program.getName() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static ProcResult doExec(File cwd, File program, String[] args, InputStream stdin, boolean allowNonzeroExit) throws InterruptedException, IOException, ExecutionException {
        String[] cmdArray = new String[args.length + 1];
        cmdArray[0] = program.getPath();
        System.arraycopy(args, 0, cmdArray, 1, args.length);

        if (logger.isLoggable(Level.FINEST)) logger.finest("Running program: " + program.getName());
        Process proc = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            proc = new ProcessBuilder(cmdArray).directory(cwd).redirectErrorStream(true).start();

            os = proc.getOutputStream();
            if (stdin != null) {
                if (logger.isLoggable(Level.FINEST)) logger.finest("Copying the provided input stream as input into program: " + program.getName());
                try {
                    IOUtils.copyStream(stdin, os);
                } finally {
                    ResourceUtils.closeQuietly(stdin);
                }
                os.flush();
            }
            os.close(); os = null;

            if (logger.isLoggable(Level.FINEST)) logger.finest("Reading output from program: " + program.getName());
            is = proc.getInputStream();
            final byte[]  slurped = IOUtils.slurpStream(is);
            is.close();
            is = null;
            if (logger.isLoggable(Level.FINEST)) logger.finest("Read " + slurped.length + " bytes of output from program: " + program.getName());

            final int status = proc.waitFor();
            if (logger.isLoggable(Level.FINEST)) logger.finest("Program " + program.getName() + " exited status code " + status);
            if (!allowNonzeroExit && status != 0)
                throw new IOException("Program " + program.getName() + " exited with nonzero status " + status + ".  Output: " + new String(slurped));

            return new ProcResult(status, slurped);
        } finally {
            ResourceUtils.closeQuietly(is);
            ResourceUtils.closeQuietly(os);
            if (proc != null)
                proc.destroy();
        }
    }
}
