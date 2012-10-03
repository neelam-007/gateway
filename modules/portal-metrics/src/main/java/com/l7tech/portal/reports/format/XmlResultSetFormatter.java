package com.l7tech.portal.reports.format;

import com.l7tech.portal.reports.parameter.ApiQuotaUsageReportParameters;
import com.l7tech.portal.reports.parameter.DefaultReportParameters;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * XML specific ResultSet formatter.
 */
public class XmlResultSetFormatter extends AbstractResultSetFormatter {
    private static final Logger LOGGER = Logger.getLogger(XmlResultSetFormatter.class);

    /**
     * Name of element which encloses all others.
     */
    private final String parentElementName;
    /**
     * Name of element which represents each row in the result set.
     */
    private final String rowElementName;
    /**
     * Set to true to enabling indenting of xml elements.
     */
    private boolean indent;

    public XmlResultSetFormatter(final String parentElementName, final String rowElementName) {
        Validate.notEmpty(parentElementName, "Parent element name cannot be null or empty.");
        Validate.notEmpty("Row element name cannot be null or empty.");
        this.parentElementName = parentElementName;
        this.rowElementName = rowElementName;
    }

    public boolean isIndent() {
        return indent;
    }

    public void setIndent(final boolean indent) {
        this.indent = indent;
    }

    @Override
    String getNullValueForGroupingColumn() {
        return null;
    }

    @Override
    String mapToString(final Map<String, List<Map<String, Object>>> resultSetMap, final ResultSetFormatOptions options) {
        String xml = "<" + parentElementName + "/>";
        try {
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            final Element root = document.createElement(parentElementName);
            document.appendChild(root);
            for (Map.Entry<String, List<Map<String, Object>>> group : resultSetMap.entrySet()) {
                appendGroupElement(options, document, root, group);
            }
            xml = documentToString(document);
        } catch (final ParserConfigurationException e) {
            LOGGER.error("Error building map document: " + e.getMessage(), e);
        } catch (final TransformerException e) {
            LOGGER.error("Error transforming map document to string: " + e.getMessage(), e);
        }
        return xml;
    }

    @Override
    String listToString(final List<Map<String, Object>> resultSetList, final ResultSetFormatOptions options) {
        String xml = "<" + parentElementName + "/>";
        try {
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            final Element root = document.createElement(parentElementName);
            document.appendChild(root);
            for (final Map<String, Object> row : resultSetList) {
                appendRowElement(document, root, row);
            }
            xml = documentToString(document);
        } catch (final ParserConfigurationException e) {
            LOGGER.error("Error building list document: " + e.getMessage(), e);
        } catch (final TransformerException e) {
            LOGGER.error("Error transforming list document to string: " + e.getMessage(), e);
        }
        return xml;
    }

    private void appendRowElement(final Document document, final Element parent, final Map<String, Object> row) {
        final Element rowElement = document.createElement(rowElementName);
        parent.appendChild(rowElement);
        for (final Map.Entry<String, Object> column : row.entrySet()) {
            final Element columnElement = document.createElement(column.getKey());
            final Object value = column.getValue();
            if (value != null) {
                columnElement.appendChild(document.createTextNode(column.getValue().toString()));
            }
            rowElement.appendChild(columnElement);
        }
    }

    private void appendGroupElement(ResultSetFormatOptions options, Document document, Element root, Map.Entry<String, List<Map<String, Object>>> group) {
        final Element groupElement = document.createElement(options.getGroupingColumnName());
        root.appendChild(groupElement);
        if (group.getKey() != null) {
            groupElement.setAttribute("value", group.getKey());
        }
        for (final Map<String, Object> row : group.getValue()) {
            appendRowElement(document, groupElement, row);
        }
    }

    private String documentToString(final Document document) throws TransformerException {
        final DOMSource domSource = new DOMSource(document);
        final Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if (indent) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        final StringWriter sw = new StringWriter();
        transformer.transform(domSource, new StreamResult(sw));
        return sw.toString();
    }

    public String formatQuotaUsageXML(Map<DefaultReportParameters.QuotaRange, String> data, ApiQuotaUsageReportParameters params) throws ParserConfigurationException, IOException, SAXException, TransformerException {

        final Document return_document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element root = return_document.createElement(parentElementName);
        return_document.appendChild(root);
        for (DefaultReportParameters.QuotaRange quota : data.keySet()) {
            List<String> original_uuids = params.getApiRanges().get(quota);
            String s = data.get(quota);
            String xmlStr = data.get(quota);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            InputSource source = new InputSource(new StringReader(s));
            Document document = factory.newDocumentBuilder().parse(source);
            NodeList apiKeys = document.getElementsByTagName("api_key");
            Element newNode = return_document.createElement("api_key");
            for (String uuid : original_uuids) {
                if (!xmlStr.contains(uuid)) {
                    Element uuidNode = return_document.createElement("uuid");
                    uuidNode.setAttribute("value", uuid);
                    uuidNode.setAttribute("range", "" + Integer.toString(quota.ordinal() + 1));
                    uuidNode.setAttribute("hits", "0");
                    newNode.appendChild(uuidNode);
                }
            }
            root.appendChild(newNode);
            newNode.setAttribute("value", params.getApiKey());
            root.appendChild(newNode);
            NodeList usages = document.getElementsByTagName("Usage");
            for (int a = 0; a < usages.getLength(); a++) {
                Element uuidE = (Element) usages.item(a);
                Element uuidNode = return_document.createElement("uuid");
                uuidNode.setAttribute("value", uuidE.getElementsByTagName("uuid").item(0).getTextContent());
                uuidNode.setAttribute("range", "" + Integer.toString(quota.ordinal() + 1));
                String hits;
                switch (quota) {
                    case SECOND:
                        hits = uuidE.getElementsByTagName("per_sec_avg").item(0).getTextContent();
                        break;
                    case MINUTE:
                        hits = uuidE.getElementsByTagName("per_min_avg").item(0).getTextContent();
                        break;
                    default:
                        hits = uuidE.getElementsByTagName("hits").item(0).getTextContent();
                }
                uuidNode.setAttribute("hits", hits);
                newNode.appendChild(uuidNode);
            }
        }

        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(return_document);
        trans.transform(source, result);
        String xmlString = sw.toString();
        return xmlString;

    }

}
