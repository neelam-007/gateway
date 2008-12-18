/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Dec 17, 2008
 * Time: 4:47:44 PM
 */
package com.l7tech.gateway.standardreports;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.junit.Test;
import org.junit.Assert;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.text.MessageFormat;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.ResourceMapEntityResolver;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;

public class TransformAndCompileTest {
    private Document transform(String xslt, String src, Map<String, Object> map) throws Exception {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslt));
        Transformer transformer = transfoctory.newTemplates(xsltsource).newTransformer();
        Document srcDoc = getStringAsDocument(src);
        DOMResult result = new DOMResult();
        XmlUtil.softXSLTransform(srcDoc, result, transformer, map);
        return (Document) result.getNode();
    }

    private Document getStringAsDocument(String xml) throws Exception{
        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderF.newDocumentBuilder();
        builder.setEntityResolver( getEntityResolver() );
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private String getResAsStringClasspath(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        //if this throws an exception during test running, just in crease the max length, as the file must have
        //increased in size
        byte[] resbytes = IOUtils.slurpStream(is, 150000);
        return new String(resbytes);
    }

    /**
     * Using a canned transform xml document, transform the performance summary template jrxml file, compile and
     * test the output for correctness.
     * @throws Exception
     */
    @Test
    public void transformAndCompilePSSummaryTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("PS_Summary_TransformDoc.xml");
        String transformXsl = getResAsStringClasspath("PS_SummaryTransform.xsl");
        String templateXml = getResAsStringClasspath("PS_Summary_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        //todo [Donal] logical tests on the transformed runtime doc
    }

    /**
     * Using a canned transform xml document, transform the performance summary template jrxml file, compile and
     * test the output for correctness.
     * @throws Exception
     */
    @Test
    public void transformAndCompilePSIntervalTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("PS_Interval_TransformDoc.xml");
        String transformXsl = getResAsStringClasspath("PS_IntervalMasterTransform.xsl");
        String templateXml = getResAsStringClasspath("PS_IntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        //todo [Donal] logical tests on the transformed runtime doc
    }

    /**
     * Validate the transformed runtime Usage Summary jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageSummaryTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportTransform.xsl");
        String templateXml = getResAsStringClasspath("Usage_Summary_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("FrameMinWidth", 565);
        params.put("PageMinWidth", 595);
        int reportInfoStaticTextSize = 128;
        params.put("ReportInfoStaticTextSize", reportInfoStaticTextSize);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();
        //COLUMN_MAPPING_TOTAL_
        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_MAPPING_TOTAL_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='SERVICE_AND_OR_OPERATION_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 SERVICE_AND_OR_OPERATION_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='GRAND_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 GRAND_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[@name='SERVICE_ONLY_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 SERVICE_ONLY_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        //COLUMN_ variables will match all COLUMN_ variables, so need to put in extra constraint to match only those varibales
        //which are actually like COLUMN_1....COLUMN_12
        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_') and number(substring-after(@name, 'COLUMN_'))]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_MAPPING_TOTAL_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_MAPPING_TOTAL_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name, 'COLUMN_SERVICE_TOTAL_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_SERVICE_TOTAL_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //constantHeader - 12 column headers, plus 2 totals
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='CONSTANT']/groupHeader/band/frame[2]/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " contant group header text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //serviceAndOperationFooter - 12 columns + 1 total + 1 display text field
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " SERVICE_AND_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //serviceIdFooter 12 columns + 1 total + 1 display text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_ID']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " SERVICE_ID group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //constantFooter - 12 columns and 1 total + 1 display text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='CONSTANT']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2)+ " CONSTANT group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //page widths should match the value from the transform doc
        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/columnWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@columnWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Column width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //make sure all frames are the correct width, there are various width requirements throughout the document
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth -= titleInnerFrameBuffer;
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //all dynamic text fields in title - two elements per frame, this is the rhs element
        //removed as these are currently not dynamic, will see if current size and stretch attributes suffices
//        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
//        expectedWidth -= reportInfoStaticTextSize;
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/textField/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        //All group header and footer frames have the same frame width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/group/*/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/leftMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@leftMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Left margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/rightMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@rightMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Right margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());


    }

    /**
     * Validate the transformed runtime Usage Master jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageMasterTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageMasterTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportIntervalTransform_Master.xsl");
        String templateXml = getResAsStringClasspath("Usage_IntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("FrameMinWidth", 820);
        params.put("PageMinWidth", 850);
        int reportInfoStaticTextSize = 128;
        params.put("ReportInfoStaticTextSize", reportInfoStaticTextSize);
        int titleInnerFrameBuffer = 7;
        params.put("TitleInnerFrameBuffer", titleInnerFrameBuffer);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);

        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();
        //COLUMN_MAPPING_TOTAL_
        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_SERVICE_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_SERVICE_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_SERVICE_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_OPERATION_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_OPERATION_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_REPORT_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_REPORT_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceHeader = 12 columns + 1 total + 1 display text field
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE']/groupHeader/band/frame[2]/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 2) + " SERVICE group header text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 2));

        //subreport return variables
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_OPERATION_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_OPERATION_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_SERVICE_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_SERVICE_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[@toVariable='ROW_REPORT_TOTAL']", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be 1 ROW_REPORT_TOTAL variable, there were " + nodeList.getLength(), nodeList.getLength() == 1);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_SERVICE_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_SERVICE_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_OPERATION_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_OPERATION_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/returnValue[contains(@toVariable, 'COLUMN_REPORT_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_REPORT_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceAndOperationFooter - 12 columns + 1 total (1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //serviceIdFooter - 12 columns + 1 total(1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //summary - 12 columns + 1 total (1 static text also not counted)
        nodeList = (NodeList) xPath.evaluate("/jasperReport/summary/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " summary text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/columnWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@columnWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Column width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //make sure all frames are the correct width, there are various width requirements throughout the document
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        expectedWidth -= titleInnerFrameBuffer;
        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //all dynamic text fields in title - two elements per frame, this is the rhs element
//        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
//        expectedWidth -= reportInfoStaticTextSize;
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/title/band/frame[2]/frame/textField/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/frame/subreport/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++){
            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
                    expectedWidth.intValue(), actualWidth.intValue()),
                    expectedWidth.intValue() == actualWidth.intValue());
        }

        //All group header and footer frames have the same frame width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/frameWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        //todo [Donal] put in the specific reportElements, can no longer do blanket
//        nodeList = (NodeList) xPath.evaluate("/jasperReport/group/*/band/frame/reportElement/@width", transformedRuntimeDoc, XPathConstants.NODESET);
//        for(int i = 0; i < nodeList.getLength(); i++){
//            actualWidth = Double.valueOf(nodeList.item(i).getNodeValue());
//            Assert.assertTrue(MessageFormat.format("Frame width should be {0} actual width was {1}",
//                    expectedWidth.intValue(), actualWidth.intValue()),
//                    expectedWidth.intValue() == actualWidth.intValue());
//        }

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/leftMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@leftMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Left margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/rightMargin/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/@rightMargin", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Right margin width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }


    /**
     * Validate the transformed runtime Usage Sub Interval jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     *
     * @throws Exception
     */
    @Test
    public void transformUsageSubIntervalTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageSubIntervalTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("UsageReportSubIntervalTransform_Master.xsl");
        String templateXml = getResAsStringClasspath("Usage_SubIntervalMasterReport_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("PageMinWidth", 535);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();

        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //sub report return values
        nodeList = (NodeList) xPath.evaluate("/jasperReport/detail/band/subreport/returnValue[contains(@toVariable, 'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns + " returnValue @toVariable=COLUMN_ , there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

        //sub report width
        expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/subReportWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        actualWidth = (Double) xPath.evaluate("/jasperReport/detail/band/subreport/reportElement/@width", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Subreport width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }

    /**
     * Validate the transformed runtime Usage Sub Interval jrxml file. Confirms that all elements which should exist do,
     * and that the dynamic widths have been incorporated into existing template elements
     * Note: This test, tests for widths. Widths will only change in a transform if the min width set as a param
     * to the transform is exceeded. Currently this width is guaranteed to be exceeded. If the canned transform
     * documents were to change such that they min width was no longer exceeded, then these tests would fail
     * @throws Exception
     */
    @Test
    public void transformUsageSubReportTemplate() throws Exception{
        String transformXml = getResAsStringClasspath("UsageSubReportTransformDoc.xml");
        String transformXsl = getResAsStringClasspath("Usage_SubReport.xsl");
        String templateXml = getResAsStringClasspath("Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

        Document runtimeDoc = XmlUtil.stringToDocument(transformXml);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("RuntimeDoc", runtimeDoc);
        params.put("PageMinWidth", 535);

        Document transformedRuntimeDoc = transform(transformXsl, templateXml, params);
        Assert.assertNotNull("Transform document should not be null", transformedRuntimeDoc);
        compileReport(transformedRuntimeDoc);

        XPathFactory factory = XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
        XPath xPath = factory.newXPath();

        NodeList nodeList = (NodeList)xPath.evaluate("/JasperRuntimeTransformation/variables/variable[contains(@name, 'COLUMN_')]", runtimeDoc, XPathConstants.NODESET);
        int numColumns = nodeList.getLength();

        nodeList = (NodeList) xPath.evaluate("/jasperReport/variable[contains(@name,'COLUMN_')]", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ numColumns+ " @COLUMN_ variables, there were " + nodeList.getLength(), nodeList.getLength() == numColumns);

        //serviceAndOperationFooter - 12 + 1 total text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/group[@name='SERVICE_AND_OPERATION']/groupFooter/band/frame/textField", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " SERVICE_AND_OPERATION group footer text fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        //no data - 12 + 1 for total text fields
        nodeList = (NodeList) xPath.evaluate("/jasperReport/noData/band/frame/staticText", transformedRuntimeDoc, XPathConstants.NODESET);
        Assert.assertTrue("There should be "+ (numColumns + 1) + " no data static fields, there were " + nodeList.getLength(), nodeList.getLength() == (numColumns + 1));

        Double expectedWidth = (Double)xPath.evaluate("/JasperRuntimeTransformation/pageWidth/text()", runtimeDoc, XPathConstants.NUMBER);
        Double actualWidth = (Double) xPath.evaluate("/jasperReport/@pageWidth", transformedRuntimeDoc, XPathConstants.NUMBER);
        Assert.assertTrue(MessageFormat.format("Page width should be {0} actual width was {1}",
                expectedWidth.intValue(), actualWidth.intValue()),
                expectedWidth.intValue() == actualWidth.intValue());

    }

    private void compileReport(String jrxmlTemplateFile) throws Exception{
        String templateXml = getResAsStringClasspath(jrxmlTemplateFile);
        Document templateDoc = getTemplateDocument(templateXml);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(templateDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        boolean exceptionThrown = false;
        try{
            JasperReport compiledReport = JasperCompileManager.compileReport(bais);
            Assert.assertTrue("Compiled report should not be null", compiledReport != null);
        }catch(Exception ex){
            ex.printStackTrace();
            exceptionThrown = true;
        }
        Assert.assertTrue("No compile exception should have been thrown", !exceptionThrown);
    }

    private void compileReport(Document runTimeDoc) throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(runTimeDoc, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        boolean exceptionThrown = false;
        try{
            JasperReport compiledReport = JasperCompileManager.compileReport(bais);
            Assert.assertTrue("Compiled report should not be null", compiledReport != null);
        }catch(Exception ex){
            ex.printStackTrace();
            exceptionThrown = true;
        }
        Assert.assertTrue("No compile exception should have been thrown", !exceptionThrown);
    }

    /**
     * Usage template jrxml files are required as resources so are not compiled as part of the build process.
     * This test compiles all 4 usage jrxml files
     * @exception
     */
    @Test
    public void compileUsageReports() throws Exception {

        compileReport("Usage_Summary_Template.jrxml");

        compileReport("Usage_IntervalMasterReport_Template.jrxml");

        compileReport("Usage_SubIntervalMasterReport_Template.jrxml");

        compileReport("Usage_SubIntervalMasterReport_subreport0_Template.jrxml");

    }

    /**
     * Performance statistics template jrxml files are required as resources so are not compiled as part of the build process.
     * This test compiles all ps jrxml files
     * @exception
     */
    @Test
    public void compilePerformanceStatisticsReports() throws Exception {

        compileReport("PS_Summary_Template.jrxml");

        compileReport("PS_IntervalMasterReport_Template.jrxml");

        compileReport("PS_SubIntervalMasterReport.jrxml");

        compileReport("PS_SubIntervalMasterReport_subreport0.jrxml");

    }

    private Document getTemplateDocument(String xml) throws Exception{
        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderF.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        builder.setEntityResolver( getEntityResolver() );
        Document doc = builder.parse(is);
        return doc;
    }

    private EntityResolver getEntityResolver() {
        Map<String,String> idsToResources = new HashMap<String,String>();
        idsToResources.put( "http://jasperreports.sourceforge.net/dtds/jasperreport.dtd", "com/l7tech/gateway/standardreports/jasperreport.dtd");
        return new ResourceMapEntityResolver( null, idsToResources, null );
    }    
}
