package com.l7tech.message;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Enum that captures which parts of a message / documents are schema-validated.
 */
public enum ValidationTarget {

    /**
     * Validate the full SOAP message including the SOAP envelope,
     * or the full XML message if applied to a non-SOAP message.
     */
    ENVELOPE(true) {
        @Override
        public Element[] elementsToValidate(Document doc) {
            return new Element[]{doc.getDocumentElement()};
        }
    },

    /**
     * Validate the SOAP body's contents,
     * or the full XML message if applied to a non-SOAP message.
     */
    BODY(true) {
        @Override
        public Element[] elementsToValidate(Document doc) throws InvalidDocumentFormatException {
            if (SoapUtil.isSoapMessage(doc)) {
                return getBodyChildren(doc);
            } else {
                return new Element[]{doc.getDocumentElement()};
            }
        }
    },

    /**
     * Validate the SOAP arguments,
     * or the full XML message if applied to a non-SOAP message.
     */
    ARGUMENTS(false) {
        @Override
        public Element[] elementsToValidate(Document doc) throws InvalidDocumentFormatException {
            if (SoapUtil.isSoapMessage(doc)) {
                return getBodyArguments(doc);
            } else {
                return new Element[]{doc.getDocumentElement()};
            }
        }
    },

    STREAM( false, true ) {
        @Override
        public Element[] elementsToValidate( Document doc ) throws InvalidDocumentFormatException {
            throw new IllegalStateException( "Should not have tried to use DOM-based validation for streaming validation request" );
        }
    }
    ;

    public boolean isAttemptHardware() {
        return attemptHardware;
    }

    public boolean isUseStreamingMode() {
        return useStreamingMode;
    }

    /**
     * Given a Document, returns an array of Elements that the ValidationTarget requires to be schema-validated.
     */
    public abstract Element[] elementsToValidate(Document doc) throws InvalidDocumentFormatException;

    // - PRIVATE

    private boolean attemptHardware;
    private boolean useStreamingMode;

    private ValidationTarget(boolean attemptHardware) {
        this( attemptHardware, false );
    }

    private ValidationTarget( boolean attemptHardware, boolean useStreamingMode ) {
        this.attemptHardware = attemptHardware;
        this.useStreamingMode = useStreamingMode;
    }

    private static Element[] getBodyChildren(Document doc) throws InvalidDocumentFormatException {
        Element bodyElement = SoapUtil.getBodyElement(doc);
        NodeList bodyChildren = bodyElement.getChildNodes();
        ArrayList<Element> children = new ArrayList<Element>();
        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node child = bodyChildren.item(i);
            if (child instanceof Element) {
                children.add((Element) child);
            }
        }
        Element[] bodyChildrenElements = new Element[children.size()];
        int cnt = 0;
        for (Iterator i = children.iterator(); i.hasNext(); cnt++) {
            bodyChildrenElements[cnt] = (Element) i.next();
        }
        return bodyChildrenElements;
    }

    /**
     * Goes one level deeper than getRequestBodyChild
     */
    private static Element[] getBodyArguments(Document soapMessage) throws InvalidDocumentFormatException {
        // first, get the body
        final Element bodyElement = SoapUtil.getBodyElement(soapMessage);
        // then, get the body's first child element
        final Element bodyFirstElement = bodyElement == null ? null : XmlUtil.findFirstChildElement(bodyElement);
        if (bodyFirstElement == null) {
            throw new InvalidDocumentFormatException("The soap body does not have a child element as expected");
        }
        // construct a return output for each element under the body first child
        NodeList maybeArguments = bodyFirstElement.getChildNodes();
        ArrayList<Element> argumentList = new ArrayList<Element>();
        for (int i = 0; i < maybeArguments.getLength(); i++) {
            Node child = maybeArguments.item(i);
            if (child instanceof Element) {
                argumentList.add((Element) child);
            }
        }
        Element[] output = new Element[argumentList.size()];
        int cnt = 0;
        for (Iterator i = argumentList.iterator(); i.hasNext(); cnt++) {
            output[cnt] = (Element) i.next();
        }
        return output;
    }
}

