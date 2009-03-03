/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 23, 2008
 * Time: 12:14:10 PM
 */
package com.l7tech.skunkworks.standardreports.test;

import java.io.*;
import java.util.*;

import com.l7tech.util.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.gateway.standardreports.RuntimeDocUtilities;
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
        try {
            byte[] resbytes = IOUtils.slurpStream(is, 100000);
            return new String(resbytes);
        } finally {
            is.close();
        }
    }

    private Document transform(String xslt, String xmlSrc, Map<String, Object> map) throws Exception {
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

//    @Test
//    public void testUsageTransformation_HardCodedTranslationDoc() throws Exception{
//        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportTransform.xsl");
//        //String xslStr = getResAsStringClasspath("UsageReportTransform.xsl");
//        //String xmlFileName = "/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/Usage_Summary_XSLT_Template.jrxml";
//        String xmlFileName = getResAsStringClasspath("Usage_Summary_XSLT_Template.jrxml");
//        String runtimeXmlStr =  getResAsString("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeUsageXsltXml.xml");
//        Document runtimeDoc = XmlUtil.stringToDocument(runtimeXmlStr);
//        Map<String, Object> params = new HashMap<String, Object>();
//        params.put("RuntimeDoc", runtimeDoc);
//        //Document doc = transform(xslStr, xmlStr, params);
//        Document doc = transform(xslStr, xmlFileName, params);
////        String s= XmlUtil.nodeToString(doc);
////        System.out.println(s);
//    }

    @Test
    public void testUsageTransformation_Dynamic() throws Exception {

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add(";");
        valueList.add("127.0.0.1");
        valueList.add("GOLD");
        distinctMappingSets.add(valueList);

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageRuntimeTransform.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(doc, fos);
        } finally {
            fos.close();
        }

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", doc);
        params.put("FrameMinWidth", 565);
        params.put("PageMinWidth", 595);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformDoc = transform(xslStr, xmlFileName, params);

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/RuntimeJasper.jrxml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testUsageTransformation_DynamicToJasperCompile() throws Exception {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<String> keys = new ArrayList<String>();
        keys.add("IP_ADDRESS");
//        keys.add("CUSTOMER");

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add(";");
        valueList.add("127.0.0.1");
        distinctMappingSets.add(valueList);

        Document transformDoc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportTransform.xsl");
        String xmlFileName = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_Summary_Template.jrxml");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);

        //Document transformDoc = transform(xslStr, xmlStr, params);
        Document jasperDoc = transform(xslStr, xmlFileName, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        XmlUtil.format(jasperDoc, true);
        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestCreatedJasperDoc.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }
        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestCreatedTransformDoc.jrxml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);
    }

    @Test
    public void testUsageIntervalTransform_Master() throws Exception {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair("127.0.0.1"));
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair("GOLD"));
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> authFilters = new ArrayList<ReportApi.FilterPair>();
        authFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("AUTH_USER", authFilters);

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("Donal");
        valueList.add("127.0.0.1");
        valueList.add("Bronze");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_IntervalMasterReport_Template.jrxml");

        Document transformDoc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/MasterTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/MasterTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

    }

    @Test
    public void testUsageIntervalMaster_CompileToJasper() throws Exception {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        List<ReportApi.FilterPair> authFilters = new ArrayList<ReportApi.FilterPair>();
        authFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("AUTH_USER", authFilters);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_IntervalMasterReport_Template.jrxml");

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("Donal");
        valueList.add("127.0.0.1");
        valueList.add("Bronze");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        Document transformDoc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);

        File f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestMasterTransformDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);


        Document jasperDoc = transform(xslStr, xmlSrc, params);

        f = new File("/home/darmstrong/ideaprojects/UneasyRoosterModular/modules/skunkworks/src/main/java/com/l7tech/standardreports/UsageTestMasterRuntimeDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

    @Test
    public void testUsageSubIntervalTransform_Master() throws Exception {
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("Donal");
        valueList.add("127.0.0.1");
        valueList.add("Bronze");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportSubIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubIntervalMasterReport_Template.jrxml");

        Document transformDoc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubMasterTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubMasterTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testUsageSubIntervalMaster_CompileToJasper() throws Exception {
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("Donal");
        valueList.add("127.0.0.1");
        valueList.add("Bronze");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/UsageReportSubIntervalTransform_Master.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubIntervalMasterReport_Template.jrxml");

        Document transformDoc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
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
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("127.0.0.1");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubReport.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document transformDoc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);
        params.put("PageMinWidth", 535);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubReportTransformJasper.jrxml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        XmlUtil.format(transformDoc, true);
        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/SubReportTransformDoc.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }
    }

    @Test
    public void testUsageSubReport_CompileToJasper() throws Exception {
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> valueList = new ArrayList<String>();
        valueList.add("127.0.0.1");
        valueList.add("Bronze");
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);
        distinctMappingSets.add(valueList);

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubReport.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document transformDoc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
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
    public void testGetPerfStatRuntimeDoc_Mapping() throws Exception {

        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        keysToFilters.put("IP_ADDRESS", Collections.<ReportApi.FilterPair>emptyList());
        keysToFilters.put("CUSTOMER", Collections.<ReportApi.FilterPair>emptyList());

        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        List<String> l1 = new ArrayList<String>();
        l1.add("127.0.0.1");
        l1.add("GOLD");
        distinctMappingSets.add(l1);
        List<String> l2 = new ArrayList<String>();
        l2.add("127.0.0.2");
        l2.add("GOLD");
        distinctMappingSets.add(l2);
        List<String> l3 = new ArrayList<String>();
        l3.add("127.0.0.3");
        l3.add("GOLD");
        distinctMappingSets.add(l3);
        List<String> l4 = new ArrayList<String>();
        l4.add("127.0.0.4");
        l4.add("GOLD");
        distinctMappingSets.add(l4);

        Document transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(keysToFilters, distinctMappingSets);
        Assert.assertTrue(transformDoc != null);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PerfStatIntervalMastereRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PS_IntervalMasterReport.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

    @Test
    public void testGetPerfStatRuntimeDoc_NoMapping() throws Exception {

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("Service 1", "Warehouse [w1]");
        linkedHashMap.put("Service 2", "Warehouse [w2]");
        linkedHashMap.put("Service 3", "Warehouse [w3]");
        linkedHashMap.put("Service 4", "Warehouse [w4]");

        Document transformDoc = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(linkedHashMap);
        Assert.assertTrue(transformDoc != null);

        File f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PerfStatIntervalRuntimeDoc.xml");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(transformDoc, fos);
        } finally {
            fos.close();
        }

        String xslStr = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/PS_IntervalMasterTransform.xsl");
        String xmlSrc = getResAsString("modules/gateway/reporting/src/main/resources/com/l7tech/gateway/standardreports/PS_IntervalMasterReport_Template.jrxml");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", transformDoc);

        Document jasperDoc = transform(xslStr, xmlSrc, params);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(jasperDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        f = new File("modules/skunkworks/src/main/java/com/l7tech/standardreports/PS_IntervalMasterReport.xml");
        f.createNewFile();
        fos = new FileOutputStream(f);
        try {
            XmlUtil.nodeToFormattedOutputStream(jasperDoc, fos);
        } finally {
            fos.close();
        }

        JasperReport report = JasperCompileManager.compileReport(bais);
        Assert.assertTrue(report != null);

    }

}
