package com.l7tech.skunkworks.wsdl;

import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;

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
        WsdlAnalizer me = new WsdlAnalizer();
        me.parseInputOutputs(me.getWsdlSample());
    }

    public void parseInputOutputs(Document wsdl) throws Exception {
        NodeList inputlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
        for (int i = 0; i < inputlist.getLength(); i++) {
            Element item = (Element)inputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                System.out.println("INPUT: " + msg);
            }
        }

        NodeList outputlist = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
        for (int i = 0; i < outputlist.getLength(); i++) {
            Element item = (Element)outputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                System.out.println("OUTPUT: " + msg);
            }
        }
    }

    public Document getWsdlSample() throws Exception {
        return XmlUtil.getDocumentBuilder().parse(getRes(WAREHOUSE_WSDL_PATH));
    }

    private InputSource getRes(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("\ncannot load resource " + path + ".\ncheck your runtime properties.\n");
        }
        return new InputSource(is);
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
    private static final String WAREHOUSE_WSDL_PATH = RESOURCE_PATH + "warehouse.wsdl";
}
