package com.l7tech.skunkworks;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;

/**
 * Test class that can generate Wssx Interop Test messages.
 */
public class WssxInteropMessageGenerator {
    private String scenario;
    private WssDecorator.DecorationResult dresult;
    private Message message;
    DecorationRequirements dreq = new DecorationRequirements();

    public WssxInteropMessageGenerator() {
        reset();
    }

    public void reset() {
        scenario = "WS-SX Interop Test";
        message = null;
        dresult = null;
        dreq = new DecorationRequirements();
        dreq.setSecurityHeaderActor(null);
    }

    public Document generateRequest() throws IOException, SAXException, DecoratorException, InvalidDocumentFormatException, GeneralSecurityException {
        message = new Message(XmlUtil.stringToDocument(MessageFormat.format(PLAINTEXT_MESS, scenario)));
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        dresult = decorator.decorateMessage(message, dreq);
        return message.getXmlKnob().getDocumentWritable();
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public DecorationRequirements dreq() {
        return dreq;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS, "true");
        WssxInteropMessageGenerator generator = new WssxInteropMessageGenerator();
        Document doc = generator.generateRequest();
        System.out.println(XmlUtil.nodeToFormattedString(doc));
    }

    private static final String PLAINTEXT_MESS = "" +
            "<s12:Envelope xmlns:s12=\"http://www.w3.org/2003/05/soap-envelope\">" +
            "    <s12:Body>\n" +
            "        <tns:EchoRequest xmlns:tns=\"http://example.com/ws/2008/09/securitypolicy\">{0}</tns:EchoRequest>\n" +
            "    </s12:Body>\n" +
            "</s12:Envelope>";
}