/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 23, 2008
 * Time: 12:14:10 PM
 */
package com.l7tech.standardreports;

import junit.framework.TestCase;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.junit.Test;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

public class UsageTestTransforms extends TestCase {

    private String getResAsString(String path) throws IOException {
        File f = new File(path);
        InputStream is = new FileInputStream(f);
        try{
            byte[] resbytes = IOUtils.slurpStream(is, 100000);
            return new String(resbytes);
        }finally{
            is.close();
        }
    }

    private Document transform(String xslt, String srcPath, Map<String, Object> map ) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transformerFactory.newTemplates(xsltsource).newTransformer();

        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        //all jasper reports must have a dtd, were not going to handle it, just ignore
        //builderF.setValidating(false);
        DocumentBuilder builder = builderF.newDocumentBuilder();

        File f = new File(srcPath);
        InputStream is = null;

        Document doc = null;
        try{
            is = new FileInputStream(f);
            doc = builder.parse(is);
        }finally{
            if(is != null) is.close();
        }

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        XmlUtil.softXSLTransform(doc, result, transformer, map);
        System.out.println(sw.toString());
        StringReader reader = new StringReader(sw.toString());
        Document returnDoc = builder.parse(new InputSource(reader));
        return returnDoc;
    }

//    private Document transform(String xslt, String src, Map<String, Object> map) throws Exception {
//        TransformerFactory transfoctory = TransformerFactory.newInstance();
//        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
//        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
//        Document srcdoc = XmlUtil.stringToDocument(src);
//        DOMResult result = new DOMResult();
//        XmlUtil.softXSLTransform(srcdoc, result, transformer, map);
//        return (Document) result.getNode();
//    }

    @Test
    public void testUsageTransformation() throws Exception{
        String xslStr = getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/test/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlStr =  getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_Summary_Template.jrxml");
        //String xmlStr =  getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/ems/src/main/java/com/l7tech/server/ems/standardreports/test.xml");
        String xmlFileName = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_Summary_XSLT_Template.jrxml";
        String runtimeXmlStr =  getResAsString("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeUsageXsltXml.xml");
        Document runtimeDoc = XmlUtil.stringToDocument(runtimeXmlStr);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ReportName", "Usage Report");
        params.put("RuntimeDoc", runtimeDoc);
        //Document doc = transform(xslStr, xmlStr, params);
        Document doc = transform(xslStr, xmlFileName, params);
//        String s= XmlUtil.nodeToString(doc);
//        System.out.println(s);
    }

}
