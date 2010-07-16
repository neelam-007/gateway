package com.l7tech.server.transport.tls;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    static boolean debug = Boolean.getBoolean("debug");
    static boolean eccOnly = false;
    static boolean luna5Only = false;
    static boolean showSkips = true;

    // SunJSSE on Sun JCE providers (no bundled ECC support as of JDK 6)
    static String[] servers_SunJSSE_with_Sun = {
            "server tlsprov sun jceprov sun                                tlsversions TLSv1 certtype rsa clientcert no      ",
            "server tlsprov sun jceprov sun                                tlsversions TLSv1 certtype rsa clientcert yes     ",
            "server tlsprov sun jceprov sun                                tlsversions TLSv1 certtype rsa clientcert optional",
    };

    // SunJSSE on RSA Crypto-J
    static String[] servers_SunJSSE_with_CryptoJ = {
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype rsa clientcert no      ",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype rsa clientcert yes     ",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype rsa clientcert optional",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert no      ",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert yes     ",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert optional",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert no       ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert yes      ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype ecc clientcert optional ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    };

    // SunJSSE on Bouncy Castle
    static String[] servers_SunJSSE_with_BC = {
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype rsa clientcert no      ",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype rsa clientcert yes     ",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype rsa clientcert optional",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert no      ",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert yes     ",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert optional",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert no       ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert yes      ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype ecc clientcert optional ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    };

    // SunJSSE on Luna4 providers
    static String[] servers_SunJSSE_with_Luna4 = {
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype rsa clientcert no      ",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype rsa clientcert yes     ",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype rsa clientcert optional",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert no      ",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert yes     ",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert optional",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert no       ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert yes      ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert optional ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    };

    // SunJSSE on Luna5 provider
    static String[] servers_SunJSSE_with_Luna5 = {
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype rsa clientcert no      ",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype rsa clientcert yes     ",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype rsa clientcert optional",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert no      ",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert yes     ",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert optional",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert no       ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert yes      ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "server tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert optional ciphers TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    };

    // RSA SSL-J on RSA Crypto-J provider
    static String[] servers_RsaJsse_with_CryptoJ = {
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert no      ",
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert yes     ",
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert optional",
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert no      ", // TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 (TLSv1.2)
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert yes     ",
            "server tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert optional",
    };

    // SunJSSE on Sun JCE providers (no bundled ECC support as of JDK 6)
    static String[] clients_SunJSSE_with_Sun = {
            "client tlsprov sun jceprov sun                                tlsversions TLSv1 certtype rsa clientcert no ",
            "client tlsprov sun jceprov sun                                tlsversions TLSv1 certtype rsa clientcert yes",
    };

    // SunJSSE on RSA Crypto-J
    static String[] clients_SunJSSE_with_CryptoJ = {
            "client tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype rsa clientcert no ",
            "client tlsprov sun jceprov rsa                                tlsversions TLSv1 certtype rsa clientcert yes",
    };

    // SunJSSE on Bouncy Castle
    static String[] clients_SunJSSE_with_BC = {
            "client tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype rsa clientcert no ",
            "client tlsprov sun jceprov bc                                 tlsversions TLSv1 certtype rsa clientcert yes",
    };

    // SunJSSE on Luna4 provider
    static String[] clients_SunJSSE_with_Luna4 = {
            "client tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype rsa clientcert no ",
            "client tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype rsa clientcert yes",
            "client tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert no ",
            "client tlsprov sun jceprov luna4 tokenPin FGAA-3LJT-tsHW-NC3E tlsversions TLSv1 certtype ecc clientcert yes",
    };

    // SunJSSE on Luna5 provider
    static String[] clients_SunJSSE_with_Luna5 = {
            "client tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype rsa clientcert no ",
            "client tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype rsa clientcert yes",
            "client tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert no ",
            "client tlsprov sun jceprov luna5 tokenPin PASSWORD            tlsversions TLSv1 certtype ecc clientcert yes",
    };

    // RSA SSL-J on RSA Crypto-J provider
    static String[] clients_RsaJsse_with_CryptoJ = {
            "client tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert no ",
            "client tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype rsa clientcert yes",
            "client tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert no ",
            "client tlsprov rsa jceprov rsa                tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert yes",
    };


    //
    //  *** Servers to run in this test ***
    //
    static String[][] servers = {
            //servers_SunJSSE_with_Sun,
            //servers_SunJSSE_with_CryptoJ,
            //servers_SunJSSE_with_Luna4,
            servers_SunJSSE_with_Luna5,
            //servers_RsaJsse_with_CryptoJ,
            clients_SunJSSE_with_BC,
    };

    //
    //  *** Clients to run in this test ***
    //
    static String[][] clients = {
            clients_SunJSSE_with_Sun,
            clients_SunJSSE_with_CryptoJ,
            //clients_SunJSSE_with_Luna4,
            clients_SunJSSE_with_Luna5,
            clients_RsaJsse_with_CryptoJ,
            clients_SunJSSE_with_BC,
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

    private static Collection<String> flatten(String[][] in) {
        Collection<String> ret = new ArrayList<String>();
        for (String[] strings : in)
            ret.addAll(Arrays.asList(strings));
        return ret;
    }

    public static void main(String[] baseCommand) throws ExecutionException, InterruptedException {
        if (baseCommand.length < 1) usage();

        for (String server : flatten(servers)) {
            boolean serverRequiresClientCert = server.contains("clientcert yes");
            if (eccOnly && server.contains("certtype rsa")) {
                if (showSkips) System.out.println("Server: " + server + ": Skipped: only testing ecc");
                continue;
            }
            System.out.println("Server: " + server);
            for (String client : flatten(clients)) {
                boolean clientHasClientCert = client.contains("clientcert yes");
                if (serverRequiresClientCert && !clientHasClientCert) {
                    if (showSkips) System.out.println("     Client: " + client + "\t\t" + "Skipped: expected to fail");
                    continue;
                }

                if (eccOnly && client.contains("certtype rsa")) {
                    if (showSkips) System.out.println("     Client: " + client + "\t\t" + "Skipped: only testing ecc");
                    continue;
                }

                if (luna5Only && (!server.contains("luna5") && !client.contains("luna5"))) {
                    if (showSkips) System.out.println("     Client: " + client + "\t\t" + "Skipped: only testing luna5");
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
