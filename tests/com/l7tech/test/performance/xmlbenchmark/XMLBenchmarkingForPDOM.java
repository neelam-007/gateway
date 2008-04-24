package com.l7tech.test.performance.xmlbenchmark;

import com.infonyte.ds.DataServerException;
import com.infonyte.ds.fds.FileDataServerFactory;
import com.infonyte.jaxp.PrefixResolver;
import com.infonyte.pdom.PDOM;
import com.infonyte.pdom.PDOMFactory;
import com.infonyte.pdom.PDOMParser;
import com.infonyte.pdom.PDOMParserFactory;
import com.infonyte.xpath.XPathExpression;
import com.infonyte.xpath.XPathFactory;
import com.infonyte.xpath.XValue;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * XMLBenchmarking implementation for PDOM package (http://www.infonyte.com/en/prod_pdom.html).
 *
 * User: vchan
 */
public class XMLBenchmarkingForPDOM extends XMLBenchmarking {

//    private Logger logger = Logger.getLogger(XMLBenchmarkingForPDOM.class);

    public static boolean NAMESPACE_AWARENESS = true;

    public static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private static final String PDOM_FILE_LOCATION = ".";
    private static final String PDOM_FILE_SUFFIX_FOR_PARSING = "forParsing";
    private static final String PDOM_FILE_SUFFIX_FOR_SCHEMA_VAL = "forSchemaVal";
    private static final String PDOM_FILE_SUFFIX_FOR_XSLT = "forXSLT";
    private static final String PDOM_FILE_SUFFIX_FOR_XPATH = "forXPath";

//    private Document doc;
    private PDOMUtil pdomUtil;

    public XMLBenchmarkingForPDOM(BenchmarkConfig cfg, BenchmarkOperation[] ops) {
        super(cfg, ops);
    }

    protected void initialize() throws BenchmarkException {
        super.initialize();

        this.pdomUtil = new PDOMUtil();
    }

    protected void runParsing() throws BenchmarkException {

        PDOM pdom = null;
        try {
            // create the PDOM
            pdom = createPDOM( createPDOMFile(PDOM_FILE_SUFFIX_FOR_PARSING), false );
            Document doc = pdom.getDocument();

            // traverse the tree
            if (doc != null) {
//                System.out.println("parsed root: " + doc.getFirstChild().getNodeName());

                testResults.setParsingTestPassed(true);
            } else {
                throw new BenchmarkException("Parsing failed.");
            }

        } finally {
            cleanup(pdom);
        }
    }

    protected void runSchemalValidation() throws BenchmarkException {

        PDOM pdom = null;
        try {
            // create the PDOM
            pdom = createPDOM( createPDOMFile(PDOM_FILE_SUFFIX_FOR_SCHEMA_VAL), true );
            Document doc = pdom.getDocument();

            // traverse the tree
            if (doc != null) {
//                System.out.println("validated root: " + doc.getFirstChild().getNodeName() + "; isValid=" + pdom.isValid(doc));

                testResults.setSchemaValidationTestPassed(true);
            } else {
                throw new BenchmarkException("Validation failed.");
            }

        } finally {
            cleanup(pdom);
        }
    }

    protected void runXSLTransform() throws BenchmarkException {

        PDOM pdom = null;
        try {
            pdom = createPDOM( createPDOMFile(PDOM_FILE_SUFFIX_FOR_XSLT), false );

            TransformerFactory tf = new com.infonyte.jaxp.TransformerFactory();
            Templates xsltemp = tf.newTemplates(new StreamSource(new FileInputStream(new File(config.xsltLocation))));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(output);
            Transformer transformer = xsltemp.newTransformer();
            transformer.transform(new DOMSource(pdom.getDocument()), result);
            testResults.setXsltResults(output.toString());
//            System.out.println("xslt output:" + output.toString());

        } catch (TransformerConfigurationException tcex) {
            tcex.printStackTrace();
            throw new BenchmarkException("Parsing failed.", tcex);

        } catch (TransformerException tex) {
            tex.printStackTrace();
            throw new BenchmarkException("Parsing failed.", tex);

        } catch (FileNotFoundException fex) {
            fex.printStackTrace();
            throw new BenchmarkException("Parsing failed.", fex);

        } finally {
            cleanup(pdom);
        }

        testResults.setXsltTestPassed(true);
    }

    protected void runXPath() throws BenchmarkException {

        PDOM pdom = null;

        try {
            pdom = createPDOM( createPDOMFile(PDOM_FILE_SUFFIX_FOR_XPATH), false );
            List<String> resultList = new ArrayList<String>();
            XPathExpression expr;
            XValue value;

            for (String query : config.getXpathQueries()) {

                expr = XPathFactory.newExpression(query, new PrefixResolver() {

                    HashMap<String, String> nsMap = config.getNamespaces();

                    public String getNamespaceForPrefix(String prefix) {
                        if (nsMap.containsKey(prefix))
                            return nsMap.get(prefix);
                        return "";
                    }
                });
                value = expr.eval(pdom.getDocument());
                if (value != null) {
                    resultList.add(value.asString());
                }
            }

            if (config.getXpathQueries().size() == resultList.size()) {
                testResults.setXPathResults(resultList);
                testResults.setXpathTestPassed(true);
            }

        } finally {
            cleanup(pdom);
        }

    }

    protected File createPDOMFile(String suffix) throws BenchmarkException {

        File newFile = new File(PDOM_FILE_LOCATION + config.getLabel() + suffix);
        if (newFile.exists()) {
            if (!newFile.delete())
                throw new BenchmarkException("Cannot remove pdom_file.");
        }
        newFile.deleteOnExit();
        return newFile;
    }

    protected PDOM createPDOM(File pdom_file, boolean validate) throws BenchmarkException {

        try {
            // Step 1: Creating the PDOM
            PDOM pdom = pdomUtil.createConfiguredPDOMFactory().create(pdom_file);

            // Step 2: Create and Configure (or re-use) a PDOMParserFactory
            // Step 3: Create a new PDOMParser
            PDOMParser parser = pdomUtil.getParser(validate);

            // Step 4: invoke parser
//            DefaultHandler dh = new DefaultHandler() {
//                private void print(SAXParseException x) {
//
//                    MessageFormat message = new MessageFormat("({0}: {1}, {2}): {3}");
//                    String msg = message.format(new Object[]
//                            {
//                                    x.getSystemId(),
//                                    new Integer(x.getLineNumber()),
//                                    new Integer(x.getColumnNumber()),
//                                    x.getMessage()
//                            });
//                    System.out.println(msg);
//                }
//
//                public void warning(SAXParseException x) {
//                    print(x);
//                }
//
//                public void error(SAXParseException x) {
//                    print(x);
//                }
//
//                public void fatalError(SAXParseException x) throws SAXParseException {
//                    print(x);
//                    throw x;
//                }
//            };
//            parser.getSAXParser().getXMLReader().setErrorHandler(dh);
//            parser.getSAXParser().parse(new InputSource(new ByteArrayInputStream(msg.getBytes())), dh);

            parser.parse(getXmlInputSource(), pdom);

            // Step 5: Committing changes
            pdom.commit();

            return pdom;

        } catch (DataServerException dex) {
            dex.printStackTrace();
            throw new BenchmarkException("Failed to create PDOM.", dex);

        } catch (SAXException sax) {
            sax.printStackTrace();
            throw new BenchmarkException("Failed to create PDOM.", sax);

        } catch (ParserConfigurationException pex) {
            pex.printStackTrace();
            throw new BenchmarkException("Failed to create PDOM.", pex);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new BenchmarkException("Failed to create PDOM.", ioe);
        }
    }


    protected void cleanup(PDOM pdom)
    {
        try {
            if (pdom != null)
                pdom.close();
        } catch (DataServerException dse) {
            // ignore
        }
    }

    /**
     * PDOM helpers.
     */
    protected class PDOMUtil {

        private PDOMFactory pdomFactory;
        private PDOMParserFactory validatingParserFactory;
        private PDOMParserFactory nonValidatingParserFactory;

        public PDOMFactory getFactory() {
            if (pdomFactory == null)
                this.pdomFactory = createConfiguredPDOMFactory();
            return pdomFactory;
        }

        public PDOMParser getParser(boolean validating) throws SAXException, ParserConfigurationException {

            PDOMParser parser;
            if (validating) {
                parser = getValidatingParserFactory().newParser();
                parser.getSAXParser().setProperty(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
                parser.getSAXParser().setProperty(SCHEMA_SOURCE, config.getSchemaLocation());

            } else {
                parser = getNonValidatingParserFactory().newParser();
            }
            return parser;
        }

        private PDOMParserFactory getValidatingParserFactory() {
            if (validatingParserFactory == null) {
                this.validatingParserFactory = createConfiguredPDOMParserFactory(true);
                this.validatingParserFactory.setFeature("http://apache.org/xml/features/validation/schema",true);
                this.validatingParserFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
            }
            return validatingParserFactory;
        }

        private PDOMParserFactory getNonValidatingParserFactory() {
            if (nonValidatingParserFactory == null) {
                this.nonValidatingParserFactory = createConfiguredPDOMParserFactory(false);
            }
            return nonValidatingParserFactory;
        }

        protected PDOMFactory createConfiguredPDOMFactory() {

            // Step 1: Creating and Configuring a DataServerFactory
            FileDataServerFactory data_server_factory = new FileDataServerFactory();
            configureDataServerFactory(data_server_factory);

            // Step 2: Creating and Configuring a PDOMFactory that uses the DataServerFactory
            PDOMFactory pdom_factory = new PDOMFactory(data_server_factory);
            configurePDOMFactory(pdom_factory);
            return pdom_factory;
        }

        protected void configureDataServerFactory(FileDataServerFactory f) {
            // Configuration
            // 1.1 Setting the automatic file defragmentation threshold value in % (used/unused file space).
            f.setAutoDefragmentationThreshold(33);
            // 1.2. Disabling/Enabling caching of file data
            f.setCacheFileData(false);
            // 1.3. Disabling/Enabling caching of file handles
            f.setCacheFileHandle(false);
            // 1.4. Disabling/Enabling compression
            f.setCompressSegments(false);
            // 1.5. Disabling/Enabling lock file based inter-process synchronization
            f.setLockFile(false);
            // 1.6. Disabling/Enabling opening in read-only mode
            f.setOpenFileReadonly(false);
        }

        // Configuration of a PDOMFactory.
        protected void configurePDOMFactory(PDOMFactory f) {
            // Configuration
            // 1. Enabling/Disabling maintenance of structure indices for newly created PDOMs.
            f.setMaintainStructureIndex(true);
        }

        protected PDOMParserFactory createConfiguredPDOMParserFactory(boolean isValidating) {

            PDOMParserFactory newFactory = new PDOMParserFactory();

            // try with xerces SAX parser
//            newFactory.setSAXParserFactory(SAXParserFactory.newInstance());

            // Enabling/Disabling creation of DTD DOM node
            newFactory.setCreateDTDNodes(false);
            // Enabling/Disabling creation of Entity Reference nodes
            newFactory.setCreateEntityReferenceNodes(false);
            // Define how mere whitespace nodes should be reflected when parsing
            newFactory.setWhitespacePolicy(PDOMParser.SKIP_IGNOREABLE_WS);
            // Applications can set the SAXPArserFactory to use
            // pdom_parser_factory.setSAXParserFactory(...);
            // and/or configure the SAXPArserFactory used
            SAXParserFactory sax_parser_factory = newFactory.getSAXParserFactory();
            // Enabling/Disabling XML validation
            sax_parser_factory.setValidating(isValidating);
            // Enabling/Disabling XML namespace support
            sax_parser_factory.setNamespaceAware(NAMESPACE_AWARENESS);
            return newFactory;
        }
    }
}
