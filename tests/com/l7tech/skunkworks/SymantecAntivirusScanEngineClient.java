package com.l7tech.skunkworks;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;

/**
 * Sends ICAP requests to Symantec Antivirus Scan Engine to scan content.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 16, 2005<br/>
 * $Id$
 */
public class SymantecAntivirusScanEngineClient {
    private static final byte[] SCAN_REQ = "RESPMOD icap://127.0.0.1:7777/AVSCAN?action=SCAN ICAP/1.0\r\n".getBytes();

    private static final byte[] VIRUS = {0x58, 0x35, 0x4f, 0x21, 0x50, 0x25,
                                         0x40, 0x41, 0x50, 0x5b, 0x34, 0x5c, 0x50, 0x5a,
                                         0x58, 0x35, 0x34, 0x28, 0x50, 0x5e, 0x29, 0x37,
                                         0x43, 0x43, 0x29, 0x37, 0x7d, 0x24, 0x45, 0x49,
                                         0x43, 0x41, 0x52, 0x2d, 0x53, 0x54, 0x41, 0x4e,
                                         0x44, 0x41, 0x52, 0x44, 0x2d, 0x41, 0x4e, 0x54,
                                         0x49, 0x56, 0x49, 0x52, 0x55, 0x53, 0x2d, 0x54,
                                         0x45, 0x53, 0x54, 0x2d, 0x46, 0x49, 0x4c, 0x45,
                                         0x21, 0x24, 0x48, 0x2b, 0x48, 0x2a};
    /**
     * do the thing this class does
     * todo, a return value or exception that indicates if the content is infected
     */
    public String scan(String scanEngineHostname, int scanEnginePort, byte[] stuffToScan, String filename) throws IOException, UnknownHostException {
        String get = "GET http://scapi.symantec.com/" + filename + " HTTP/1.0\r\n\r\n";
        String header = "HTTP/1.0 200 OK\r\n\r\n";

        long bodylength = stuffToScan.length;

        int reshdr = get.length();
        int resbodylngt = reshdr + header.length();

        String icapRequest = "Host: " + scanEngineHostname + ":" + scanEnginePort + "\r\n" +
                             "Allow: 204\r\n" +
                             "Encapsulated: req-hdr=0, res-hdr=" + reshdr + ", res-body=" + resbodylngt + "\r\n\r\n" +
                             get + header + Long.toHexString(bodylength) + "\r\n";

        Socket socket = new Socket(scanEngineHostname, scanEnginePort);
        socket.getOutputStream().write(SCAN_REQ);
        socket.getOutputStream().write(icapRequest.getBytes());
        socket.getOutputStream().write(stuffToScan);
        socket.getOutputStream().write("\r\n0\r\n\r\n".getBytes());

        byte[] returnedfromsav = new byte[4096];
        int read = 0;
        read = socket.getInputStream().read(returnedfromsav);
        StringBuffer output = new StringBuffer();
        while (read > 0) {
            output.append(new String(returnedfromsav, 0, read));
            if (socket.getInputStream().available() <= 0) break;
            read = socket.getInputStream().read(returnedfromsav);
        }
        socket.close();
        return output.toString();

    }

    public String getSavScanEngineOptions(String scanEngineHostname, int scanEnginePort) throws IOException, UnknownHostException {
        String req = "OPTIONS icap://savse.com/avscan ICAP/1.0\r\n\r\n";
        Socket socket = new Socket(scanEngineHostname, scanEnginePort);
        socket.getOutputStream().write(req.getBytes());
        byte[] returnedfromsav = new byte[4096];
        int read = 0;
        read = socket.getInputStream().read(returnedfromsav);
        StringBuffer output = new StringBuffer();
        while (read > 0) {
            output.append(new String(returnedfromsav, 0, read));
            if (socket.getInputStream().available() <= 0) break;
            read = socket.getInputStream().read(returnedfromsav);
        }
        socket.close();
        return output.toString();
    }

    public static void main(String[] args) throws Exception {
        SymantecAntivirusScanEngineClient me = new SymantecAntivirusScanEngineClient();
        //String res = me.getSavScanEngineOptions("localhost", 7777);
        String res = me.scan("localhost", 7777, VIRUS, "blah2092302392");
        //String res = me.scan("localhost", 7777, "blahblahblah".getBytes(), "blah2092302392");
        System.out.println("----------------VIRUS SCAN RESULT----------------\n" + res);
    }
}
