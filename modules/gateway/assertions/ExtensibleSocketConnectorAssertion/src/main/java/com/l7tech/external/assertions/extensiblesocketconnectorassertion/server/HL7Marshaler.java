package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 4/2/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class HL7Marshaler {

    private static final String DEFAULT_SEGMENT_DELIMITER = "\r";
    private static final String DEFAULT_FIELD_DELIMITER = "|";
    private static final String DEFAULT_SUB_FIELD_DELIMITER = "^";
    private static final String DEFAULT_SUB_SUB_FIELD_DELIMITER = "&";
    private static final String DEFAULT_CHARSET = "ISO-8859-1";

    private static final String ROOT_TAG = "HL7";
    private static final String MSH_TAG = "MSH";
    private static final String MSH_FIELD_DELIMITER_TAG = MSH_TAG + ".1";
    private static final String MSH_ALL_DELIMITERS_TAG = MSH_TAG + ".2";

    private Document doc = null;
    private Element root = null;

    private String charset = DEFAULT_CHARSET;
    private String segmentDelimiter = DEFAULT_SEGMENT_DELIMITER;
    private String fieldDelimiter = DEFAULT_FIELD_DELIMITER;
    private String subFieldDelimiter = DEFAULT_SUB_FIELD_DELIMITER;
    private String subSubFieldDelimiter = DEFAULT_SUB_SUB_FIELD_DELIMITER;

    public HL7Marshaler() {
    }

    public HL7Marshaler(final String charset) {
        this.charset = charset;
    }

    public byte[] marshal(final byte[] xmlData) throws IOException, SAXException, HL7MarshalingException {
        doc = XmlUtil.parse(new ByteArrayInputStream(xmlData), charset);
        root = doc.getDocumentElement();
        if (0 != root.getTagName().compareTo(ROOT_TAG)) {
            throw new HL7MarshalingException("Unexpected root element in XML. Expected: " + ROOT_TAG);
        }

        byte[] mshSegment = this.marshalMshSegment();
        byte[] otherSegments = this.marshalSegment();

        byte[] result = new byte[mshSegment.length + otherSegments.length];
        System.arraycopy(mshSegment, 0, result, 0, mshSegment.length);
        System.arraycopy(otherSegments, 0, result, mshSegment.length, otherSegments.length);

        return result;
    }

    public byte[] unmarshal(final byte[] hl7Data) throws IOException, HL7MarshalingException {
        doc = XmlUtil.createEmptyDocument(ROOT_TAG, null, null);
        root = doc.getDocumentElement();

        String hl7StrVal = new String(hl7Data, charset);
        String[] segments = hl7StrVal.split(segmentDelimiter, -1);

        for (int ix = 0; ix < segments.length; ix++) {
            if (ix == 0) {
                // The first segment is always MSH.
                //
                this.unmarshalMshSegment(segments[ix]);
            } else {
                this.unmarshalSegment(segments[ix]);
            }
        }

        return XmlUtil.nodeToString(doc).getBytes(charset);
    }

    private byte[] marshalMshSegment() throws HL7MarshalingException {
        NodeList mshNodeList = doc.getElementsByTagName(MSH_TAG);
        if (mshNodeList == null || mshNodeList.getLength() != 1) {
            throw new HL7MarshalingException("Unexpected Error. Exactly 1 MSH tag expected.");
        }

        Node mshNode = root.getFirstChild();
        if (!mshNode.getNodeName().equals(MSH_TAG)) {
            throw new HL7MarshalingException("The first node must be MSH.");
        }

        // Find field delimiter in the "MSH.1" element.
        //
        Node node = mshNode.getFirstChild();
        String nodeName = node.getNodeName();
        if (!nodeName.equals(MSH_FIELD_DELIMITER_TAG)) {
            throw new HL7MarshalingException("The first element in MSH must be " + MSH_FIELD_DELIMITER_TAG + ".");
        }
        fieldDelimiter = node.getTextContent();

        // Find all delimiters in the "MSH.2" element.
        //
        node = node.getNextSibling();
        nodeName = node.getNodeName();
        if (!nodeName.equals(MSH_ALL_DELIMITERS_TAG)) {
            throw new HL7MarshalingException("The second element in MSH must be " + MSH_ALL_DELIMITERS_TAG + ".");
        }
        String delimiters = node.getTextContent();
        if (delimiters.length() == 4) {
            subFieldDelimiter = String.valueOf(delimiters.charAt(0));
            subSubFieldDelimiter = String.valueOf(delimiters.charAt(3));
        } else {
            throw new HL7MarshalingException("Unexpected delimiter characters: " + delimiters);
        }

        // Build HL7 MSH segment.
        //
        StringBuffer sb = new StringBuffer();
        sb.append(mshNode.getNodeName());
        sb.append(fieldDelimiter);
        sb.append(delimiters);

        // Add fields.
        // Ignore the first 2 fields. They are already added
        //
        NodeList mshFieldNodeList = mshNode.getChildNodes();
        for (int ix = 2; ix < mshFieldNodeList.getLength(); ix++) {
            sb.append(fieldDelimiter);
            this.marshalField(mshFieldNodeList.item(ix), sb);
        }
        sb.append(segmentDelimiter);

        try {
            return sb.toString().getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new HL7MarshalingException(e);
        }
    }

    private byte[] marshalSegment() throws HL7MarshalingException {
        StringBuffer sb = new StringBuffer();

        NodeList segmentNodeList = root.getChildNodes();
        for (int ix = 0; ix < segmentNodeList.getLength(); ix++) {
            Node segmentNode = segmentNodeList.item(ix);
            if (ix == 0) {
                // Skip MSH segment.
                //
                continue;
            }

            sb.append(segmentNode.getNodeName());
            NodeList fieldNodeList = segmentNode.getChildNodes();
            for (int iy = 0; iy < fieldNodeList.getLength(); iy++) {
                sb.append(fieldDelimiter);
                this.marshalField(fieldNodeList.item(iy), sb);
            }

            if (ix != segmentNodeList.getLength() - 1) {
                sb.append(segmentDelimiter);
            }
        }

        try {
            return sb.toString().getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new HL7MarshalingException(e);
        }
    }

    private void marshalField(final Node node, final StringBuffer sb) {
        if (!node.hasChildNodes()) {
            sb.append(node.getTextContent());
        } else {
            // Add sub-fields.
            //
            NodeList subFieldNodeList = node.getChildNodes();
            for (int ix = 0; ix < subFieldNodeList.getLength(); ix++) {
                if (ix != 0) {
                    sb.append(subFieldDelimiter);
                }
                this.marshalSubField(subFieldNodeList.item(ix), sb);
            }
        }
    }

    private void marshalSubField(final Node node, final StringBuffer sb) {
        if (!node.hasChildNodes()) {
            sb.append(node.getTextContent());
        } else {
            // Add sub-sub-fields.
            //
            NodeList subSubFieldNodeList = node.getChildNodes();
            for (int ix = 0; ix < subSubFieldNodeList.getLength(); ix++) {
                if (ix != 0) {
                    sb.append(subSubFieldDelimiter);
                }
                sb.append(subSubFieldNodeList.item(ix).getTextContent());
            }
        }
    }

    private void unmarshalMshSegment(final String msh) throws HL7MarshalingException {
        if (msh.substring(0, 3).equals(MSH_TAG)) {
            Element mshElement = this.addElement(root, MSH_TAG);

            // Find field delimiter.
            //
            fieldDelimiter = String.valueOf(msh.charAt(3));
            this.addElement(mshElement, MSH_FIELD_DELIMITER_TAG).setTextContent(fieldDelimiter);

            // Split based on field Delimiter
            //
            String[] fields = msh.split("\\" + fieldDelimiter, -1);

            // Find all delimiters.
            //
            String delimiters = fields[1];
            if (delimiters.length() == 4) {
                subFieldDelimiter = String.valueOf(delimiters.charAt(0));
                subSubFieldDelimiter = String.valueOf(delimiters.charAt(3));
            } else {
                throw new HL7MarshalingException("Unexpected delimiter characters: " + delimiters);
            }

            this.addElement(mshElement, MSH_ALL_DELIMITERS_TAG).setTextContent(delimiters);

            // Add fields.
            // Ignore the first 2 fields. They are already added
            //
            for (int ix = 2; ix < fields.length; ix++) {
                this.unmarshalField(mshElement, MSH_TAG + "." + String.valueOf(ix + 1), fields[ix]);
            }
        } else {
            throw new HL7MarshalingException("The first segment must be MSH.");
        }
    }

    private void unmarshalSegment(final String segment) {
        // Split based on field Delimiter
        //
        String[] fields = segment.split("\\" + fieldDelimiter, -1);
        String segmentName = fields[0];

        Element segmentElement = this.addElement(root, segmentName);

        // Add fields.
        // Ignore the first field. The first segment is the segment name.
        //
        for (int ix = 1; ix < fields.length; ix++) {
            this.unmarshalField(segmentElement, segmentName + "." + String.valueOf(ix), fields[ix]);
        }
    }

    private void unmarshalField(final Element parent, final String tagName, final String fieldValue) {
        Element fieldElement = this.addElement(parent, tagName);

        if (!fieldValue.contains(subFieldDelimiter)) {
            fieldElement.setTextContent(fieldValue);
        } else {
            // Split based on sub-field Delimiter
            //
            String[] subFields = fieldValue.split("\\" + subFieldDelimiter, -1);

            // Add sub-fields.
            //
            for (int ix = 0; ix < subFields.length; ix++) {
                this.unmarshalSubField(fieldElement, tagName + "." + String.valueOf(ix + 1), subFields[ix]);
            }
        }
    }

    private void unmarshalSubField(final Element parent, final String tagName, final String fieldValue) {
        Element subFieldElement = this.addElement(parent, tagName);

        if (!fieldValue.contains(subSubFieldDelimiter)) {
            subFieldElement.setTextContent(fieldValue);
        } else {
            // Split based on sub-sub-field Delimiter
            //
            String[] subSubFields = fieldValue.split("\\" + subSubFieldDelimiter, -1);

            // Add sub-sub-fields.
            //
            for (int ix = 0; ix < subSubFields.length; ix++) {
                Element subSubFieldElement = this.addElement(parent, tagName + "." + String.valueOf(ix + 1));
                subSubFieldElement.setTextContent(subSubFields[ix]);
            }
        }
    }

    private Element addElement(final Element parent, final String tagName) {
        Element element = doc.createElement(tagName);
        parent.appendChild(element);
        return element;
    }
}