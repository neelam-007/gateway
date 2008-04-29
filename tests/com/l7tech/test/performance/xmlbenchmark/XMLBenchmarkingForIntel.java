package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * The benchmark test for Intel's XML Software Suite.
 * Requires: Intel-xss.jar
 * Set up VM parameter = -Djava.library.path=[libintel-xss-j.so file location]
 * Set up LD_LIBRARY_PATH = [libintel-xss-j.so file location]
 *
 * User: dlee
 * Date: Apr 21, 2008
 */
public class XMLBenchmarkingForIntel extends XMLBenchmarking {

    private SchemaValidationErrorHandler errorHandler;
    public static boolean NAMESPACE_AWARENESS = true;

    protected void initialize() throws BenchmarkException {
        try{
            super.initialize();

            //initialize the error handler for schema validation
            errorHandler = new SchemaValidationErrorHandler();
            errorHandler.reset();
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForIntel - initialize()", e);
        }
    }

    public XMLBenchmarkingForIntel(BenchmarkConfig cfg, BenchmarkOperation[] ops) {
        super(cfg, ops);
    }

    protected void runParsing() throws BenchmarkException {
        try {

            //Get a DocumentBuilderFactory through newInstance()
            DocumentBuilderFactory factory = com.intel.xml.dom.impl.ESDocumentBuilderFactory.newInstance();
                    //DocumentBuilderFactory.newInstance();

            //Create a DocumentBuilder form DocumentBuilderFactory.
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse your XML file to a DOM tree through the file URI.
            Document document = builder.parse(new InputSource(new ByteArrayInputStream(config.getXmlMessage().getBytes())));

            if ( document != null) {
                testResults.setParsingTestPassed(true);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForIntel - runParsing()", e);
        }

    }

    /**
     * This method makes use of SchemaFactory which is not thread safe, so making this method synchronized.
     *
     * @throws BenchmarkException
     */
    protected synchronized void runSchemalValidation() throws BenchmarkException {
        try {
            //create xml and xsd stream source for validation
            StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));
            StreamSource xsdSource = new StreamSource(new File(config.getSchemaLocation()));

            //get a schema factory
            SchemaFactory schemaFactory = com.intel.xml.validation.impl.SchemaFactoryImpl.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                        //        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            //create schema from the schema file
            Schema schema = schemaFactory.newSchema(xsdSource);

            //define validator from schema and set the error handler to catch any errors
            Validator validator = schema.newValidator();
            validator.reset();
            validator.setErrorHandler(errorHandler);    //error handler to catch errors
            validator.validate(xmlSource);

            //verify validation results
            if ( errorHandler.recordedErrors().isEmpty() ) {
                testResults.setSchemaValidationTestPassed(true);
            }
            else {
                //we dont really need this because the test flag will still be set to false
                throw new BenchmarkException("Validation failed.");
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForIntel - runSchemalValidation()", e);
        }
    }

    protected void runXSLTransform() throws BenchmarkException {
        try {
            //create stream source for XML and XSL and stream results container
            StreamSource xmlSource = new StreamSource(new ByteArrayInputStream(config.getXmlMessage().getBytes()));
            StreamSource xslSource = new StreamSource(new File(config.getXsltLocation()));
            ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream();
            StreamResult streamResults = new StreamResult(byteArrayOutStream);

            //get TranformerFactory
            TransformerFactory transformerFactory = com.intel.xml.transform.TransformerFactoryImpl.newInstance();

            //create the transformer and transform the xml
            Transformer transformer = transformerFactory.newTransformer(xslSource);
            transformer.transform(xmlSource, streamResults);

            //verify the transformation was done
            if ( byteArrayOutStream.toString() != null && !byteArrayOutStream.toString().equalsIgnoreCase("") ) {
                testResults.setXsltTestPassed(true);
                testResults.setXsltResults(byteArrayOutStream.toString());
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForIntel - runSchemalValidation()", e);
        }
    }

    protected void runXPath() throws BenchmarkException {
        try {
            //build document
            DocumentBuilderFactory factory = com.intel.xml.dom.impl.ESDocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(NAMESPACE_AWARENESS);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new ByteArrayInputStream(config.getXmlMessage().getBytes())));

            // Generate an XPathFactory object.
            XPathFactory xpathFactory = com.intel.xml.xpath.impl.XPathFactoryImpl.newInstance();

            // Generate an XPath object.
            XPath xpath = xpathFactory.newXPath();
            xpath.reset();
            xpath.setNamespaceContext(new NamespaceContext() {

                HashMap<String, String> nsMap = config.getNamespaces();

                public String getNamespaceURI(String prefix) {
                    if (nsMap.containsKey(prefix))
                        return nsMap.get(prefix);
                    return "";
                }

                public String getPrefix(String namespaceURI) {
                    return null;
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            // Compile an expression.
            List<String> xpathQueries = super.config.getXpathQueries();
            List<String> xpathResults = new ArrayList<String>();

            //loop through each xpath query
            for (String xpathQuery : xpathQueries){
                XPathExpression xpathExpression = xpath.compile(xpathQuery);

                String result = xpathExpression.evaluate(document);
                xpathResults.add(result);
            }

            if ( xpathResults.size() == xpathQueries.size() ) {
                testResults.setXpathTestPassed(true);
                testResults.setXPathResults(xpathResults);
            }
        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForIntel - runSchemalValidation()", e);
        }
    }
}
