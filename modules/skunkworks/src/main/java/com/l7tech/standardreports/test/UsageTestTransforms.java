/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 23, 2008
 * Time: 12:14:10 PM
 */
package com.l7tech.standardreports.test;

import java.io.*;
import java.util.*;

import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.ems.standardreports.Utilities;
import org.w3c.dom.Document;
import org.junit.Test;
import org.junit.Assert;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

public class UsageTestTransforms{

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

    private Document transform(String xslt, String xmlSrc, Map<String, Object> map ) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transformerFactory.newTemplates(xsltsource).newTransformer();

        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        //all jasper reports must have a dtd, were not going to handle it, just ignore
        //builderF.setValidating(false);
        DocumentBuilder builder = builderF.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlSrc));
        Document doc = builder.parse(is);

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

    private String getResAsStringClasspath(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        byte[] resbytes = IOUtils.slurpStream(is, 100000);
        return new String(resbytes);
    }

    @Test
    public void testUsageTransformation_HardCodedTranslationDoc() throws Exception{
        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        //String xslStr = getResAsStringClasspath("UsageReportTransform.xsl");
        //String xmlFileName = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_Summary_XSLT_Template.jrxml";
        String xmlFileName = getResAsStringClasspath("Usage_Summary_XSLT_Template.jrxml");
        String runtimeXmlStr =  getResAsString("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeUsageXsltXml.xml");
        Document runtimeDoc = XmlUtil.stringToDocument(runtimeXmlStr);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        //Document doc = transform(xslStr, xmlStr, params);
        Document doc = transform(xslStr, xmlFileName, params);
//        String s= XmlUtil.nodeToString(doc);
//        System.out.println(s);
    }

    @Test
    public void testUsageTransformation_Dynamic() throws Exception {
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");

        Document doc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", doc);
        //Document doc = transform(xslStr, xmlStr, params);
        Document transformDoc = transform(xslStr, xmlFileName, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testUsageTransformation_DynamicToJasperCompile() throws Exception {
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
//        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1");
        mappingValues.add("127.0.0.2");
//        mappingValues.add("127.0.0.1Bronze");
//        mappingValues.add("127.0.0.1Gold");
//        mappingValues.add("127.0.0.1Silver");
//        mappingValues.add("127.0.0.2Bronze");
//        mappingValues.add("127.0.0.2Gold");
//        mappingValues.add("127.0.0.2Silver");

        Document transformDoc = Utilities.getUsageRuntimeDoc(false, keys, mappingValues);
        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);

        //Document transformDoc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        XmlUtil.format(jasperDoc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestCreatedJasperDoc.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }
        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestCreatedTransformDoc.jrxml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);
    }

    @Test
    public void testUsageIntervalTransform_Master() throws Exception {
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
//        mappingValues.add("127.0.0.1");
//        mappingValues.add("127.0.0.2");
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");
        mappingValues.add("127.0.0.2Bronze");
        mappingValues.add("127.0.0.2Gold");
        mappingValues.add("127.0.0.2Silver");
        mappingValues.add("127.0.0.1Bronze1");
        mappingValues.add("127.0.0.1Gold1");
        mappingValues.add("127.0.0.1Silver1");
        mappingValues.add("127.0.0.2Bronze1");
        mappingValues.add("127.0.0.2Gold1");
        mappingValues.add("127.0.0.2Silver1");

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_IntervalMasterReport_Template.jrxml");

        Document transformDoc = Utilities.getUsageIntervalMasterRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/MasterTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/MasterTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

    }

    @Test
    public void testUsageIntervalMaster_CompileToJasper() throws Exception {
        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
        keys.add("CUSTOMER");

        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();
        mappingValues.add("127.0.0.1Bronze");
        mappingValues.add("127.0.0.1Gold");
        mappingValues.add("127.0.0.1Silver");
        mappingValues.add("127.0.0.2Bronze");
        mappingValues.add("127.0.0.2Gold");
        mappingValues.add("127.0.0.2Silver");
        mappingValues.add("127.0.0.1Bronze1");
        mappingValues.add("127.0.0.1Gold1");
        mappingValues.add("127.0.0.1Silver1");
        mappingValues.add("127.0.0.2Bronze1");
        mappingValues.add("127.0.0.2Gold1");
        mappingValues.add("127.0.0.2Silver1");

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_IntervalMasterReport_Template.jrxml");

        Document transformDoc = Utilities.getUsageIntervalMasterRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

}
