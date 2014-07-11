package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/15/11
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMLStreamContentHandler implements ContentHandler {
    private XMLStreamCodecConfiguration configuration;
    private boolean inbound;

    private IoBuffer in;
    private int startPosition;
    private IoSession session;

    private ByteArrayOutputStream bufferStream;

    private SessionHandler sessionHandler;
    private TokenHandler tokenHandler;

    private Stack<String> elements = new Stack<String>();
    private boolean done = false;

    public XMLStreamContentHandler(IoSession session,
                                   XMLStreamCodecConfiguration configuration,
                                   boolean inbound,
                                   SessionHandler sessionHandler)
    {
        bufferStream = new ByteArrayOutputStream();
        this.session = session;
        this.configuration = configuration;
        this.inbound = inbound;
        this.sessionHandler = sessionHandler;
    }

    public void setTokenHandler(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    public void processingInstruction(String target, String data) {
        //
    }

    public void setDocumentLocator(Locator locator) {
        //
    }

    public void startDocument() {
        //
    }

    public void endDocument() {
        if(sessionHandler != null && !inbound) {
            sessionHandler.sessionTerminated();
        }
    }

    public void characters(char[] ch, int start, int length) {
        //
    }

    public void ignorableWhitespace(char[] ch , int start, int length) {
        //
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) {
        if((configuration.getResetElementNamespace() == null && uri == null ||
                configuration.getResetElementNamespace() != null &&
                        configuration.getResetElementNamespace().equals(uri)) &&
                localName.equals(configuration.getResetElement()))
        {
            elements.clear();
            tokenHandler.reset();
        }

        for(StanzaToProcessRule rule : configuration.getStanzaProcessingRules()) {
            if(rule.processStartElement(elements, uri, localName, qName, atts)) {
                sendData();
                break;
            }
        }

        elements.push(qName);
    }

    public void endElement(String uri, String localName, String qName) {
        String removedQName = elements.pop();
        if(!removedQName.equals(qName)) {
            elements.push(removedQName);
        }

        for(StanzaToProcessRule rule : configuration.getStanzaProcessingRules()) {
            if(rule.processEndElement(elements, uri, localName, qName)) {
                sendData();
                break;
            }
        }
    }

    private void sendData() {
        int currentPosition = in.position();
        byte[] bytes = new byte[currentPosition - startPosition];

        in.position(startPosition);
        in.get(bytes);
        in.position(currentPosition);

        startPosition = currentPosition + 1;

        try {
            bufferStream.write(bytes);
        } catch(IOException e) {
            //
        }

        sessionHandler.addDataFragment(bufferStream.toByteArray());
        reset(in);
        //out.write(bufferStream.toByteArray());
        done = true;

        bufferStream = new ByteArrayOutputStream();
    }

    //public void sendEmptyMessage() {
    //    out.write(new byte[0]);
    //}

    public void endPrefixMapping(String prefix) {
        //
    }

    public void skippedEntity(String name) {
        //
    }

    public void startPrefixMapping(String prefix, String uri) {
        //
    }

    public void reset(IoBuffer in/*, ProtocolDecoderOutput out*/) {
        this.in = in;
        startPosition = in.position();
        //this.out = out;
        done = false;
    }

    public boolean storeUnwrittenData() {
        if(in != null && startPosition < in.position()) {
            int currentPosition = in.position();
            byte[] bytes = new byte[currentPosition - startPosition];

            in.position(startPosition);
            in.get(bytes);

            try {
                bufferStream.write(bytes);
            } catch(IOException e) {
                //
            }

            return false;
        } else {
            return true;
        }
    }

    public boolean isDone() {
        return done;
    }
}
