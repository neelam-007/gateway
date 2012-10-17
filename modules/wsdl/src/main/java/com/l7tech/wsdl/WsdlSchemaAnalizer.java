package com.l7tech.wsdl;

import com.l7tech.util.DomUtils;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Parses schemas present in a wsdl and split them into two versions; one for input messages and
 * one for output messages. The schema definition can be defined as nested or schema import from another file.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 17, 2004<br/>
 */
public class WsdlSchemaAnalizer {
    /**
     * @param wsdl The WSDL Document
     * @param docs Schema imported from the WSDL Document, the key should be the uri of the
     *             of the schema and the value should contain the content of the schema.
     */
    public WsdlSchemaAnalizer(Document wsdl, Map<String,Document> docs) {
        this.wsdl = wsdl;
        this.docs = docs;
        this.schemaElements = extractSchemaElementFromWsdl(wsdl, docs);
    }

    /**
     * This creates a list of schema elements that can only be present in requests and responses respectively.
     * Then, it makes two clones of the schemas and removes those elements in each of the clones to get as a result
     * schemas that are only applicable to requests and responses respectively.
     */
    public void splitInputOutputs() {
        if (schemaElements == null) return;
        NodeList inputlist = wsdl.getElementsByTagNameNS(WSDL_NS, "input");
        Collection intputQnames = new ArrayList();
        for (int i = 0; i < inputlist.getLength(); i++) {
            Element item = (Element)inputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                Collection schemaElementsForMsg = getMessagePartsElementNames(msg);
                intputQnames.addAll(schemaElementsForMsg);
            }
        }
        Collection outputQnames = new ArrayList();
        NodeList outputlist = wsdl.getElementsByTagNameNS(WSDL_NS, "output");
        for (int i = 0; i < outputlist.getLength(); i++) {
            Element item = (Element)outputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                Collection schemaElementsForMsg = getMessagePartsElementNames(msg);
                outputQnames.addAll(schemaElementsForMsg);
            }
        }
        ArrayList toberemovedfromoutputs = new ArrayList();
        for (Iterator iterator = intputQnames.iterator(); iterator.hasNext();) {
            QName qName = (QName) iterator.next();
            if (outputQnames.contains(qName)) {
                iterator.remove();
                toberemovedfromoutputs.add(qName);
            }
        }
        for (Iterator iterator = outputQnames.iterator(); iterator.hasNext();) {
            QName qName = (QName) iterator.next();
            if (intputQnames.contains(qName)) {
                iterator.remove();
            } else if (toberemovedfromoutputs.contains(qName)) {
                iterator.remove();
            }
        }
        ouputSchemas = removeQnamesFromSchemas(intputQnames);
        inputSchemas = removeQnamesFromSchemas(outputQnames);
    }

    private Collection getMessagePartsElementNames(String messageName) {
        if (messages == null) {
            messages = wsdl.getElementsByTagNameNS(WSDL_NS, "message");
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
                List parts = DomUtils.findChildElementsByName(item, WSDL_NS, "part");
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

    public static List<Element> extractSchemaElementFromWsdl(Document wsdl, Map<String,Document> docs)  {
        if (wsdl == null) return null;
        NodeList potentiallists = wsdl.getDocumentElement().getElementsByTagName(WSDL_TYPES_ELNAME);
        Element typesel = null;
        switch (potentiallists.getLength()) {
            case 1:
                typesel = (Element)potentiallists.item(0);
                break;
            default:
                break;
        }
        if (typesel == null) {
            potentiallists = wsdl.getDocumentElement().getElementsByTagNameNS(WSDL_NS,
                                                                              WSDL_TYPES_ELNAME);
            typesel = null;
            switch (potentiallists.getLength()) {
                case 1:
                    typesel = (Element)potentiallists.item(0);
                    break;
                default:
                    break;
            }
        }

        if (typesel == null) {
            return null;
        }
        //Extract the import schema elements
        potentiallists = typesel.getElementsByTagNameNS(W3C_XML_SCHEMA, IMPORT_ELNAME);
        
        List<Element> schemas = new ArrayList<Element>();

        //Extract the schema definition from the schemaLocation
        //The schemaLocation will be used for lookup the schema from the provided schema map
        if (potentiallists.getLength() > 0) {
            for (int i = 0; i < potentiallists.getLength(); i++) {
                Element e = (Element)potentiallists.item(i);
                Element schema = getSchemaElement(e.getAttribute(SCHEMA_LOCATION), docs);
                if (schema != null) {
                    schemas.add(schema);
                }
            }
        }

        //Extract the 'nested' schema
        NodeList output = typesel.getElementsByTagNameNS(W3C_XML_SCHEMA, TOP_SCHEMA_ELNAME);
        for (int i = 0; i < output.getLength(); i++) {
            schemas.add((Element) output.item(i));
        }

        // transpose the namespaces from schema parents
        Node node = typesel;
        while (node != null) {
            if (node instanceof Element) {
                Element el = (Element)node;
                NamedNodeMap attrsmap = el.getAttributes();
                for (int i = 0; i < attrsmap.getLength(); i++) {
                    Attr attrnode = (Attr)attrsmap.item(i);
                    if (attrnode.getName().startsWith("xmlns:")) {
                        for (int ii = 0; ii < schemas.size(); ii++) {
                            Element schemael = schemas.get(ii);
                            if ( !schemael.hasAttributeNS( DomUtils.XMLNS_NS, attrnode.getLocalName() ) ) {
                                schemael.setAttributeNS(DomUtils.XMLNS_NS, attrnode.getName(), attrnode.getValue());
                            }
                        }
                    }
                }
            }
            node = node.getParentNode();
        }
        return schemas;
    }

    /**
     * Extract the schema element from defined schemaLocation.
     *
     * @param schemaLocation The schema Location
     * @param docs Schemas imported from the WSDL Document
     * @return The schema element or null if schema definition not found.
     */
    private static Element getSchemaElement(String schemaLocation, Map<String,Document> docs) {
        for ( final Map.Entry<String, Document> doc : docs.entrySet() ) {
            if (schemaLocation!= null && doc.getKey().endsWith(schemaLocation)) {
                return doc.getValue().getDocumentElement();
            }
        }
        return null;
    }

    private Element[] removeQnamesFromSchemas(Collection qnames) {
        Element[] output = new Element[schemaElements.size()];
        for (int i = 0; i < schemaElements.size(); i++) {
            // clone the schema
            output[i] = (Element)(schemaElements.get(i).cloneNode(true));
            // only keep the elements that are in the qnames passed
            String tns = output[i].getAttribute("targetNamespace");
            NodeList schemaChildren = output[i].getChildNodes();
            for (int ii = 0; ii < schemaChildren.getLength(); ii++) {
                Node child = schemaChildren.item(ii);
                if (child.getNodeType() != Node.ELEMENT_NODE) continue;
                Element el = (Element)child;
                String elName = el.getAttribute("name");
                if (qnames.contains(new QName(tns, elName))) {
                    output[i].removeChild(el);
                }
            }
        }
        return output;
    }

    public Element[] getFullSchemas() {
        if (schemaElements == null) return null;
        Element[] output = new Element[schemaElements.size()];
        for (int i = 0; i < schemaElements.size(); i++) {
            output[i] = (Element)schemaElements.get(i);
        }
        return output;
    }

    public Element[] getInputSchemas() {
        return inputSchemas;
    }

    public Element[] getOutputSchemas() {
        return ouputSchemas;
    }

    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String WSDL_TYPES_ELNAME = "types";
    public static final String TOP_SCHEMA_ELNAME = "schema";
    public static final String IMPORT_ELNAME = "import";
    public static final String SCHEMA_LOCATION = "schemaLocation";
    public static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    public Element[] inputSchemas;
    public Element[] ouputSchemas;

    private Document wsdl;
    private NodeList messages = null;
    private List<Element> schemaElements = null;
    private Map<String,Document> docs = null;


}
