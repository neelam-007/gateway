package com.l7tech.server.tomcat;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that can parse Connector properties out of a server.xml file.
 */
public class ServerXmlParser {
    private List<Map<String, String>> connectors = new ArrayList<Map<String, String>>();

    public ServerXmlParser() {
    }

    public void load(File file) throws IOException, SAXException, InvalidDocumentFormatException {
        InputStream fis = null;
        try {
            Document doc = XmlUtil.parse(fis = new FileInputStream(file));
            load(doc);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    public void load(Document doc) throws SAXException, InvalidDocumentFormatException {
        Element server = doc.getDocumentElement();
        if (!"Server".equals(server.getLocalName()))
            throw new InvalidDocumentFormatException("Document element is not Server");
        String ns = server.getNamespaceURI();
        Element service = DomUtils.findOnlyOneChildElementByName(server, ns, "Service");
        if (service == null)
            throw new InvalidDocumentFormatException("Server does not contain exactly one Service");            
        List<Element> connectorEls = DomUtils.findChildElementsByName(service, ns, "Connector");
        for (Element connectorEl : connectorEls) {
            Map<String, String> map = new LinkedHashMap<String, String>();
            NamedNodeMap attrs = connectorEl.getAttributes();
            int num = attrs.getLength();
            for (int i = 0; i < num; ++i) {
                Node n = attrs.item(i);
                map.put(n.getNodeName(), n.getNodeValue());
            }
            connectors.add(map);
        }
    }

    /**
     *
     * @return the attributes of every Connector from this server.xml
     */
    public List<Map<String, String>> getConnectors() {
        return connectors;
    }
}
