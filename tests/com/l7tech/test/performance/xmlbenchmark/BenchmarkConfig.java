package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.test.performance.xmlbenchmark.cfg.TestConfiguration;
import com.l7tech.test.performance.xmlbenchmark.cfg.XPathQuery;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * Configuration bean for one benchmark test.
 *
 * @user: vchan
 */
public class BenchmarkConfig {

    /** XML Message size threshold that determines whether the test reads the data from the file directly */
    public static final long LARGE_XML_FILE_THRESHOLD=1024*10; // in bytes (10Kb)
    
    /** Test name */
    protected String label;

    /** The XML message to run the ops agains */
    protected String xmlMessage;

    /** The size in bytes for the test message */
    protected long xmlMessageSize;

    /** The XML file location **/
    protected String xmlLocation;

    /** List the operations in execution order performed by the benchmark test */
    protected BenchmarkOperation[] operations;

    /** Schema location */
    protected String schemaLocation;

    /** XSL transform location */
    protected String xsltLocation;

    /** List of XPath query to run */
    protected List<String> xpathQueries;

    /** List of XPath query result */
    protected List<String> xpathResult;

    /** Flag specifying whether the xml message is being read from file or in-line */
    private boolean xmlFromFile;

    /**
     * Constructor.  Using all operations in the following order:
     * <ul>
     * <li>Parse message</li>
     * <li>Validate message against schema</li>
     * <li>Transform message</li>
     * <li>Run XPath queries</li>
     * </ul>
     *
     * @param testCfg one benchmark test from the test configuration
     * @throws BenchmarkException when the configuration could not be loaded properly
     */
    public BenchmarkConfig(TestConfiguration testCfg) throws BenchmarkException {
        super();
        this.label = testCfg.getName();
        loadConfiguration(testCfg);

        // by default, run all operations
        this.operations = BenchmarkOperation.all();
    }

    public String getLabel() {
        return label;
    }

    public String getXmlMessage() {
        return xmlMessage;
    }

    public long getXmlMessageSize() {
        return xmlMessageSize;
    }

    public InputStream getXmlStream() throws IOException {
        InputStream ins;

        if (xmlFromFile)
            ins = new FileInputStream(xmlLocation);
        else
            ins = new ByteArrayInputStream(xmlMessage.getBytes());

//        streams.add(ins);
        return ins;
    }

    public String getXmlLocation() {
        return xmlLocation;
    }

    public BenchmarkOperation[] getOperations() {
        return operations;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public String getXsltLocation() {
        return xsltLocation;
    }

    public List<String> getXpathQueries() {
        return xpathQueries;
    }

    public boolean isXmlFromFile() {
        return xmlFromFile;
    }

    protected void loadConfiguration(TestConfiguration testCfg) throws BenchmarkException {

        this.schemaLocation = testCfg.getSchemaLocation();
        this.xsltLocation = testCfg.getXsltLocation();
        parseXmlMessage(testCfg);
        parseXPath(testCfg.getXpathQueries().getQuery());
    }

    protected void parseXmlMessage(TestConfiguration testCfg) throws BenchmarkException {

        if ( testCfg.getXmlMessage().getData() != null ){
            //xml data is already in the config file
            this.xmlMessage = testCfg.getXmlMessage().getData();
            this.xmlMessageSize = xmlMessage.length();
            this.xmlLocation = testCfg.getXmlMessage().getLocation();
        }
        else if ( testCfg.getXmlMessage().getLocation() != null ){
            //xml data is a file at the specified location, so we'll read it off there
            this.xmlLocation = testCfg.getXmlMessage().getLocation();

            File f = new File(xmlLocation);
            if (f.exists()) {
                this.xmlMessageSize = f.length();

                if (xmlMessageSize > BenchmarkConfig.LARGE_XML_FILE_THRESHOLD) {
                    this.xmlMessage = "";
                    this.xmlFromFile = true;
                } else {
                    this.xmlMessage = getXMLDataFromFile(testCfg.getXmlMessage().getLocation());
                }
            } else {
                throw new BenchmarkException("XML message file not found: " + xmlLocation);
            }
        }
        else{
            //hopefully doesnt fall into this case
            throw new BenchmarkException("Missing xmlMessage configuration for: " + label);
        }
    }

    protected void parseXPath(List<XPathQuery> queries) {
        List<String> xpaths = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        for (XPathQuery q : queries) {
            xpaths.add(q.getXpath());
            values.add(q.getValue());
        }

        if (xpaths.size() == values.size())
        {
            this.xpathQueries = xpaths;
            this.xpathResult = values;
        }
    }

    /**
     * This will read the xml location path and read that xml file and output the entire message in string.
     * @return  The string data of the xml data.
     */
    private String getXMLDataFromFile(String xmlLocation){

        BufferedReader reader = null;
        StringBuilder xmlData = new StringBuilder("");

        try{
            //initialize reader
            reader = new BufferedReader(new FileReader(xmlLocation));
            String data = null;

            //read xml data from file
            while ( (data = reader.readLine()) != null ){
                xmlData.append(data);   //append to the final xml data product
            }
        }
        catch (IOException ioe){
            System.err.println("Failed to load XML file from : " + xmlLocation);
        }
        finally{
            try{
                //clean up - there should be some helper that can do this but lazy to look for it :P
                if (reader !=null){
                    reader.close();
                }
            }
            catch (IOException ioe){
                //nothing we can do here, should not come here!
            }
        }

        return xmlData.toString();
    }

}
