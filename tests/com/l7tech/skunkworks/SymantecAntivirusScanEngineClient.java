package com.l7tech.skunkworks;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
    private static final Logger logger =  Logger.getLogger(SymantecAntivirusScanEngineClient.class.getName());
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
     * get a raw response from the sav scan engine
     */
    private String getSavseResponse(String scanEngineHostname, int scanEnginePort, byte[] stuffToScan, String filename) throws IOException, UnknownHostException {
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
            // todo, handle this better
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            if (socket.getInputStream().available() <= 0) break;
            read = socket.getInputStream().read(returnedfromsav);
        }
        socket.close();
        return output.toString();
    }

    /**
     * The following headers typically indicate infection.
     *
     * X-Infection-Found: Type=0; Resolution=0; Threat=EICAR Test String;
     * X-Violations-Found: 1
     * 	blah2092302392
     * 	EICAR Test String
     * 	11101
     * 	0
     * @return true means savse is saying there is infection
     */
    public boolean savseResponseIndicateInfection(SAVScanEngineResponse parsedResponse) {
        if (parsedResponse.headers == null) {
            logger.warning("this parsed response does not contain headers, infection cannot be determined");
            // todo, some sort of maybe infected return code
        }
        String str = (String)parsedResponse.headers.get("X-Infection-Found");
        if (str != null && str.length() > 0) {
            logger.warning("Infection detected - X-Infection-Found: " + str);
            return true;
        }
        str = (String)parsedResponse.headers.get("X-Violations-Found");
        if (str != null && str.length() > 0) {
            logger.warning("Infection detected - X-Violations-Found: " + str);
            return true;
        }

        if (parsedResponse.statusCode == 204) {
            return false;
        }

        logger.warning("Unexpected return code: " + parsedResponse.statusString);
        // todo, some sort of maybe infected return code
        return false;
    }

    public class SAVScanEngineResponse {
        String statusString;
        long statusCode;
        Map headers;
    }

    private SAVScanEngineResponse parseResponse(String response) {
        SAVScanEngineResponse output = new SAVScanEngineResponse();
        // read status
        int statusstart = response.indexOf(' ');
        int rtnlpos = response.indexOf('\r');
        if (rtnlpos < 1) rtnlpos = response.indexOf('\n');
        if (rtnlpos < statusstart) {
            logger.warning("Unexpected response\n" + response);
            // todo, throw?
            return null;
        } else {
            String statusstr = response.substring(statusstart+1, rtnlpos);
            output.statusString = statusstr;
            String statusnr = statusstr;
            if (statusstr.indexOf(' ') > 1) {
                statusnr = statusstr.substring(0, statusstr.indexOf(' '));
            }
            long status = Long.parseLong(statusnr);
            output.statusCode = status;
        }

        // read headers
        int startofheaders = -1;
        for (int i = rtnlpos; i < response.length(); i++) {
            char c = response.charAt(i);
            if (c != '\r' && c != '\n') {
                startofheaders = i;
                break;
            }
        }
        if (startofheaders < rtnlpos) {
            // there are no headers
            // todo, throw?
            logger.warning("Unexpected response\n" + response);
            return null;
        }
        int endofheaders = response.indexOf("\r\n\r\n", startofheaders);
        if (endofheaders < startofheaders) {
            endofheaders = response.indexOf("\n\n", startofheaders);
        }
        if (endofheaders < startofheaders) {
            logger.warning("there are no headers");
        }
        output.headers = parseHeaders(response, startofheaders, endofheaders);
        return output;
    }

    private HashMap parseHeaders(String input, int start, int end) {
        HashMap output = new HashMap();

        int headerSeparPos = input.indexOf(':', start);
        String currentHeaderName = null;
        int previousSeparPos = -1;
        while (headerSeparPos > start && headerSeparPos < end) {
            int startofheadername = headerSeparPos;
            char c = input.charAt(startofheadername-1);
            while (c != '\n') {
                startofheadername--;
                c = input.charAt(startofheadername-1);
            }
            String newHeadername = input.substring(startofheadername, headerSeparPos);
            if (currentHeaderName != null) {
                int posOfEndOfPreviousValue = startofheadername-2;
                if (input.charAt(posOfEndOfPreviousValue) == '\r') {
                    posOfEndOfPreviousValue--;
                }
                int posOfBegginingOfPreviousValue = previousSeparPos+1;
                if (input.charAt(posOfBegginingOfPreviousValue) == ' ') {
                    posOfBegginingOfPreviousValue++;
                }
                String value = input.substring(posOfBegginingOfPreviousValue, posOfEndOfPreviousValue+1);
                logger.finest("Key: " + currentHeaderName + ", Value: " + value);
                output.put(currentHeaderName, value);
            }
            currentHeaderName = newHeadername;
            previousSeparPos = headerSeparPos;
            // get next separator
            int nextnl = input.indexOf('\n', previousSeparPos);
            if (nextnl < 0) break;
            headerSeparPos = input.indexOf(':', nextnl+1);
        }
        if (currentHeaderName != null) {
            int posOfBegginingOfPreviousValue = previousSeparPos+1;
            if (input.charAt(posOfBegginingOfPreviousValue) == ' ') {
                posOfBegginingOfPreviousValue++;
            }
            String value = input.substring(posOfBegginingOfPreviousValue, end);
            logger.finest("Key: " + currentHeaderName + ", Value: " + value);
            output.put(currentHeaderName, value);
        }

        return output;
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

    public SAVScanEngineResponse scan(byte[] contentToScan)  throws IOException, UnknownHostException {
        // todo, read hostname and port from some properties file
        // make up some random filename
        String res = getSavseResponse("phlox.l7tech.com", 7777, contentToScan, "blah2092302392");
        if (res == null) {
            throw new IOException("cannot get response from savse server");
        }
        return parseResponse(res);
    }

    public static void main(String[] args) throws Exception {
        SymantecAntivirusScanEngineClient me = new SymantecAntivirusScanEngineClient();
        //String res = me.getSavScanEngineOptions("phlox.l7tech.com", 7777);
        SAVScanEngineResponse res = me.scan(VIRUS);
        //SAVScanEngineResponse res = me.scan("blahblah".getBytes());
                if (me.savseResponseIndicateInfection(res)) {
            logger.severe("CONTENT IS INFECTED!");
        } else {
            logger.info("Content is clean");
        }

    }
}
