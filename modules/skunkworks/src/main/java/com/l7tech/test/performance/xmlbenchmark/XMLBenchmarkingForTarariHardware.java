package com.l7tech.test.performance.xmlbenchmark;

import com.tarari.xml.XmlResult;
import com.tarari.xml.xslt11.Stylesheet;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.fastxpath.*;
import com.tarari.xml.XmlSource;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.xml.TarariLoader;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * This provides the implementation that will perform benchmark test for the Tarari.
 * The test will always make use of the Tarari API calls, therefore, if you enable the Tarari service, it may
 * use Tarari hardware.
 *
 * User: dlee
 * Date: Apr 15, 2008
 */
public class XMLBenchmarkingForTarariHardware extends XMLBenchmarking {

    protected GlobalTarariContextImpl gtci;

    /**
     * Initialize the variables needed to test Tarari
     */
    protected void initialize() throws BenchmarkException {

        try{
            //initialize tarari
            System.setProperty("com.l7tech.common.xml.tarari.enable", "true");    //this just initialize the probe for tarari
            gtci = (GlobalTarariContextImpl) TarariLoader.getGlobalContext();

            SchemaLoader.unloadAllSchemas();    //make sure no schema loaded
        }
        catch(Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariHardware - initialize()", e);
        }
    }

    /**
     * Default constructor
     * @param cfg
     */
    public XMLBenchmarkingForTarariHardware(BenchmarkConfig cfg, BenchmarkOperation[] ops) {
        super(cfg, ops);
    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runParsing() throws BenchmarkException {
        try{
            XmlSource xmlSource = new XmlSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));
            RaxDocument raxDoc = RaxDocument.createDocument(xmlSource);

            if ( raxDoc != null ) {
                testResults.setParsingTestPassed(true);
            }
        }
        catch (Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariHardware - runParsing()", e);
        }
    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runSchemalValidation() throws BenchmarkException {
        try{
            XmlSource xmlSource = new XmlSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));

            //create schema for validation
            SchemaLoader.loadSchema(super.config.getSchemaLocation());
            SchemaLoader.unloadAllSchemas();

            //validate
            Stylesheet styleSheet = Stylesheet.create(xmlSource);
            styleSheet.setValidate(true);
            RaxDocument raxDoc = RaxDocument.createDocument(xmlSource);

            boolean isValid = raxDoc.validate();
            testResults.setSchemaValidationTestPassed(isValid);
        }
        catch (Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariHardware - runSchemalValidation()", e);
        }

    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runXSLTransform() throws BenchmarkException {
        try{
            XmlSource xmlSource = new XmlSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));
            Stylesheet styleSheet = Stylesheet.create(xmlSource);
            styleSheet.setValidate(false);
            RaxDocument raxDoc = RaxDocument.createDocument(xmlSource);

            //load the data into the style sheet
            //xmlSource.setData(raxDoc);

            //we wont need to output the result, so just out stream it to a null stream
            String transformedResult = "";
            XmlResult result = new XmlResult(transformedResult);  //not sure if this will work
            styleSheet.transform(xmlSource, result);

            //record results
            if ( transformedResult != null && !transformedResult.equalsIgnoreCase("") ) {
                testResults.setXsltTestPassed(true);
                testResults.setXsltResults(transformedResult);
            }
        }
        catch(Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariHardware - runSchemalValidation()", e);
        }

    }

    /**
     * 
     * @throws BenchmarkException
     */
    protected void runXPath() throws BenchmarkException {
        try {
            XmlSource xmlSource = new XmlSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));
            RaxDocument raxDoc = RaxDocument.createDocument(xmlSource);

            XPathCompiler.reset();
            XPathCompiler.compile(new ArrayList(config.getXpathQueries()));

            XPathProcessor xPathProcessor = new XPathProcessor(raxDoc);
            XPathResult xPathResults = xPathProcessor.processXPaths(); //TODO: where you want the results?

            //Assumption: It is assumed that each xpath query will return one node/result
            List<String> xpathResults = new ArrayList<String>();
            for (int i =0; i < config.getXpathQueries().size(); i++){
                FNodeSet fNodeSet = xPathResults.getNodeSet(i);
                FNode fNode = fNodeSet.getNode(0);  //assumes there is only one node answer to the query
                xpathResults.add(fNode.getValue());
            }

            //record results
            if ( xpathResults.size() == config.getXpathQueries().size() ) {
                testResults.setXpathTestPassed(true);
                testResults.setXPathResults(xpathResults);
            }
        }
        catch (Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariHardware - runXPath()", e);
        }
    }
}
