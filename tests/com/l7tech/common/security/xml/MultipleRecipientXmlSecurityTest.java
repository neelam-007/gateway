/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 26, 2005<br/>
 */
package com.l7tech.common.security.xml;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests decoration and processing of soap messages secured for multiple recipients.
 *
 * @author flascelles@layer7-tech.com
 */
public class MultipleRecipientXmlSecurityTest extends TestCase {
    private static Logger logger = Logger.getLogger(MultipleRecipientXmlSecurityTest.class.getName());

    static {
        JceProvider.init();
    }

    public MultipleRecipientXmlSecurityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MultipleRecipientXmlSecurityTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testBodySignedForTwoRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");
        DecorationRequirements req = defaultDecorationRequirements(doc);
        WssDecorator decorator = new WssDecoratorImpl();
        decorator.decorateMessage(doc, req);
        req = otherDecorationRequirements(doc, "downstream");
        decorator.decorateMessage(doc, req);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");
    }

    private DecorationRequirements defaultDecorationRequirements(Document doc) throws Exception {
        DecorationRequirements output = new DecorationRequirements();
        output.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        output.setSenderCertificate(TestDocuments.getFrancoCertificate());
        output.setSenderPrivateKey(TestDocuments.getFrancoPrivateKey());
        output.setSignTimestamp(true);
        Element body = SoapUtil.getBodyElement(doc);
        output.getElementsToSign().add(body);
        return output;
    }

    private DecorationRequirements otherDecorationRequirements(Document doc, String actor) throws Exception {
        DecorationRequirements output = new DecorationRequirements();
        output.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        output.setSenderCertificate(TestDocuments.getFrancoCertificate());
        output.setSenderPrivateKey(TestDocuments.getFrancoPrivateKey());
        output.setSignTimestamp(true);
        Element body = SoapUtil.getBodyElement(doc);
        output.getElementsToSign().add(body);
        output.setSecurityHeaderActor(actor);
        return output;
    }
}
