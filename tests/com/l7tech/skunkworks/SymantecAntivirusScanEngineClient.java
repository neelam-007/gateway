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

    /**
     * do the thing this class does
     * todo, a return value or exception that indicates if the content is infected
     */
    public void scan(String scanEngineHostname, int scanEnginePort, byte[] stuffToScan, String filename) throws IOException, UnknownHostException {
        String get = "GET http://scapi.symantec.com/" + filename + " HTTP/1.1\n\n";
        String header = "HTTP/1.1 200 OK\n" +
                        "Transfer-Encoding: chunked\n\n";

        long bodylength = stuffToScan.length;

        int reshdr = get.length();
        int resbodylngt = reshdr + header.length();

        String icapRequest = "OPTIONS icap://savse.com/avscan ICAP/1.0\n\n" +
                             "RESPMOD icap://127.0.0.1:7777/AVSCAN?action=SCAN ICAP/1.0\n" +
                             "Host: 127.0.0.1:7777\n" +
                             "Allow: 204\n" +
                             "Encapsulated: req-hdr=0, res-hdr=" + reshdr + ", res-body=" + resbodylngt + "\n\n" +
                             get + header + Long.toHexString(bodylength) + "\n";

        Socket socket = new Socket("localhost", 7777);
        socket.getOutputStream().write(icapRequest.getBytes());
        socket.getOutputStream().write(stuffToScan);
        socket.getOutputStream().write("\n0".getBytes());

        byte[] returnedfromsav = new byte[2048];
        int read = socket.getInputStream().read(returnedfromsav);
        if (read > 0) {
            System.out.println(new String(returnedfromsav, 0, read));
        }
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        SymantecAntivirusScanEngineClient me = new SymantecAntivirusScanEngineClient();
        String something = "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblahblahblahblah" +
                           "blahblahblahblahblahblahblahblah";
        String eicarVirusTest = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        me.scan("localhost", 7777, something.getBytes(), "blah94");
    }
}
