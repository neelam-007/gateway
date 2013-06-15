package com.l7tech.skunkworks.gclient;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.TeeInputStream;
import com.l7tech.common.io.TeeOutputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.ExceptionUtils;

import javax.xml.soap.SOAPConstants;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

/**
 * This class transmits an enormous (configurable, 500mb by default) SOAP request, and ensures that the
 * response exactly matches the request.
 */
public class HugeEchoClient {
    private static final byte[] headerBytes = ("<s:Envelope xmlns:s=\"" + SOAPConstants.URI_NS_SOAP_ENVELOPE + "\">\n  <s:Body>\n    <blah>").getBytes();
    private static final byte[] footerBytes = "\n    </blah>\n  </s:Body>\n</s:Envelope>".getBytes();
    private static final long footerSize = headerBytes.length;
    private static final long headerSize = footerBytes.length;
    private static final long extraSize = footerSize + headerSize;
    private static final long minSize = extraSize + 1;
    private static boolean verbose = false;

    private static void usage(PrintStream err) {
        final String mess = "Usage: HugeEchoClient size (postUrl | filename) [options]\n\n" +

                            "Size may be a number followed by k, M, or G to specify size in kB, MB, or GB.\n" +
                            "If postUrl is included, the request will be posted to that URL, and the response\n" +
                            "will be read and checked to ensure that it is an exact echo of the request.\n" +
                            "If filename is included, the generated SOAP envelope will be written to the\n" +
                            "specified file and no HTTP activity will be attempted.\n\n" +

                            "Supported options:\n" +
                            "    logReq          log request message to System.out\n" +
                            "    logReqErr       log request message to System.err\n" +
                            "    logRes          log response message to System.out\n" +
                            "    logResErr       log response message to System.err\n" +
                            "    octetStream     transmit as application/octet-stream instead of text/xml\n\n" +

                            "Examples: HugeEchoClient 200M req200m.xml           Create 200mb test file\n" +
                            "          HugeEchoClient 1G http://ssg:8080/echo logRes octetStream\n" +
                            "                                                    Test echo service with 1GB,\n" +
                            "                                                    logging response and sending\n" +
                            "                                                    MIME type disguised as bytes\n";
        err.print(mess);
    }

    enum Opts {
        logreq,
        logres,
        logreqerr,
        logreserr,
        octetstream,
        verbose,
    }

    private static void usage() {
        usage(System.err);
        System.exit(7);
    }

    private static void usage(String message) {
        System.err.println(message);
        System.err.println("\nRerun with no arguments to see usage instructions.");
        System.exit(7);
    }

    private static long plong(String str, int numChop, long mult) throws NumberFormatException {
        str = str.substring(0, str.length() - numChop);
        return Long.valueOf(str) * mult;
    }
    
    public static void main(String[] args) throws IllegalArgumentException, IOException {
        if (args.length < 2) usage();

        long size = -1;
        try {
            String str = args[0].toUpperCase();
            if (str.endsWith("K"))       size = plong(str, 1, 1024);
            else if (str.endsWith("KB")) size = plong(str, 2, 1024);
            else if (str.endsWith("M"))  size = plong(str, 1, 1024 * 1024);
            else if (str.endsWith("MB")) size = plong(str, 2, 1024 * 1024);
            else if (str.endsWith("G"))  size = plong(str, 1, 1024 * 1024 * 1024);
            else if (str.endsWith("GB")) size = plong(str, 2, 1024 * 1024 * 1024);
            else
                size = Long.valueOf(str);
        } catch (NumberFormatException nfe) {
            // fallthrough and fail
        }
        if (size <= minSize) {
            usage("Size must be an integer between " + minSize + " and " + Long.MAX_VALUE);
            return;
        }

        URL url = null;
        File file = null;
        try {
            url = new URL(args[1]);
        } catch (MalformedURLException mue) {
            file = new File(args[1]);
        }

        OutputStream logreq = new NullOutputStream();
        OutputStream logres = new NullOutputStream();
        ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;

        // Scan for options
        for (int i = 2; i < args.length; ++i) {
            String name = args[i];
            try {
                Opts opt = Opts.valueOf(name.trim().toLowerCase());
                switch (opt) {
                case verbose:
                    verbose = true;
                    mess("Verbose enabled");
                    break;
                case logreqerr:
                    logreq = System.err;
                    mess("Logging request to System.err");
                    break;
                case logreq:
                    logreq = System.out;
                    mess("Logging request to System.out");
                    break;
                case logreserr:
                    logres = System.err;
                    mess("Logging response to System.err");
                    break;
                case logres:
                    logres = System.out;
                    mess("Logging response to System.out");
                    break;
                case octetstream:
                    ctype = ContentTypeHeader.OCTET_STREAM_DEFAULT;
                    mess("Sending content type: " + ctype.getFullValue());
                    break;
                }
            } catch (IllegalArgumentException e) {
                usage("Unrecognized option: " + name);
                return;
            }
        }

        if (file != null) {
            createTestFile(size, file, logreq);
            return;
        }

        try {
            boolean equal = runEchoTest(url, size, logreq, logres, ctype);
            if (!equal) {
                System.err.println("The posted stream did not match the response stream.");
                System.exit(1);
            }
            System.out.println("The posted stream matched the response stream.");
        } catch (IOException ioe) {
            System.err.println(ExceptionUtils.getMessage(ioe));
            System.exit(2);
        } catch (Throwable t) {
            final String mess = "Failed to run echo test: " + ExceptionUtils.getMessage(t);
            System.err.println(mess);
            t.printStackTrace(System.err);
            throw new RuntimeException(mess, t); 
        }

    }

    private static void mess(String s) {
        if (!verbose) return;
        System.err.println(s);
    }

    public static void createTestFile(long size, File file, OutputStream logreq) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            IOUtils.copyStream(createGenerator(size), new TeeOutputStream(fos, logreq));
        } finally {
            fos.close();
        }
    }

    public static boolean runEchoTest(URL url, long size, OutputStream logreq, OutputStream logres, ContentTypeHeader ctype) throws IOException
    {

        if (logreq == null) logreq = new NullOutputStream();
        if (logres == null) logres = new NullOutputStream();

        GenericHttpClient client = new HttpComponentsClient();
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);
        params.setContentType(ctype);
        params.setContentLength(size);

        GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
        request.setInputStream(new TeeInputStream(createGenerator(size), logreq));

        GenericHttpResponse resp = request.getResponse();

        boolean equal = IOUtils.compareInputStreams(createGenerator(size), true, new TeeInputStream(resp.getInputStream(), logres), true);

        int status = resp.getStatus();
        if (status != 200)
            System.err.println("WARNING: Response status was " + status);
        if (!ctype.getFullValue().equalsIgnoreCase(resp.getContentType().getFullValue()))
            System.err.println("WARNING: Response content type was not " + ctype.getFullValue() + " (got " + resp.getContentType().getFullValue() + ")");

        return equal;
    }

    private static InputStream createGenerator(long size) {
        final InputStream header = createHeaderStream();
        final InputStream footer = createFooterStream();
        return new SequenceInputStream(header,
                                       new SequenceInputStream(createPayloadStream(size - headerSize - footerSize, 1234567),
                                                               footer));
    }

    private static InputStream createHeaderStream() {
        return new ByteArrayInputStream(headerBytes);
    }

    private static InputStream createFooterStream() {
        return new ByteArrayInputStream(footerBytes);
    }

    /*
     * Returns an InputStream that will emit the specified number of bytes of well-formed XML.
     * Two invocations of this method with the same seed will produce exactly the same bytestream.
     */
    private static InputStream createPayloadStream(final long bytes, long seed) {
        final Random rand = new Random(seed);
        return new InputStream() {
            long count = bytes;

            public int read() throws IOException {
                if (count < 1) return -1;
                count--;
                int i = rand.nextInt(charlen);
                return chars[i];
            }
        };
    }

    private static final int[] chars;
    private static final int charlen;
    static {
        int[] ch = new int[1024];
        int c = 0;
        for (int i = 'a'; i <= 'z';) { ch[c++] = i; ch[c++] = i++; }
        for (int i = 'A'; i <= 'Z';) ch[c++] = i++;
        for (int i = '0'; i <= '9';) ch[c++] = i++;
        ch[c++] = '_';
        ch[c++] = '-';
        ch[c++] = '=';
        ch[c++] = '*';
        ch[c++] = '(';
        ch[c++] = ')';
        ch[c++] = '+';
        ch[c++] = '$';
        ch[c++] = '@';
        ch[c++] = '!';
        ch[c++] = '^';
        ch[c++] = '%';
        ch[c++] = '\n';
        ch[c++] = '\n';
        for (int i = 'a'; i <= 'z';) ch[c++] = i++;
        chars = new int[c];
        System.arraycopy(ch, 0, chars, 0, c);
        charlen = chars.length;
    }
}
