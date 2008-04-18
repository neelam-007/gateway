package com.l7tech.test.performance.xmlbenchmark;

import junit.framework.TestCase;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBElement;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.l7tech.test.performance.xmlbenchmark.cfg.TestConfiguration;
import com.l7tech.test.performance.xmlbenchmark.cfg.BenchmarkConfiguration;
import org.xml.sax.InputSource;

/**
 * @user: vchan
 */
public class XMLBenchmarkTest extends TestCase {

    private static final Logger logger = Logger.getLogger(XMLBenchmarkTest.class.getName());

    private static final String configLocation = "benchmark-config.xml";

    private static List<BenchmarkConfig> testConfigurations;

    protected void setUp() throws Exception {

        if (testConfigurations == null) {
            // load configuration from xml file
            loadConfiguration();
        }
    }

    protected void tearDown() throws Exception {
    }

    public void testTarariHW() {

    }

    public void testTarariSW() {

    }

    /**
     * Unit test against Tarari.  This method will not determine whether it is running the hardware or software version
     * of Tarari.  To test the hardware of tarari, you'll need to disable the tarari service.
     */
    public void testTarari(){
//        try {
//            XMLBenchmarking test = new XMLBenchmarkingForTarari(testConfigurations.get(0));
//            test.run();
//        }
//        catch (BenchmarkException be) {
//            be.printStackTrace();
//            fail();
//        }
    }

    /**
     * Unit test against Xerces/Xalan.
     */
    public void testXerces() {
        try{
            XMLBenchmarking test = new XMLBenchmarkingForXercesXalan(testConfigurations.get(0));
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
            XMLBenchmarking test = new XMLBenchmarkingForVTD(testConfigurations.get(0));
            test.run();
        }
        catch (BenchmarkException be){
            be.printStackTrace();
            fail();
        }
    }


    private void loadConfiguration() throws Exception
    {
        StringBuffer configXml = new StringBuffer();

        InputStreamReader inReader = new InputStreamReader( this.getClass().getResourceAsStream(configLocation) );
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

    private void parseConfiguration(BenchmarkConfiguration config) throws Exception
    {
        ArrayList<BenchmarkConfig> cfgList = new ArrayList<BenchmarkConfig>();

        for (TestConfiguration testCfg: config.getBenchmarkTest()) {

            cfgList.add( new BenchmarkConfig(testCfg) );
        }

        this.testConfigurations = cfgList;
    }
}
