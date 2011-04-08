package com.l7tech.external.assertions.sophos.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 6-Jan-2011
 * Time: 3:45:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class SsspClient {
    public static class ScanResult {
        private boolean clean;
        private String virusName;
        private String virusType;
        private String virusLocation;
        private String disinfectable;
        
        public ScanResult(boolean clean, String virusName) {
            this.clean = clean;
            this.virusName = virusName;
        }

        public ScanResult(boolean clean, String virusName, String virusType, String virusLocation, String disinfectable) {
            this.clean = clean;
            this.virusName = virusName;
            this.virusType = virusType;
            this.virusLocation = virusLocation;
            this.disinfectable = disinfectable;
        }

        public boolean isClean() {
            return clean;
        }

        public String getVirusName() {
            return virusName;
        }
        public String getVirusType() {
            return virusType;
        }
        public String getVirusLocation() {
            return virusLocation;
        }
        public String getDisinfectable() {
            return disinfectable;
        }
    }

    public SsspClient(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void connect(int connectTimeout, int readTimeout) throws IOException {
        InetSocketAddress inetAddr =  new InetSocketAddress(address, port);
        saviSocket = new Socket();
        saviSocket.connect(inetAddr, connectTimeout);

        saviSocket.setSoTimeout(readTimeout);
        reader = new BufferedReader( new InputStreamReader(saviSocket.getInputStream()));
        writer = new OutputStreamWriter(saviSocket.getOutputStream());

        try {
            String r = reader.readLine(); //Access Message "OK SSSP/1.0" read
            if(!("OK SSSP/1.0").equals(r)) {
                throw new IOException("Unexpected SAVI server greeting.");
            }

            writer.write("SSSP/1.0\n");
            writer.flush();
            r = reader.readLine(); //ACC read

            if(!r.startsWith("ACC ")) {
                throw new IOException("Unexpected SAVI server response to protocol command.");
            }
        } catch(IOException ioe) {
            close();
            throw ioe;
        }
    }

    public boolean isConnected() {
        return saviSocket != null && saviSocket.isConnected();
    }

    public void close() throws IOException {
        reader.close();
        writer.close();
        saviSocket.close();
    }

    /*
     * after communication with Sophos has ended
     * close session with sophos by a "BYE" handshake (refer to Sophos docs)
     * @throws IOException
     */
    public void closeSophosSession() throws IOException {
        writer.write("BYE\n");
        writer.flush();
        String r = reader.readLine(); //BYE read
        if(r.length() != 0 &&!r.startsWith("BYE ")) {
            throw new IOException("Unexpected SAVI server response when ending session(BYE write) with sophos. Throwing Exception...");
        }
    }

    public void setOption(String name, String value) throws IOException {
        writer.write("OPTIONS\n");
        writer.write(name);
        writer.write(": ");
        writer.write(value);
        writer.write("\n\n");
        writer.flush();
        String r = reader.readLine(); //ACC read

        if(r.length() != 0 &&!r.startsWith("ACC ")) {
            throw new IOException("Unexpected SAVI server response to set options command.");
        }

        StringBuilder sb = new StringBuilder();
        r = reader.readLine();
        while(r.length() > 0) {
            sb.append(r);
            r = reader.readLine();
        }

        String statusResponse = sb.toString();
        if(statusResponse.startsWith("<done ")) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder parser = factory.newDocumentBuilder();

                Document resultDoc = parser.parse(new ByteArrayInputStream(statusResponse.getBytes()));
                Element doneElement = resultDoc.getDocumentElement();

                if(!doneElement.getAttribute("result").equals("0000") || !doneElement.getAttribute("status").equals("OK")) {
                    throw new IOException("Unexpected SAVI server response to set options command.");
                }
            } catch(SAXException saxe) {
                throw new IOException(saxe);
            } catch(ParserConfigurationException pce) {
                throw new IOException(pce);
            }
        } else if(!statusResponse.startsWith("DONE OK 0000 ")) {
            throw new IOException("Unexpected SAVI server response to set options command.");
        }
    }

    public void scanData(long length, InputStream is) throws IOException {
        writer.write("SCANDATA " + length + "\n");
        writer.flush();
        String r = reader.readLine(); //ACC read

        if(r.length() != 0 &&!r.startsWith("ACC ")) {
            throw new IOException("Unexpected SAVI server response to set options command.");
        }

        byte[] buffer = new byte[4096];
        while(true) {
            int bytesRead = is.read(buffer);
            if(bytesRead == -1) {
                break;
            }
            saviSocket.getOutputStream().write(buffer, 0, bytesRead);
        }
        saviSocket.getOutputStream().flush();

        boolean done = false;
        StringBuilder sb = new StringBuilder();
        boolean resultsStarted = false;
        while (! done) {
            r = reader.readLine();

            if(r.length() == 0) {
                try {
                    Thread.sleep(500);
                } catch(Exception e) {}
            }

            if(r.startsWith("<results>")) {
                resultsStarted = true;
            }

            if(resultsStarted) {
                sb.append(r);
                sb.append('\n');
            }

            if(r.startsWith("</results>")) {
                done = true;
            }
        }

        scanResponse = sb.toString();



//        reader.close();
//        writer.close();
//        saviSocket.close();
    }

    public ScanResult getScanResult() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();

            Document resultDoc = parser.parse(new ByteArrayInputStream(scanResponse.getBytes()));
            NodeList nodes = resultDoc.getElementsByTagName("done");

            if(nodes.getLength() > 0) {
                Element doneEl = (Element)nodes.item(0);

                if(!"0000".equals(doneEl.getAttribute("result")) && !"021E".equals(doneEl.getAttribute("result"))) {
                    nodes = resultDoc.getElementsByTagName("sweepresults");
                    if(nodes.getLength() == 0) {
                        return new ScanResult(false, "Unknown");
                    }
                    Element parentEl = (Element)nodes.item(0);

                    nodes = parentEl.getElementsByTagName("sweepresult");
                    if(nodes.getLength() == 0) {
                        return new ScanResult(false, "Unknown");
                    }

                    parentEl = (Element)nodes.item(0);

                    nodes = parentEl.getElementsByTagName("name");
                    String virusNameStr ="Unknown";
                    if(nodes.getLength() > 0) {
                        virusNameStr = ((Element)nodes.item(0)).getTextContent();
                    }

                    nodes = parentEl.getElementsByTagName("virustype");
                    String virusTypeStr ="Unknown";
                    if(nodes.getLength() > 0) {
                        virusTypeStr = ((Element)nodes.item(0)).getTextContent();
                    }

                    String locationStr ="Unknown";
                    nodes = parentEl.getElementsByTagName("location");
                    if(nodes.getLength() > 0) {
                        locationStr = ((Element)nodes.item(0)).getTextContent();
                    }

                    String disinfectableStr ="Unknown";
                    nodes = parentEl.getElementsByTagName("disinfectable");
                    if(nodes.getLength() > 0) {
                        disinfectableStr = ((Element)nodes.item(0)).getTextContent();
                    }
                    return new ScanResult(false, virusNameStr, virusTypeStr, locationStr, disinfectableStr);

                } else {
                    return new ScanResult(true, null);
                }
            } else {
                return new ScanResult(false, "Unknown");
            }
        } catch(SAXException saxe) {
            throw new IOException(saxe);
        } catch(ParserConfigurationException pce) {
            throw new IOException(pce);
        }

    }

    private String address;
    private int port;
    private Socket saviSocket;
    private OutputStreamWriter writer;
    private BufferedReader reader;
    private String scanResponse;

    private static final Logger logger = Logger.getLogger(SsspClient.class.getName());
}
