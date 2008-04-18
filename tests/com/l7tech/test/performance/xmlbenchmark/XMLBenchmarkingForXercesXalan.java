package com.l7tech.test.performance.xmlbenchmark;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;


/**
 * Benchmark test on Xerces/Xalan.
 *
 * User: dlee
 * Date: Apr 15, 2008
 */
public class XMLBenchmarkingForXercesXalan extends XMLBenchmarking {

    private Document document;
    public static final boolean NAMESPACE_AWARENESS = true;
    SchemaValidationErrorHandler errorHandler;

    /**
     * Initialize benchmark config file
     * @param cfg    Configuration file used for the benchmark test
     */
    public XMLBenchmarkingForXercesXalan(BenchmarkConfig cfg) {
        super(cfg);
    }

    /**
     * Initialize the document that will be used in the XPath part of the test.
     * @throws BenchmarkException
     */
    protected void initialize() throws BenchmarkException {
        try{
            super.initialize();

            //initialize the error handler for schema validation
            errorHandler = new SchemaValidationErrorHandler();
            errorHandler.reset();

            //create document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactoryImpl.newInstance();
            docBuilderFactory.setNamespaceAware(NAMESPACE_AWARENESS);

            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(new InputSource(new ByteArrayInputStream(xmlMessage.getBytes())));

        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForXercesXalan - initialize() : " + e.getMessage());
            throw new BenchmarkException(e);
        }

    }
        
    protected void runParsing() throws BenchmarkException {
        try {
            //parse using DOM
            DOMParser domParser = new DOMParser();
            domParser.reset();
            domParser.parse(new InputSource(new ByteArrayInputStream(xmlMessage.getBytes())));
            testResults.setParsingTestPassed(true);
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForXercesXalan - runParsing() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }

    protected void runSchemalValidation() throws BenchmarkException {
        try { /*
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(NAMESPACE_AWARENESS);
            saxFactory.setValidating(true);

            //need to verify if features are set properly for our testing purposes
            saxFactory.setFeature("http://xml.org/sax/features/validation",true);
            saxFactory.setFeature("http://apache.org/xml/features/validation/schema",true);
            saxFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
                                                                
            SAXParser saxParser = saxFactory.newSAXParser();
            saxParser.reset();
            saxParser.getParser().setErrorHandler(errorHandler); //listen on any errors during validation process
                                                                               
            saxParser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", super.config.getSchemaLocation());
            saxParser.getParser().parse(new InputSource(new ByteArrayInputStream(xmlMessage.getBytes())));
                */
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactoryImpl.newInstance();
            docBuilderFactory.setNamespaceAware(NAMESPACE_AWARENESS);
            docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", super.config.getSchemaLocation());
            docBuilderFactory.setValidating(true);

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            builder.parse(new InputSource(new ByteArrayInputStream(xmlMessage.getBytes())));


            //check if there were any errors during the validation, throw exception to fail the test if not valid
            if ( errorHandler.recordedErrors().isEmpty() ){
                testResults.setSchemaValidationTestPassed(true);
            }
            else{
                throw new BenchmarkException("Validation failed.");
            }
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForXercesXalan - runSchemalValidation() : " + e.getMessage());
            throw new BenchmarkException(e);
        }

    }

    protected void runXSLTransform() throws BenchmarkException {
        try{
            //create the stream source for the XML and XSL
            StreamSource xslStreamSource = new StreamSource(new File(super.config.getXsltLocation()));
            StreamSource xmlStreamSource = new StreamSource(new ByteArrayInputStream(xmlMessage.getBytes()));

            //initialize the transformer used for the transformation
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(xslStreamSource);
            transformer.reset();

            //we'll need the output in string format, so we'll create bytearrayoutputstream to output the data for us
            ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
            StreamResult streamResults = new StreamResult(byteArrayOutStream);

            //begin transformation
            transformer.transform(xmlStreamSource, streamResults);
            testResults.setXsltResults(byteArrayOutStream.toString());
            testResults.setXsltTestPassed(true);    //we dont know the transformation correctness until checked
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForXercesXalan - runXSLTransform() : " + e.getMessage());
            throw new BenchmarkException(e);
        }

    }

    protected void runXPath() throws BenchmarkException {
        try{
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            xpath.reset();
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return "http://l7tech.com/xmlbench";
                }

                public String getPrefix(String namespaceURI) {
                    return "ns1";
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            List<String> xpathQueries = super.config.getXpathQueries();
            List<String> xpathResults = new ArrayList<String>();

            //loop through each xpath query
            for (String xpathQuery : xpathQueries){
                XPathExpression xpathExpression = xpath.compile(xpathQuery);

                String result = xpathExpression.evaluate(document);
                xpathResults.add(result);
            }

            testResults.setXpathTestPassed(true);
            testResults.setXPathResults(xpathResults);
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForXercesXalan - runXPath() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }
}
