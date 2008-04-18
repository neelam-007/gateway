package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.common.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.io.NullOutputStream;
import com.tarari.xml.XmlResult;
import com.tarari.xml.xslt11.Stylesheet;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.fastxpath.*;
import com.tarari.xml.XmlSource;

import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * This provides the implementation that will perform benchmark test for the Tarari.
 * The test will always make use of the Tarari API calls, therefore, if you enable the Tarari service, it may
 * use Tarari hardware.  If you have disabled Tarari service then it will use the software version.  Please be sure
 * to define the Tarari settings accordingly to your test case.
 *
 * User: dlee
 * Date: Apr 15, 2008
 */
public class XMLBenchmarkingForTarari extends XMLBenchmarking {

    protected GlobalTarariContextImpl gtci;
    protected RaxDocument raxDoc;
    protected XmlSource xmlSource;
    protected Stylesheet styleSheet;

    /**
     * Initialize the variables needed to test Tarari
     */
    protected void initialize() throws BenchmarkException {

        try{
            //initialize tarari
            System.setProperty("com.l7tech.common.xml.tarari.enable", "true");    //this just initialize the probe for tarari
            gtci = (GlobalTarariContextImpl) TarariLoader.getGlobalContext();

            xmlSource = new XmlSource(new FileInputStream(super.config.getXmlMessage()));
            styleSheet = Stylesheet.create(xmlSource);

            SchemaLoader.unloadAllSchemas();    //make sure no schema loaded
        }
        catch(Exception e){
            System.err.println("Failed in XMLBenchmarkingForTarari - initialize() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }

    /**
     * Default constructor
     * @param cfg
     */
    public XMLBenchmarkingForTarari(BenchmarkConfig cfg) {
        super(cfg);
    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runParsing() throws BenchmarkException {
        try{
            styleSheet.setValidate(false);
            raxDoc = RaxDocument.createDocument(xmlSource);

            testResults.setParsingTestPassed(true);
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForTarari - runParsing() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runSchemalValidation() throws BenchmarkException {
        try{
            styleSheet.setValidate(true);    //not sure what this will actually do
            SchemaLoader.loadSchema(super.config.getSchemaLocation());
            boolean isValid = raxDoc.validate();

            testResults.setSchemaValidationTestPassed(isValid);
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForTarari - runSchemalValidation() : " + e.getMessage());
            throw new BenchmarkException(e);
        }

    }

    /**
     *
     * @throws BenchmarkException
     */
    protected void runXSLTransform() throws BenchmarkException {
        try{
            //load the data into the style sheet
            //xmlSource.setData(raxDoc);

            styleSheet.setValidate(false);

            //we wont need to output the result, so just out stream it to a null stream
            String transformedResult = "";
            XmlResult result = new XmlResult(transformedResult);  //not sure if this will work
            styleSheet.transform(xmlSource, result);

            //record results
            testResults.setXsltTestPassed(true);
            testResults.setXsltResults(transformedResult);
        }
        catch(Exception e){
            System.err.println("Failed in XMLBenchmarkingForTarari - runSchemalValidation() : " + e.getMessage());
            throw new BenchmarkException(e);
        }

    }

    /**
     * 
     * @throws BenchmarkException
     */
    protected void runXPath() throws BenchmarkException {
        try {
            String[] expressions = (String[]) super.config.getXpathQueries().toArray();
            XPathCompiler.reset();
            XPathCompiler.compile(expressions);

            XPathProcessor xPathProcessor = new XPathProcessor(raxDoc);
            XPathResult xPathResults = xPathProcessor.processXPaths(); //TODO: where you want the results?

            //Assumption: It is assumed that each xpath query will return one node/result
            List<String> xpathResults = new ArrayList<String>();
            for (int i =0; i < expressions.length; i++){
                FNodeSet fNodeSet = xPathResults.getNodeSet(i);
                FNode fNode = fNodeSet.getNode(0);  //assumes there is only one node answer to the query
                xpathResults.add(fNode.getValue());
            }

            //record results
            testResults.setXpathTestPassed(true);
            testResults.setXPathResults(xpathResults);              
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForTarari - runXPath() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }
}
