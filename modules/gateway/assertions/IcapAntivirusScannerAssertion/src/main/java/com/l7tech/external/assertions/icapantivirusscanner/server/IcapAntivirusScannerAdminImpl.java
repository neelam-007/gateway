package com.l7tech.external.assertions.icapantivirusscanner.server;

import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAdmin;
import com.l7tech.util.InetAddressUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    public void testConnection(final String host, final int port, final String serviceName) throws IcapAntivirusScannerTestException {

        Socket sock = new Socket();
        OutputStream out = null;
        BufferedReader br = null;
        try {
            InetAddress address = InetAddressUtil.getAddress(host);
            //testing the server is trivial enough that we can do it by hand.
            //using the netty & icap framework is rather expensive
            sock.connect(new InetSocketAddress(address, port), 30000);
            out = sock.getOutputStream();
            String icapUri = "icap://" + host + ":" + port + "/" + serviceName;
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
            throw new IcapAntivirusScannerTestException("Error connecting to server: " + ex.getMessage());
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
