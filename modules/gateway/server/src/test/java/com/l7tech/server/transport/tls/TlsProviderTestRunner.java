package com.l7tech.server.transport.tls;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs all combinations of server and client configurations.
 * Skips any configuration where the server requires a client cert but the client doesn't have one,
 * as these are expected to fail anyway.
 */
public class TlsProviderTestRunner {
    private static boolean debug = Boolean.getBoolean("debug");

    static String[] servers = {
            "server tlsprov sun tlsversions TLSv1                 certtype rsa clientcert no",
            "server tlsprov sun tlsversions TLSv1                 certtype rsa clientcert yes",
            "server tlsprov sun tlsversions TLSv1                 certtype rsa clientcert optional",
            "server tlsprov sun tlsversions TLSv1                 certtype ecc clientcert no",
            "server tlsprov sun tlsversions TLSv1                 certtype ecc clientcert yes",
            "server tlsprov sun tlsversions TLSv1                 certtype ecc clientcert optional",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert no",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert yes",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert optional",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert no",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert yes",
            "server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert optional",
    };

    static String[] clients = {
            "client tlsprov sun tlsversions TLSv1                 certtype rsa clientcert no",
            "client tlsprov sun tlsversions TLSv1                 certtype rsa clientcert yes",
            "client tlsprov sun tlsversions TLSv1                 certtype ecc clientcert no",
            "client tlsprov sun tlsversions TLSv1                 certtype ecc clientcert yes",
            "client tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert no",
            "client tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert yes",
            "client tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert no",
            "client tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert yes",
    };

    enum Result {
        SUCCESS,
        FAILURE,
        UNABLE_TO_RUN
    }

    static ExecutorService executor = Executors.newCachedThreadPool();

    static class Pair<L,R> {
        public final L left;
        public final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
    }

    static Future<Pair<Result, String>> spawn(String[] baseCommand, String args) {
        final List<String> command = new ArrayList<String>(Arrays.asList(baseCommand));
        command.addAll(Arrays.asList(args.split("\\s+")));
        return executor.submit(new Callable<Pair<Result, String>>() {
            @Override
            public Pair<Result, String> call() throws Exception {
                Process proc = null;
                InputStream is = null;
                try {
                    if (debug) System.err.println("Running command: " + command);
                    proc = new ProcessBuilder(command).redirectErrorStream(true).start();
                    is = proc.getInputStream();
                    final InputStream is1 = is;
                    byte[] output = (new Callable<ByteArrayHolder>() {
                        @Override
                        public ByteArrayHolder call() throws Exception {
                            return new ByteArrayHolder(slurp(is1));
                        }
                    }).call().getData();
                    is.close(); is = null;
                    proc.waitFor();
                    int exitValue = proc.exitValue();
                    String outString = new String(output);
                    if (debug) System.err.println("Command " + command + " exitValue=" + exitValue + " output: " + outString);
                    if (exitValue == 0) {
                        Matcher matcher = Pattern.compile("^Connected: ([^\\015\\012]+)", Pattern.MULTILINE).matcher(outString);
                        String msg = matcher.find() ? matcher.group(1) : "Unknown connection info";
                        return new Pair<Result, String>(Result.SUCCESS, msg);
                    } else {
                        Matcher matcher = Pattern.compile("^Exception in thread \"main\" ([^\\015\\012]+)", Pattern.MULTILINE).matcher(outString);
                        String msg = matcher.find() ? matcher.group(1) : "Unknown connection info";
                        return new Pair<Result, String>(Result.FAILURE, msg);
                    }
                } catch (IOException e) {
                    System.err.println("Failure running " + command + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    return new Pair<Result, String>(Result.UNABLE_TO_RUN, e.getMessage());
                } finally {
                    closeQuietly(is);
                    if (proc != null) proc.destroy();
                }
            }
        });
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            // Ignored
        }
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[512];
        int got;
        while ((got = in.read(buf)) > 0)
            out.write(buf, 0, got);
    }

    static byte[] slurp(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private static class ByteArrayHolder {
        private final byte[] data;
        private ByteArrayHolder(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }

    public static void main(String[] baseCommand) throws ExecutionException, InterruptedException {
        if (baseCommand.length < 1) usage();

        for (String server : servers) {
            System.out.println("Server: " + server);
            boolean serverRequiresClientCert = server.contains("clientcert yes");
            for (String client : clients) {
                boolean clientHasClientCert = client.contains("clientcert yes");
                if (serverRequiresClientCert && !clientHasClientCert) {
                    System.out.println("     Client: " + client + "\t\t" + "Skipped: expected to fail");
                    continue;
                }

                Future<Pair<Result, String>> serverResult = spawn(baseCommand, server);
                Future<Pair<Result, String>> clientResult = spawn(baseCommand, client);

                Pair<Result, String> sr = serverResult.get();
                Pair<Result, String> cr = clientResult.get();

                if (Result.SUCCESS.equals(sr.left) && Result.SUCCESS.equals(cr.left)) {
                    System.out.println("     Client: " + client + "\t\t" + "Success: " + sr.right);
                } else {
                    System.out.println("     Client: " + client + "\t\t" + "FAILURE");
                    System.out.println("          Server result: " + sr.left + " " + sr.right);
                    System.out.println("          Client result: " + cr.left + " " + cr.right);
                }
            }
        }
    }

    private static void usage() {
        System.out.println("Usage: TlsProviderTestRunner <command prefix>\n" +
                "\n" +
                " Command prefix is the command to run with the various options, ie   /usr/bin/java -Doption=val com.l7tech.StuffDoer");
        System.exit(1);
    }
}
