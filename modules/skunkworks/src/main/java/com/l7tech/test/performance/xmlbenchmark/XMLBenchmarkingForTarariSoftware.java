package com.l7tech.test.performance.xmlbenchmark;

import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.XmlException;
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
    private static RaxCursorFactory raxCursorFactory = new RaxCursorFactory();

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
        RaxDocument raxDocument = null;
        XmlSource xmlSource = null;
        try {
            xmlSource = new XmlSource(config.getXmlStream());
            raxDocument = RaxDocument.createDocument(xmlSource);

            //pass parsing if document created
            if ( raxDocument != null ) {
                testResults.setParsingTestPassed(true);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - runParsing()", e);
        }
        finally {
            //cleanup
            if ( raxDocument != null && !raxDocument.isReleased() ) {
                raxDocument.release();
            }

            if ( xmlSource != null ) {
                xmlSource.cleanup();
            }
        }

    }

    protected void runSchemalValidation() throws BenchmarkException {
        RaxDocument raxDocument = null;
        XmlSource xmlSource = null;

        try {
            xmlSource = new XmlSource(config.getXmlStream());
            raxDocument = RaxDocument.createDocument(xmlSource);

            boolean isValid = raxDocument.validate(); //validate the doc against the load schema

            //check validation
            if ( isValid ) {
                testResults.setSchemaValidationTestPassed(true);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - schemalValidation()", e);
        }
        finally {
            //cleanup
            if ( raxDocument != null && !raxDocument.isReleased() ) {
                raxDocument.release();
            }

            if ( xmlSource != null ) {
                xmlSource.cleanup();
            }
        }
    }

    protected void runXSLTransform() throws BenchmarkException {
        XmlSource xslSource = null;
        XmlSource xmlSource = null;
        XmlResult xmlResult = null;

        try {
            //set style sheet used for transformation
            xslSource = new XmlSource(config.getXsltLocation());
            Stylesheet stylesheet = Stylesheet.create(xslSource);

            //initialize xml source
            xmlSource = new XmlSource(config.getXmlStream());

            //set result storage area
            ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
            byteArrayOutStream.reset();
            xmlResult = new XmlResult(byteArrayOutStream);

            //transform
            stylesheet.transform(xmlSource, xmlResult);

            //update test results
            testResults.setXsltTestPassed(true);
            testResults.setXsltResults(xmlResult.getOutputStream().toString());
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForTarariSoftware - runXSLTransform()", e);
        }
        finally {
            //clean up
            if ( xslSource != null ) {
                xslSource.cleanup();
            }

            if ( xmlSource != null  ) {
                xmlSource.cleanup();
            }

            if ( xmlResult != null ) {
                xmlResult.cleanup();
            }
        }
    }

    protected void runXPath() throws BenchmarkException {
        RaxDocument raxDocument = null;
        XmlSource xmlSource = null;

        try {
            //initialize xml source
            xmlSource = new XmlSource(config.getXmlStream());
            raxDocument = RaxDocument.createDocument(xmlSource);
 
            //just process the fastxpaths first
            List<String> result = new ArrayList<String>();  //hold the xpath results
            XPathProcessor xpathProcessor = new XPathProcessor(raxDocument);
            XPathResult xpathResults = null;

            try {
                xpathProcessor.processXPaths();
            }
            catch (XmlException xe) {
                //no xpath loaded probably
            }

            int count = config.getXpathQueries().size() - config.getForDirectXPath().size();
            //System.out.println("the count is : " + count);
            for ( int i=0; i < count; i++ ) {
                FNodeSet fNodeSet = xpathResults.getNodeSet(i+1);
                FNode node = fNodeSet.getNode(0);   //get only the first value because of our assumption
                //System.out.println("from fastxpath : " + node.getXPathValue());
                result.add(i, node.getXPathValue());
            }

            //do direct x path
            XPathParseContext xpathParseContext = new XPathParseContext();
            Iterator<String> it = config.getNamespaces().keySet().iterator();
            String key;
            while (it.hasNext()) {
                key = it.next();
                xpathParseContext.declareNamespace(key, config.getNamespaces().get(key));
            }

            ExpressionParser expressParser = new ExpressionParser(xpathParseContext);
            XPathContext xpathContext = new XPathContext();

            //RaxCursorFactory raxCursorFactory = new RaxCursorFactory();
            RaxCursor cursor = raxCursorFactory.createCursor("", raxDocument);
            xpathContext.setNode(cursor);

            final ArrayList<String> directXpaths = new ArrayList<String>();
            directXpaths.addAll(config.getForDirectXPath());
            for (int i = 0; i < directXpaths.size(); i++) {
                Expression expression = expressParser.parseExpression(directXpaths.get(i));
                String xpathResult = expression.toStringValue(xpathContext);

                //System.out.println("from direct: " + xpathResult);
                result.add(i, xpathResult);
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
        finally {
            //cleanup
            if ( raxDocument != null && !raxDocument.isReleased() ) {
                raxDocument.release();
            }

            if ( xmlSource != null ) {
                xmlSource.cleanup();
            }
        }
    }
}
