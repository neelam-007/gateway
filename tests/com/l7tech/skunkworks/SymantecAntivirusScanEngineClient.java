package com.l7tech.skunkworks;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

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
    private static final String SCAN_REQ = "RESPMOD icap://{0}:{1}/AVSCAN?action=SCAN ICAP/1.0\r\n";
    public static final String DEF_HEADER = "Content-Type: application/octet-stream\r\n\r\n";

    /**
     * Send content to scan using ICAP and read response from sav scan engine.
     */
    private String getSavseResponse(String scanEngineHostname, int scanEnginePort, byte[] headers, byte[] payload, String filename) throws IOException, UnknownHostException {
        String get = "GET http://scapi.symantec.com/" + filename + " HTTP/1.0\r\n\r\n";

        String hdr;
        if (headers == null || headers.length < 1) {
            // we need some generic header
            hdr = "Content-Type: text/plain\r\n\r\n";
        } else {
            String tmp = new String(headers);
            if (tmp.endsWith("\r\n\r\n")) {
                hdr = tmp;
            } else {
                hdr = tmp + "\r\n\r\n";
            }
        }

        long bodylength = payload.length;

        int reshdr = get.length();
        int resbodylngt = reshdr + hdr.length();

        String icapRequest = "Host: " + scanEngineHostname + ":" + scanEnginePort + "\r\n" +
                             "Allow: 204\r\n" +
                             "Encapsulated: req-hdr=0, res-hdr=" + reshdr + ", res-body=" + resbodylngt + "\r\n\r\n" +
                             get + hdr + Long.toHexString(bodylength) + "\r\n";

        Socket socket = new Socket(scanEngineHostname, scanEnginePort);
        socket.getOutputStream().write(new MessageFormat(SCAN_REQ).format(new Object[]{scanEngineHostname,
                                                                                       ""+scanEnginePort}).getBytes());
        socket.getOutputStream().write(icapRequest.getBytes());
        socket.getOutputStream().write(payload);
        socket.getOutputStream().write("\r\n0\r\n\r\n".getBytes());

        byte[] returnedfromsav = new byte[4096];
        int read = 0;
        read = socket.getInputStream().read(returnedfromsav);
        StringBuffer output = new StringBuffer();
        while (read > 0) {
            String tmp = new String(returnedfromsav, 0, read);
            output.append(tmp);
            // are we done? (the server does not close the socket when it
            // is done so we have to be careful to not block current thread
            // by simply reading from socket)
            if (socket.getInputStream().available() <= 0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Reading of response from server interupted", e);
                    throw new RuntimeException(e);
                }
            }

            if (socket.getInputStream().available() > 0) {
                read = socket.getInputStream().read(returnedfromsav);
            } else {
                read = 0;
            }
        }
        socket.close();
        logger.fine("Response from sav scan engine:\n" + output.toString());
        return output.toString();
    }

    /**
     * Interpret a response from the sav scan engine (figure out whether the scan detected anything nasty).
     *
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
            // todo, some sort of 'maybe infected' return code
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
        // todo, some sort of 'maybe infected' return code
        return false;
    }

    public class SAVScanEngineResponse {
        String statusString;
        long statusCode;
        Map headers;
    }

    /**
     * parse a text response from the sav scan engine into its status, header parts and body
     * @param response
     * @return
     */
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

    /**
     * Parse header section from a http response
     * @param input raw input
     * @param start position of beggining of headers
     * @param end position of end of headers
     * @return a map with key for header name and value for header value
     */
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

    // todo, read this from some config file
    private String scannerHostName() {
        return "localhost";
    }

    // todo, read this from some config file
    private int scannerPort() {
        return 7777;
    }

    /**
     *
     * @param headers a byte array containing the http headers of the payload to scan
     * @param payload the actual payload to scan
     * @return the result of the scan
     */
    public SAVScanEngineResponse scan(byte[] headers, byte[] payload)  throws IOException, UnknownHostException {
        String res = getSavseResponse(scannerHostName(),
                                      scannerPort(),
                                      headers,
                                      payload,
                                      "ssgreq." + System.currentTimeMillis());

        if (res == null) {
            throw new IOException("cannot get response from savse server");
        }

        return parseResponse(res);
    }

    public SAVScanEngineResponse[] scan(Message msg)  throws IOException, UnknownHostException, NoSuchPartException {
        ArrayList res = new ArrayList();
        for (PartIterator i = msg.getMimeKnob().getParts(); i.hasNext();) {
            PartInfo pi = i.next();
            InputStream is = pi.getInputStream(false);
            String headers = "Content-Type: " + pi.getContentType().getFullValue() + "\r\n\r\n";
            res.add(scan(headers.getBytes(), HexUtils.slurpStream(is)));
        }
        return (SAVScanEngineResponse[])res.toArray(new SAVScanEngineResponse[res.size()]);
    }
}
