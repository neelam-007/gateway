package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.*;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


import java.io.StringReader;

/**
 * Tests the ability to convert audit records to and from XML.
 */
public class AuditDetailMarshallerTest {

    private static final AuditDetailPropertiesDomMarshaller detailsMarshaller = new AuditDetailPropertiesDomMarshaller();

    final String[] params = new String[]{"<>?:@#$%^&*{}][","param1","","param2",""};
    private static final String detailProperties =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<detail xmlns=\"http://l7tech.com/audit/detail\">\n" +
            "    <params>\n" +
            "        <param>&lt;&gt;?:@#$%^&amp;*{}][</param>\n" +
            "        <param>param1</param>\n" +
            "        <param/>\n" +
            "        <param>param2</param>\n" +
            "        <param/>\n" +
            "    </params>\n" +
            "</detail>";

    @Test
    public void testDetailMarshal() throws Exception {
        final AuditDetail detail = new AuditDetail(Messages.EXCEPTION_SEVERE,params, new RuntimeException("message"));

        Document doc = XmlUtil.createEmptyDocument();
        Element element = detailsMarshaller.marshal(doc, detail);
        doc.appendChild(element);
        String marshalled = XmlUtil.nodeToFormattedString(element);

        AuditDetailPropertiesHandler handler = new AuditDetailPropertiesHandler();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        StringReader sr = new StringReader(marshalled);
        xr.parse(new InputSource(sr));

        String[] parameters = handler.getParameters();
        Assert.assertArrayEquals(params,parameters);
    }

    @Test
    public void testDetailParsing() throws Exception {

        AuditDetailPropertiesHandler handler = new AuditDetailPropertiesHandler();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        StringReader sr = new StringReader(detailProperties);
        xr.parse(new InputSource(sr));

        String[] parameters = handler.getParameters();
        Assert.assertArrayEquals(params,parameters);
    }
}
