package com.l7tech.test.performance.xmlbenchmark;

import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.cursor.RaxCursor;
import com.tarari.xml.rax.cursor.RaxCursorFactory;
import com.tarari.xml.rax.fastxpath.*;
import com.tarari.xml.xpath10.XPathContext;
import com.tarari.xml.xpath10.expr.Expression;
import com.tarari.xml.xpath10.parser.ExpressionParser;
import com.tarari.xml.xpath10.parser.XPathParseContext;
import com.tarari.xml.xslt11.Stylesheet;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This runs the Tarari Software version.  This will make straight calls to the tarari api.
 * In order to run this properly, the driver for the Tarari should be in place.  You can find the files located at
 * spock: /home/layer7/shareDrive/L7_SoftwareDistribution
 *
 * <b>NOTE:<b> FastXPath does not use namespace.  So you'll need to inform this class to use namespace or not.
 *
 * User: dlee
 * Date: Apr 18, 2008
 */
public class XMLBenchmarkingForTarariSoftware extends XMLBenchmarking {

    public static boolean NAMESPACE_AWARENESS = true;

    public XMLBenchmarkingForTarariSoftware(BenchmarkConfig cfg, BenchmarkOperation[] ops) {
        super(cfg, ops);
    }

    protected void initialize() throws BenchmarkException {
        try {
            super.initialize();
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - initialize()", e);
        }
    }

    protected void runParsing() throws BenchmarkException {
        try {
            XmlSource xmlSource = new XmlSource(config.getXmlStream());
            RaxDocument raxDocument = RaxDocument.createDocument(xmlSource);

            //pass parsing if document created
            if ( raxDocument != null ) {
                testResults.setParsingTestPassed(true);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - runParsing()", e);
        }

    }

    protected void runSchemalValidation() throws BenchmarkException {
        try {
            XmlSource xmlSource = new XmlSource(config.getXmlStream());
            RaxDocument raxDocument = RaxDocument.createDocument(xmlSource);

            boolean isValid = raxDocument.validate(); //validate the doc against the load schema

            //check validation
            if ( isValid ) {
                testResults.setSchemaValidationTestPassed(true);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - schemalValidation()", e);
        }
    }

    protected void runXSLTransform() throws BenchmarkException {
        try {
            //set style sheet used for transformation
            XmlSource xslSource = new XmlSource(config.getXsltLocation());
            Stylesheet stylesheet = Stylesheet.create(xslSource);

            //initialize xml source
            XmlSource xmlSource = new XmlSource(config.getXmlStream());

            //set result storage area
            ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
            byteArrayOutStream.reset();
            XmlResult xmlResult = new XmlResult(byteArrayOutStream);

            //transform
            stylesheet.transform(xmlSource, xmlResult);

            //update test results
            testResults.setXsltTestPassed(true);
            testResults.setXsltResults(xmlResult.getOutputStream().toString());
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - runXSLTransform()", e);
        }
    }

    protected void runXPath() throws BenchmarkException {
        try {
            //initialize xml source
            XmlSource xmlSource = new XmlSource(config.getXmlStream());
            RaxDocument raxDocument = RaxDocument.createDocument(xmlSource);

            List<String> result = new ArrayList<String>();  //hold the xpath results

            //fastxpath does not use namespace, so we need to know whether to use fastxpath or direct xpath
            if ( NAMESPACE_AWARENESS ) {
                //declare namespace
                XPathParseContext xpathParseContext = new XPathParseContext();
                Iterator<String> it = config.getNamespaces().keySet().iterator();
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    xpathParseContext.declareNamespace(key, config.getNamespaces().get(key));
                }

                ExpressionParser expressParser = new ExpressionParser(xpathParseContext);
                XPathContext xpathContext = new XPathContext();

                RaxCursorFactory raxCursorFactory = new RaxCursorFactory();
                RaxCursor cursor = raxCursorFactory.createCursor("", raxDocument);
                xpathContext.setNode(cursor);

                for (int i=0; i < config.getXpathQueries().size(); i++) {
                    Expression expression = expressParser.parseExpression(config.getXpathQueries().get(i));
                    String xpathResult = expression.toStringValue(xpathContext);
                    result.add(i, xpathResult);
                }
            }
            else {
                XPathLoader.load(new ArrayList<String>(config.getXpathQueries()));

                //process expressions
                XPathProcessor xpathProcessor = new XPathProcessor(raxDocument);
                XPathResult xpathResults = xpathProcessor.processXPaths();

                for (int i=0; i < config.getXpathQueries().size(); i++) {
                    FNodeSet fNodeSet = xpathResults.getNodeSet(i + 1); //get the result from the (i+1) expression index
                    FNode node = fNodeSet.getNode(0);   //get only the first value because of our assumption
                    result.add(i, node.getValue());
                }
            }

            //update test results
            if ( result.size() == config.getXpathQueries().size() ) {
                testResults.setXpathTestPassed(true);
                testResults.setXPathResults(result);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - runXPath()", e);
        }
    }
}
