/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Feb 16, 2005<br/>
 */
package com.l7tech.skunkworks;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.server.ServerConfig;

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
 * @author flascell<br/>
 */
public class SymantecAntivirusScanEngineClient {
    private static final Logger logger =  Logger.getLogger(SymantecAntivirusScanEngineClient.class.getName());
    private static final String SCAN_REQ = "RESPMOD icap://{0}:{1}/AVSCAN?action=SCAN ICAP/1.0\r\n";
    private static final String DEF_HEADER = "Content-Type: application/octet-stream\r\n\r\n";
    private String scannerHostName;
    private Integer scannerPort;

    /**
     * Send content to scan using ICAP and read response from sav scan engine.
     */
    private String getSavseResponse(String scanEngineHostname, int scanEnginePort, byte[] headers, byte[] payload, String filename) throws IOException, UnknownHostException {
        String get = "GET http://scapi.symantec.com/" + filename + " HTTP/1.0\r\n\r\n";

        String hdr;
        if (headers == null || headers.length < 1) {
            // we need some generic header
            hdr = DEF_HEADER;
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
        int read = readFromSocket(socket, returnedfromsav, 250);
        socket.close();
        if (read <= 0) {
            throw new IOException("server did not return anything");
        }
        String output = new String(returnedfromsav, 0, read);
        logger.fine("Response from sav scan engine:\n" + output);
        return output;
    }

    /**
     * The sav scan engine will not close the socket when it is done transmitting response. To ensure that
     * the entire response is sent back, reading from the server involves a timeout.
     */
    private int readFromSocket(Socket s, byte[] buffer, long timeout) throws IOException {
        int read = 0;
        int offset = 0;
        InputStream stream = s.getInputStream();
        do {
            if (stream.available() <= 0) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    String msg = "Reading from socket interupted";
                    logger.log(Level.SEVERE, msg, e);
                    throw new IOException(msg + e.getMessage());
                }
            }
            if (stream.available() <= 0) break;
            read = stream.read(buffer, offset, buffer.length-offset);
            offset += read;
        } while (read > 0);
        return offset;
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
            throw new RuntimeException("unexpected response format. " + parsedResponse); // todo, special exception type
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
        String body;
        public String toString() {
            StringBuffer output = new StringBuffer();
            output.append("Response from Scan Engine\n");
            output.append("\tStatus: " + statusString + "\n");
            String str = (String)headers.get("X-Infection-Found");
            if (str != null) {
                output.append("\tX-Infection-Found: " + str + "\n");
            }
            str = (String)headers.get("X-Violations-Found");
            if (str != null) {
                output.append("\tX-Violations-Found: " + str + "\n");
            }
            if (body != null) {
                output.append("\tBody:" + body + "\n");
            }
            return output.toString();
        }
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
            throw new RuntimeException("unexpected response format. " + response); // todo, a special exception type
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
            logger.warning("Unexpected response\n" + response);
            throw new RuntimeException("unexpected response format. " + response); // todo, a special exception type
        }
        int endofheaders = response.indexOf("\r\n\r\n", startofheaders);
        if (endofheaders < startofheaders) {
            endofheaders = response.indexOf("\n\n", startofheaders);
        }
        if (endofheaders < startofheaders) {
            logger.warning("there are no headers");
        }
        output.headers = parseHeaders(response, startofheaders, endofheaders);
        if ((response.length()+4) > endofheaders) {
            output.body = response.substring(endofheaders+4);
        }
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

    public String getSavScanEngineOptions() throws IOException, UnknownHostException {
        String req = "OPTIONS icap://savse.com/avscan ICAP/1.0\r\n\r\n";
        Socket socket = new Socket(scannerHostName(), scannerPort());
        socket.getOutputStream().write(req.getBytes());
        byte[] returnedfromsav = new byte[4096];
        int read = readFromSocket(socket, returnedfromsav, 250);
        socket.close();
        if (read <= 0) {
            throw new IOException("server did not return anything");
        }
        return new String(returnedfromsav, 0, read);
    }

    private synchronized String scannerHostName() {
        if (scannerHostName == null) {
            scannerHostName = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ANTIVIRUS_HOST);
            if (scannerHostName == null) {
                // todo a special exception type for this type of event
                throw new IllegalStateException("The property " + ServerConfig.PARAM_ANTIVIRUS_HOST +
                                                " cannot be retrieved. This gateway is not configured properly.");
            }
        }
        return scannerHostName;
    }

    private synchronized int scannerPort() {
        if (scannerPort == null) {
            String tmp = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ANTIVIRUS_PORT);
            if (tmp == null) {
                // todo a special exception type for this type of event
                throw new IllegalStateException("The property " + ServerConfig.PARAM_ANTIVIRUS_PORT +
                                                " cannot be retrieved. This gateway is not configured properly.");
            } else {
                scannerPort = new Integer(tmp);
            }
        }
        return scannerPort.intValue();
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
