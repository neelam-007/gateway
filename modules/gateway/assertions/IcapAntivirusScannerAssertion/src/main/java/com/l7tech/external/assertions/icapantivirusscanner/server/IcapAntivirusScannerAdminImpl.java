package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAdmin;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.util.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>An implementation to test the ICAP connection.</p>
 *
 * @author KDiep
 */
public class IcapAntivirusScannerAdminImpl implements IcapAntivirusScannerAdmin {
    private static final Pattern STATUS_LINE = Pattern.compile("(?i)ICAP/\\d\\.\\d\\s(\\d+)\\s(.*)");

    @Override
    public void testConnection(final String icapServerUrl) throws IcapAntivirusScannerTestException {

        Socket sock = new Socket();
        OutputStream out = null;
        BufferedReader br = null;
        try {
            //testing the server is trivial enough that we can do it by hand.
            //using the netty & icap framework is rather expensive
            final Matcher icapMatcher = IcapAntivirusScannerAssertion.ICAP_URI.matcher(icapServerUrl);
            if (!icapMatcher.matches()) {
                throw new IcapAntivirusScannerTestException("Invalid ICAP URL");
            }

            final String nonSchemePart = icapMatcher.group(1);
            final URL icapUrl = new URL("http" + nonSchemePart);
            sock.connect(new InetSocketAddress(icapUrl.getHost(), icapUrl.getPort()), 30000);
            out = sock.getOutputStream();
            String icapUri = "icap://" + icapUrl.getHost() + ":" + icapUrl.getPort() + icapUrl.getPath();
            out.write(String.format("OPTIONS %1$s %2$s %3$s%3$s", icapUri, "ICAP/1.0", "\r\n").getBytes());
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = br.readLine();
            if (line == null) {
                throw new IcapAntivirusScannerTestException("No response received from server.");
            }
            Matcher matcher = STATUS_LINE.matcher(line);
            if (!(matcher.matches() && "200".equals(matcher.group(1)))){
                throw new IcapAntivirusScannerTestException("Invalid server connection: " + line);
            }
        } catch (IOException ex) {
            throw new IcapAntivirusScannerTestException("Error connecting to server: " + ex.getMessage(), ExceptionUtils.getDebugException(ex));
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (br != null) {
                    br.close();
                }
                if (sock != null) {
                    sock.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
    }
}
