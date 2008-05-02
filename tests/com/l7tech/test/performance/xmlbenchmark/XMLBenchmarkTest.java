package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.test.performance.xmlbenchmark.cfg.BenchmarkConfiguration;
import com.l7tech.test.performance.xmlbenchmark.cfg.TestConfiguration;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.common.xml.xpath.FastXpath;
import com.tarari.xml.rax.fastxpath.XPathLoader;
import com.tarari.xml.rax.fastxpath.XPathCompiler;
import com.tarari.xml.rax.fastxpath.XPathCompilerException;
import com.tarari.xml.rax.schema.SchemaLoader;
import junit.framework.TestCase;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * @user: vchan
 */
public class XMLBenchmarkTest extends TestCase {

    private static final Logger logger = Logger.getLogger(XMLBenchmarkTest.class.getName());

    private static final String PROPERTY_RUN_INDEX = "layer7.xmlbench.cfg.runindex";
    private static final String PROPERTY_JAPEX_REPORTS_DIR = "japex.reportsDirectory";
    private static final String PROPERTY_JAPEX_NUMTHREADS = "japex.numberOfThreads";

    private static final String configLocation = "benchmark-config.xml";

    private static List<BenchmarkConfig> testConfigurations;

    private static int runConfigIndex;

    private static File testInfoFile;
    private static Boolean testInfoCreated = Boolean.FALSE;

    public static Boolean xpathLoaded = false;

    private BenchmarkOperation[] runOperations;

    protected void setUp() throws Exception {
        // load configuration from xml file
        XMLBenchmarkTest.setupClass();

        // create test-info file for the charting util to use (should only be done once per test run) if the
        if (!testInfoCreated && testInfoFile == null) {
            createTestInfo();
        }
    }

    protected void tearDown() throws Exception {

    }

    protected void tearDownForTarari() throws Exception {
        try {
            if ( this.runOperations[0] == BenchmarkOperation.V ){
                //System.out.println("Unloading Schema");
                SchemaLoader.unloadAllSchemas(); //clear out any schema that might be in the system already
            }
            else if ( this.runOperations[0] == BenchmarkOperation.XP ) {
                //System.out.println("Unload XPath");
                XPathLoader.unload();
                XPathCompiler.reset();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testTarariSW() {
        try {
            // need this if running JUnit locally
            // setUpSchemaForTarari();
            //     setUpXPathForTarari();
            XMLBenchmarking test = new XMLBenchmarkingForTarariSoftware(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be) {
            be.printStackTrace();
            fail();
        }
        catch (Exception ex) {
            ex.printStackTrace();
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
            XMLBenchmarking test = new XMLBenchmarkingForIntel(testConfigurations.get(runConfigIndex), runOperations);
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }

    public static void setupClass() throws Exception
    {
        try {
            if (testConfigurations == null) {
                // load configuration from xml file
               loadConfiguration();

                // check the system property for a run index passing
                parseRunIndex();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
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

    protected void setUpSchemaForTarari() throws Exception {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.V};

        try {

            if (SchemaLoader.listSchemas() == null || SchemaLoader.listSchemas().length == 0) {
                //Based on how Tarari does schema validation, it only needs to load the schema once.
                SchemaLoader.loadSchema(testConfigurations.get(runConfigIndex).getSchemaLocation()); //load schema
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setUpXPathForTarari() throws Exception {
        setUp();
        this.runOperations = new BenchmarkOperation[] {BenchmarkOperation.XP};

        if (xpathLoaded == false) {
            synchronized (xpathLoaded) {
                xpathLoaded = true;
                XPathCompiler.reset();
                //System.out.println("loading the xpaths");
                ArrayList<String> fastXpaths = new ArrayList<String>();
                ArrayList<String> directXpaths = new ArrayList<String>();
                final ArrayList<String> tempXqs = new ArrayList<String>();
                tempXqs.addAll(testConfigurations.get(runConfigIndex).getXpathQueries());

                final HashMap<String, String> namespace = new HashMap<String, String>();
                namespace.putAll(testConfigurations.get(runConfigIndex).getNamespaces());

                for (String xpath : tempXqs) {
                    try {
                        String convertXpath = TarariXpathConverter.convertToFastXpath(namespace, xpath).getExpression();
                        XPathCompiler.compile(new String[]{convertXpath});
                        fastXpaths.add(convertXpath);
                        //System.out.println("loaded : " + xpath);
                    }
                    catch (ParseException e) {
                        //System.err.println("Cannot convert to tarari normal form");
                        directXpaths.add(xpath);
                    }
                    catch (XPathCompilerException e) {
                        //System.err.println("xpath compiler exception!!");
                        directXpaths.add(xpath);
                    }
                    finally {
                        XPathCompiler.reset();
                    }
                }

                if ( !fastXpaths.isEmpty() ) {
                    XPathCompiler.compile(fastXpaths);
                }


                testConfigurations.get(runConfigIndex).setForDirectXPath(directXpaths); //store the ones later for direct xpath
            }
        }
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

    protected void createTestInfo() throws IOException
    {
        // create the test info text file for the chart util to use
        String reportDir = System.getProperty(PROPERTY_JAPEX_REPORTS_DIR);
        String numThreads = "T=" + System.getProperty(PROPERTY_JAPEX_NUMTHREADS);
        if (reportDir != null) {

            synchronized(this) {
                testInfoFile = new File(reportDir + "/" + ChartBenchmarkResults.TEST_INFO_FILE);
            }

            if (!testInfoFile.exists()) {
                FileWriter out = null;
                try {
                    out = new FileWriter(testInfoFile);
                    out.write(testConfigurations.get(runConfigIndex).getTestInfoString() + numThreads );
                    out.flush();
                    testInfoCreated = Boolean.TRUE;
                } finally {
                    try {
                        if (out != null) out.close();
                    } catch (IOException ioe) {} // ignore
                }
            }
        } else {
            testInfoCreated = Boolean.TRUE;
        }
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
