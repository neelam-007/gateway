package com.l7tech.test.performance.xmlbenchmark;

import com.ximpleware.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * XML Benchmark testing for VTD which is own by Ximpleware.  It seems that Ximpleware does not have a way for
 * schema validation nor does it have XSLT, therefore no implementation will be done these two tests.
 * 
 * User: dlee
 * Date: Apr 16, 2008
 */
public class XMLBenchmarkingForVTD extends XMLBenchmarking {

    public static boolean NAMESPACE_AWARENESS = true;

    public XMLBenchmarkingForVTD(BenchmarkConfig cfg, BenchmarkOperation[] ops) {
        super(cfg, ops);
    }

    protected void initialize() throws BenchmarkException {
        try{
            super.initialize();
        }
        catch (Exception e){
            System.err.println("Failed in XMLBenchmarkingForVTD - initialize() : " + e.getMessage());
            throw new BenchmarkException(e);
        }
    }

    protected void runParsing() throws BenchmarkException {
        try{
            VTDGen vtdGen = xmlToVTDGen();

            if (vtdGen != null)
                testResults.setParsingTestPassed(true);
            else
                throw new BenchmarkException("Failed in XMLBenchmarkingForVTD - runParsing()");

        }
        catch (Exception e) {
            throw new BenchmarkException("Failed in XMLBenchmarkingForVTD - runParsing()", e);
        }
    }

    protected void runSchemalValidation() throws BenchmarkException {
        //no schema validation for ximpleware at the moment.
        testResults.setSchemaValidationTestPassed(true);
    }

    protected void runXSLTransform() throws BenchmarkException {
        //doesn't look like it has XSLT
        testResults.setXsltTestPassed(true);
    }

    protected void runXPath() throws BenchmarkException {
        try{
            VTDGen vtdGen = xmlToVTDGen();

            //have to initialize the navigator then put it into the auto pilot which is XPath
            VTDNav vtdNav = vtdGen.getNav();
            AutoPilot autoPilot = new AutoPilot(vtdNav);    //does the XPath

            //set the namespace if we are using it
            if ( NAMESPACE_AWARENESS ) {

                Iterator<String> it = config.getNamespaces().keySet().iterator();
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    autoPilot.declareXPathNameSpace(key, config.getNamespaces().get(key));
                }
            }

            List<String> xPathQueries = super.config.getXpathQueries();
            List<String> xPathResults = new ArrayList<String>();
            for (String xPathQuery : xPathQueries){
                autoPilot.resetXPath(); //documentation says to reset it each time ?
                autoPilot.selectXPath(xPathQuery);

                String result = autoPilot.evalXPathToString();
                xPathResults.add(result);
            }

            //update test results
            testResults.setXpathTestPassed(true);
            testResults.setXPathResults(xPathResults);

        } catch (Exception e){
            throw new BenchmarkException("Failed in XMLBenchmarkingForVTD - runXPath()", e);
        }

    }

    private VTDGen xmlToVTDGen() throws BenchmarkException {

        VTDGen newVTD = new VTDGen();

        try {
            if (config.isXmlFromFile()) {
                newVTD.parseFile(config.getXmlLocation(), true);
            } else {
                newVTD.setDoc(config.xmlMessage.getBytes());
                newVTD.parse(true);
            }
            return newVTD;

        } catch (EncodingException enex) {
            throw new BenchmarkException("Error in xmlToVTDGen.", enex);
        } catch (EOFException eof) {
            throw new BenchmarkException("Error in xmlToVTDGen.", eof);
        } catch (EntityException ent) {
            throw new BenchmarkException("Error in xmlToVTDGen.", ent);
        } catch (ParseException pex) {
            throw new BenchmarkException("Error in xmlToVTDGen.", pex);
        }
    }

}
