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

public class ReportTransformTests {

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
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageRuntimeTransform.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        }finally{
            fos.close();
        }

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", doc);
        params.put("FrameMinWidth", 565);
        params.put("PageMinWidth", 595);
        int reportInfoStaticTextSize = 128;
        params.put("ReportInfoStaticTextSize", reportInfoStaticTextSize);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformDoc = transform(xslStr, xmlFileName, params);

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeJasper.jrxml");
        f.createNewFile();
        fos = new FileOutputStream(f);
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
        String xmlSrc = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_IntervalMasterReport_Template.jrxml");

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

    @Test
    public void testUsageSubIntervalTransform_Master() throws Exception {
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

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportSubIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_Template.jrxml");

        Document transformDoc = Utilities.getUsageSubIntervalMasterRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubMasterTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubMasterTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testUsageSubIntervalMaster_CompileToJasper() throws Exception {
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

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/UsageReportSubIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_Template.jrxml");

        Document transformDoc = Utilities.getUsageSubIntervalMasterRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

    @Test
    public void testUsageSubReport() throws Exception {
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
//        mappingValues.add("127.0.0.1Bronze1");
//        mappingValues.add("127.0.0.1Gold1");
//        mappingValues.add("127.0.0.1Silver1");
//        mappingValues.add("127.0.0.2Bronze1");
//        mappingValues.add("127.0.0.2Gold1");
//        mappingValues.add("127.0.0.2Silver1");

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_SubReport.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document transformDoc = Utilities.getUsageSubReportRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubReportTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubReportTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }
    }

    @Test
    public void testUsageSubReport_CompileToJasper() throws Exception {
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

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/Usage_SubReport.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/java/com/l7tech/server/ems/standardreports/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document transformDoc = Utilities.getUsageSubReportRuntimeDoc(false, keys, mappingValues);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

    @Test
    public void testGetPerfStatRuntimeDoc_Mapping() throws Exception{

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("Group 1", "IP_ADDRESS: 127.0.0.1, CUSTOMER: GOLD");
        linkedHashMap.put("Group 2", "IP_ADDRESS: 127.0.0.2, CUSTOMER: GOLD");
        linkedHashMap.put("Group 3", "IP_ADDRESS: 127.0.0.3, CUSTOMER: GOLD");
        linkedHashMap.put("Group 4", "IP_ADDRESS: 127.0.0.4, CUSTOMER: GOLD");

        Document transformDoc = Utilities.getPerfStatIntervalMasterRuntimeDoc(true, linkedHashMap);
        Assert.assertTrue(transformDoc != null);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PerfStatIntervalMastereRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }
        
        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PS_IntervalMasterReport.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);
        
    }
    
    @Test
    public void testGetPerfStatRuntimeDoc_NoMapping() throws Exception{

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("Service 1", "Warehouse [w1]");
        linkedHashMap.put("Service 2", "Warehouse [w2]");
        linkedHashMap.put("Service 3", "Warehouse [w3]");
        linkedHashMap.put("Service 4", "Warehouse [w4]");

        Document transformDoc = Utilities.getPerfStatIntervalMasterRuntimeDoc(false, linkedHashMap);
        Assert.assertTrue(transformDoc != null);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PerfStatIntervalRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        }finally{
            fos.close();
        }

        String xslStr = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString("modules/ems/src/main/resources/com/l7tech/server/ems/standardreports/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PS_IntervalMasterReport.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try{
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        }finally{
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

}
