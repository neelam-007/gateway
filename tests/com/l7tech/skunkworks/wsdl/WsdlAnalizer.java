package com.l7tech.skunkworks.wsdl;

import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Parses a wsdl into input and output schema elements.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 13, 2004<br/>
 * $Id$<br/>
 */
public class WsdlAnalizer {
    public static void main(String[] args) throws Exception {
        WsdlAnalizer me = new WsdlAnalizer(getWsdlSample());
        me.parseInputOutputs();
    }

    private WsdlAnalizer(Document wsdl) {
        this.wsdl = wsdl;
    }

    public void parseInputOutputs() throws Exception {
        NodeList inputlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
        for (int i = 0; i < inputlist.getLength(); i++) {
            Element item = (Element)inputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                System.out.println("\nINPUT: " + msg);
                Collection schemaElements = getMessagePartsElementNames(msg);
                for (Iterator iterator = schemaElements.iterator(); iterator.hasNext();) {
                    QName qn = (QName) iterator.next();
                    System.out.println("\twith schema element: " + qn.getNamespaceURI() + " : " + qn.getLocalPart());
                }
            }
        }

        NodeList outputlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
        for (int i = 0; i < outputlist.getLength(); i++) {
            Element item = (Element)outputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                System.out.println("\nOUTPUT: " + msg);
                Collection schemaElements = getMessagePartsElementNames(msg);
                for (Iterator iterator = schemaElements.iterator(); iterator.hasNext();) {
                    QName qn = (QName) iterator.next();
                    System.out.println("\twith schema element: " + qn.getNamespaceURI() + " : " + qn.getLocalPart());
                }
            }
        }
    }

    public static Document getWsdlSample() throws Exception {
        return XmlUtil.getDocumentBuilder().parse(getRes(WAREHOUSE_WSDL_PATH));
    }

    private static InputSource getRes(String path) throws IOException {
        InputStream is = WsdlAnalizer.class.getResourceAsStream(path);
        if (is == null) {
            throw new IOException("\ncannot load resource " + path + ".\ncheck your runtime properties.\n");
        }
        return new InputSource(is);
    }

    private Collection getMessagePartsElementNames(String messageName) {
        if (messages == null) {
            messages = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "message");
        }
        String msgnamewoutprefix = messageName;
        int prefixendpos = messageName.indexOf(':');
        if (prefixendpos != -1) {
            msgnamewoutprefix = messageName.substring(prefixendpos+1);
        }
        ArrayList output = new ArrayList();
        // find messages elements that fit the name
        for (int i = 0; i < messages.getLength(); i++) {
            Element item = (Element)messages.item(i);
            String msgname = item.getAttribute("name");
            if (msgname == null) continue;
            if (msgname.equals(messageName) || msgname.equals(msgnamewoutprefix)) {
                // add all parts elements
                List parts = XmlUtil.findChildElementsByName(item, "http://schemas.xmlsoap.org/wsdl/", "part");
                for (Iterator iterator = parts.iterator(); iterator.hasNext();) {
                    Element part = (Element) iterator.next();
                    String elementname = part.getAttribute("element");
                    if (elementname != null && elementname.length() > 0) {
                        output.add(qnameFromElementName(elementname, part));
                    }
                }
            }
        }
        return output;
    }

    private QName qnameFromElementName(String elementname, Element elWhereDefined) {
        int prefixendpos = elementname.indexOf(':');
        if (prefixendpos != -1) {
            String prefix = elementname.substring(0, prefixendpos);
            String suffix = elementname.substring(prefixendpos+1);
            String ns = null;
            Element potentialNsDefiner = elWhereDefined;
            while (ns == null && potentialNsDefiner != null) {
                String attr = potentialNsDefiner.getAttribute("xmlns:"+prefix);
                if (attr != null && attr.length() > 0) {
                    ns = attr;
                    break;
                } else {
                    potentialNsDefiner = (Element)potentialNsDefiner.getParentNode();
                }
            }
            return new QName(ns, suffix);
        } else {
            return new QName(null, elementname);
        }
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
    private static final String WAREHOUSE_WSDL_PATH = RESOURCE_PATH + "warehouse.wsdl";
    private Document wsdl;
    private NodeList messages = null;
}
