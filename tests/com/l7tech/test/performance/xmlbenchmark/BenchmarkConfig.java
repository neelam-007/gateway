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

    /**
     * Allowed operations to be performed on the benchmark
     */
    public enum Operation {
        P("Parse"),
        V("Schema Validation"),
        T("XSL Transformation"),
        XP("XPath");

        String desc;
        private Operation(String description) {
            this.desc = description;
        }

        public boolean isParse() {
            return P.equals(this);
        }

        public boolean isValidate() {
            return V.equals(this);
        }

        public boolean isTransform() {
            return T.equals(this);
        }

        public boolean isXPath() {
            return XP.equals(this);
        }
    }

    /** Test name */
    protected String label;

    /** The XML message to run the ops agains */
    protected String xmlMessage;

    /** The XML file location **/
    protected String xmlLocation;

    /** List the operations in execution order performed by the benchmark test */
    protected Operation[] operations;

    /** Schema location */
    protected String schemaLocation;

    /** XSL transform location */
    protected String xsltLocation;

    /** List of XPath query to run */
    protected List<String> xpathQueries;

    /** List of XPath query result */
    protected List<String> xpathResult;

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
     */
    public BenchmarkConfig(TestConfiguration testCfg) {
        super();
        this.label = testCfg.getName();
        loadConfiguration(testCfg);
        //this.operations = new Operation[] {Operation.P, Operation.V, Operation.T, Operation.XP};
        this.operations = new Operation[] { Operation.P, Operation.V, Operation.T, Operation.XP };
    }

    public String getXmlMessage() {
        return xmlMessage;
    }

    public String getXmlLocation() {
        return xmlLocation;
    }

    public Operation[] getOperations() {
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

    protected void loadConfiguration(TestConfiguration testCfg)
    {
        if ( testCfg.getXmlMessage().getData() != null ){
            //xml data is already in the config file
            this.xmlMessage = testCfg.getXmlMessage().getData();
            this.xmlLocation = testCfg.getXmlMessage().getLocation();
        }
        else if ( testCfg.getXmlMessage().getLocation() != null ){
            //xml data is a file at the specified location, so we'll read it off there
            this.xmlMessage = getXMLDataFromFile(testCfg.getXmlMessage().getLocation());
            this.xmlLocation = testCfg.getXmlMessage().getLocation();
        }
        else{
            //hopefully doesnt fall into this case
            this.xmlMessage = "";
            this.xmlLocation = "";
        }

        this.schemaLocation = testCfg.getSchemaLocation();
        this.xsltLocation = testCfg.getXsltLocation();
        parseXPath(testCfg.getXpathQueries().getQuery());
    }

    protected void parseXPath(List<XPathQuery> queries)
    {
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
