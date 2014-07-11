package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.external.assertions.websocket.console.WebSocketNumberFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.util.HexUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketMessage {
    protected static final Logger logger = Logger.getLogger(WebSocketMessage.class.getName());

    private String TYPE;
    public final static String TEXT_TYPE = "TEXT";
    public final static String BINARY_TYPE = "BINARY";

    private Document document;
    private String id;
    private String clientId;
    private int offset;
    private int length;
    private byte[] payload;
    private String origin;
    private String protocol;
    private String status;
    private AuthenticationContext context;

    private void initialize() {
        document = null;
        clientId = null;
        payload = null;
        TYPE = null;
        origin = null;
        protocol = null;
        status = AssertionStatus.NONE.getMessage();
    }

    public WebSocketMessage(String message) throws Exception {
        initialize();
        setPayload(message);
    }

    public WebSocketMessage(byte[] message, int offset, int length) throws Exception {
        initialize();
        setPayload(message, offset, length);
    }

    public void setId(String id) {
        this.id = id;
        try {
            updateDocument();
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Unable to set id");
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        try {
            updateDocument();
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Unable to set client id");
        }
    }

    public AuthenticationContext getAuthCtx() {
        return context;
    }

    public void setAuthCtx(AuthenticationContext context) {
        this.context = context;
    }

    public String getType() {
        return TYPE;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
        try {
            updateDocument();
        } catch (SAXException e) {
           logger.log(Level.WARNING, "Unable to set protocol");
        }
    }

    public void setOrigin(String origin) {
        this.origin = origin;
        try {
            updateDocument();
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Unable to set origin");
        }
    }

    public String getOrigin() {
        return origin;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    private void createDocument() throws SAXException {
        final String template = "<request><websocket><id></id><clientId></clientId><type></type><origin></origin><protocol></protocol><offset></offset><length></length><data></data></websocket></request>";
        document = XmlUtil.parse(template);
        updateDocument();
    }

    private void updateDocument() throws  SAXException {
        document.getElementsByTagName("id").item(0).setTextContent(id);
        document.getElementsByTagName("clientId").item(0).setTextContent(clientId);
        document.getElementsByTagName("type").item(0).setTextContent(TYPE);
        document.getElementsByTagName("offset").item(0).setTextContent(String.valueOf(offset));
        document.getElementsByTagName("origin").item(0).setTextContent(origin);
        document.getElementsByTagName("protocol").item(0).setTextContent(protocol);
        document.getElementsByTagName("length").item(0).setTextContent(String.valueOf(length));
        if (TYPE.equals(BINARY_TYPE)) {
            document.getElementsByTagName("data").item(0).setTextContent(HexUtils.encodeBase64(payload));
        }
        if (TYPE.equals(TEXT_TYPE)) {
            document.getElementsByTagName("data").item(0).setTextContent(new String(payload));
        }
    }

    private void setPayload(byte[] message, int offset, int length) throws Exception {
        TYPE = BINARY_TYPE;
        payload = message;
        this.offset = offset;
        this.length = length;
        createDocument();
    }

    private void setPayload(String message) throws Exception {
        TYPE = TEXT_TYPE;
        payload = message.getBytes();
        createDocument();
    }

    private String getMessageAsString() {
        return new String(payload);
    }

    public String getPayloadAsString() throws WebSocketInvalidTypeException {
        if (!TYPE.equals(TEXT_TYPE)) {
            logger.log(Level.WARNING, "Attempting to get a binary message as string");
            throw new WebSocketInvalidTypeException("Message Type is incorrect for this method");
        }
        return getMessageAsString();
    }

    public byte[] getPayloadAsBytes() throws WebSocketInvalidTypeException {
        if (!TYPE.equals(BINARY_TYPE)) {
            logger.log(Level.WARNING, "Attempting to get a string message as binary");
            throw new WebSocketInvalidTypeException("Message Type is incorrect for this method");
        }
        return payload;
    }

    public void setPayload(Document document) {
        initialize();
        this.document = document;
        try {
            parsePayload(this.document);
        } catch (WebSocketNumberFormatException e) {
            logger.log(Level.WARNING, "Invalid message input: NumberFormatException");
        }
    }

    public Document getMessageAsDocument() {
        return document;
    }

    private void parsePayload(Document document) throws WebSocketNumberFormatException {
        //Determine type
        id = document.getElementsByTagName("id").item(0).getTextContent();
        clientId = document.getElementsByTagName("clientId").item(0).getTextContent();
        String type = document.getElementsByTagName("type").item(0).getTextContent();
        origin = document.getElementsByTagName("origin").item(0).getTextContent();
        protocol = document.getElementsByTagName("protocol").item(0).getTextContent();
        if (type.equals(BINARY_TYPE)) {
            TYPE = BINARY_TYPE;
            offset = WebSocketUtils.isInt(document.getElementsByTagName("offset").item(0).getTextContent(), "");
            length = WebSocketUtils.isInt(document.getElementsByTagName("length").item(0).getTextContent(), "");
            payload = HexUtils.decodeBase64(document.getElementsByTagName("data").item(0).getTextContent());
        } else {
            TYPE = TEXT_TYPE;
            payload = document.getElementsByTagName("data").item(0).getTextContent().getBytes();
        }

    }

}
