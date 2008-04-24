package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.test.performance.xmlbenchmark.cfg.BenchmarkConfiguration;
import com.l7tech.test.performance.xmlbenchmark.cfg.TestConfiguration;
import com.tarari.xml.rax.fastxpath.XPathLoader;
import com.tarari.xml.rax.schema.SchemaLoader;
import junit.framework.TestCase;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @user: vchan
 */
public class XMLBenchmarkTest extends TestCase {

    private static final Logger logger = Logger.getLogger(XMLBenchmarkTest.class.getName());

    private static final String PROPERTY_RUN_INDEX = "layer7.xmlbench.cfg.runindex";

    private static final String configLocation = "benchmark-config.xml";

    private static List<BenchmarkConfig> testConfigurations;

    private static int runConfigIndex;

    private BenchmarkOperation[] runOperations;

    protected void setUp() throws Exception {
        // load configuration from xml file
        XMLBenchmarkTest.setupClass();
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Unit test against Tarari hardware (if available)
     */
//    public void testTarariHW() {
//        try {
//            XMLBenchmarking test = new XMLBenchmarkingForTarariHardware(testConfigurations.get(runConfigIndex), runOperations);
//            test.run();
//        }
//        catch (BenchmarkException be) {
//            be.printStackTrace();
//            fail();
//        }
//    }

    public void testTarariSW() {
        try {
            XMLBenchmarking test = new XMLBenchmarkingForTarariSoftware(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be) {
            be.printStackTrace();
            fail();
        }
    }

    /**
     * Unit test against Xerces/Xalan.
     */
    public void testXercesXalan() {
        try{
            XMLBenchmarking test = new XMLBenchmarkingForXercesXalan(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }

    /**
     * Unit test against PDOM
     */
    public void testInfonytePDOM() {
        try{
            XMLBenchmarking test = new XMLBenchmarkingForPDOM(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }

    /**
     * Unit test against VTD by Ximpleware
     */
    public void testVTD(){
        try{
            XMLBenchmarking test = new XMLBenchmarkingForVTD(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }

     /**
     * Unit test against Intel XML Software Suite
     */
    public void testIntel(){
        try{
            XMLBenchmarking test = new XMLBenchmarkingForIntel(testConfigurations.get(0), runOperations);
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }

    public static void setupClass() throws Exception
    {
        if (testConfigurations == null) {
            // load configuration from xml file
            loadConfiguration();
            
            // check the system property for a run index passing
            parseRunIndex();

            //Based on how Tarari does schema validation, it only needs to load the schema once.
            SchemaLoader.unloadAllSchemas(); //clear out any schema that might be in the system already
            SchemaLoader.loadSchema(testConfigurations.get(0).getSchemaLocation()); //load schema
            XPathLoader.unload();
        }
    }

    public static void teardownClass() throws Exception
    {
        // add anything else that needs to be executed before exit
    }

    protected void setUpParsing() throws Exception
    {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.P};
    }

    protected void setUpSchema() throws Exception
    {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.V};
    }

    protected void setUpXSLT() throws Exception
    {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.T};
    }

    protected void setUpXPath() throws Exception
    {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.XP};
    }

    private static void loadConfiguration() throws Exception
    {
        StringBuffer configXml = new StringBuffer();

        InputStreamReader inReader = new InputStreamReader( XMLBenchmarkTest.class.getResourceAsStream(configLocation) );
        BufferedReader fr = new BufferedReader(inReader);

        try {
            String oneLine;
            while ((oneLine = fr.readLine()) != null)
            {
                configXml.append(oneLine);
            }

        } catch (IOException ioe) {
            throw ioe;

        } finally {

            if (fr != null)
                fr.close();

            if (inReader != null)
                inReader.close();
        }

        try {
            JAXBContext ctx = JAXBContext.newInstance("com.l7tech.test.performance.xmlbenchmark.cfg");
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            InputSource in = new InputSource(new StringReader(configXml.toString()));
            Object obj = unmarshaller.unmarshal(in);

            if (obj != null && obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            else {
                throw new Exception("Bad benchmark-config.xml file.");
            }

            parseConfiguration(BenchmarkConfiguration.class.cast(obj));

        } catch (JAXBException jex) {

            throw jex;
        }
    }

    private static void parseConfiguration(BenchmarkConfiguration config) throws Exception
    {
        ArrayList<BenchmarkConfig> cfgList = new ArrayList<BenchmarkConfig>();

        for (TestConfiguration testCfg: config.getBenchmarkTest()) {

            cfgList.add( new BenchmarkConfig(testCfg) );
        }

        testConfigurations = cfgList;
    }

    private static void parseRunIndex() {

        String value = System.getProperty(PROPERTY_RUN_INDEX);

        if (value != null && value.length() > 0) {
            try {
                System.out.println("");

                runConfigIndex = Integer.parseInt(value);
                if (runConfigIndex < 0 || runConfigIndex >= testConfigurations.size()) {
                    System.out.println("****Cannot run test configuration (" + runConfigIndex + "): Index not found." );
                    runConfigIndex = 0;
                }

            } catch (NumberFormatException nfe) {
                // default value;
                runConfigIndex = 0;
            } // ignore
        }

        System.out.println("****Running test configuration: " + testConfigurations.get(runConfigIndex).getLabel());
    }
}
