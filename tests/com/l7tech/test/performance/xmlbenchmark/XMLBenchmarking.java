package com.l7tech.test.performance.xmlbenchmark;

import org.xml.sax.InputSource;

import javax.xml.transform.stream.StreamSource;
import java.util.List;
import java.io.IOException;

/**
 * This is the interface that defines the all operations that can be executed during the XML performance benchmarking tests.
 *
 * @user: vchan
 */
public abstract class XMLBenchmarking {

    /** The configuration for one benchmark test */
    protected BenchmarkConfig config;

//    /** XML message instance to test against */
//    protected String xmlMessage;

    /** The XML operations to run against the test message */
    protected BenchmarkOperation[] ops;

    /** Stores test results */
    protected BenchmarkResults testResults;

    /**
     * Constructor.
     *
     * @param cfg configuration for the benchmark test
     * @param runOps specifies the set of xml operations to run
     */
    public XMLBenchmarking(BenchmarkConfig cfg, BenchmarkOperation[] runOps) {
        super();

        this.config = cfg;
//        this.xmlMessage = cfg.getXmlMessage();

        if (runOps == null)
            this.ops = cfg.getOperations();
        else
            this.ops = runOps;
    }

    /**
     * Runs the test.
     *
     * @throws BenchmarkException when errors are encountered during execution
     */
    public final void run() throws BenchmarkException {

        initialize();

        for (BenchmarkOperation op : ops)
        {
            if (op.isParse()) {
                this.runParsing();
            }
            else if (op.isValidate()) {
                runSchemalValidation();
            }
            else if (op.isTransform()) {
                runXSLTransform();
            }
            else if (op.isXPath()) {
                runXPath();
            }
        }

        verifyTestResults();

    }

    /**
     * This method will verify the test results that is sitting in the BenchmarkResults object.
     * If all 4 test-runs flag are set to true, then it was successful in running through their tests.  However, it will
     * not know if the XPath or Transformation was done correctly.  Hence, this method will verify the test results
     * against what is expected in the benchmark-config file.
     *
     * @throws BenchmarkException   If any of the test failed, we'll throw an exception to fail the test
     */
    public final void verifyTestResults() throws BenchmarkException{

        for (BenchmarkOperation op : ops ){
            //verify for parse operation
            if ( op.isParse() && !testResults.getParsingTestPassed() ){
                throw new BenchmarkException("Parsing failed.");
            }
            else if ( op.isValidate() && !testResults.getSchemaValidationTestPassed() ){ //verify for validation
                throw new BenchmarkException("Schema Validation failed.");
            }

            //verify for transformation
            if ( op.isTransform() ){
                if ( testResults.getXsltTestPassed() ){
                    //we'll need to find a way to validate the expected results and actual results
                }
                else{
                    throw new BenchmarkException("Transformation failed.");
                }
            }

            //verify for xpath
            if ( op.isXPath() ){
                if ( testResults.getXpathTestPassed() ){
                    //compare the results to make sure the results is correct
                    List<String> expectedResults = config.xpathResult;
                    List<String> actualResults = testResults.getXPathResults();
                    if ( expectedResults.size() == actualResults.size() ){
                        for (int i=0; i < expectedResults.size(); i++){
                            if ( !expectedResults.get(i).equals(actualResults.get(i)) ){
                                throw new BenchmarkException("XPath results not accurate: expected=" + expectedResults.get(i) + "  actual=" + actualResults.get(i));
                            }
                        }
                    }
                    else{
                        throw new BenchmarkException("XPath expected results did not match the number results as the actual: expected=" + expectedResults.size() + "  actual=" + actualResults.size());
                    }
                }
                else{
                    throw new BenchmarkException("XPath failed.");
                }
            }
        }//for
    }

    protected void initialize() throws BenchmarkException {
        /* Placeholder for any initialization code  */
        testResults = new BenchmarkResults();
    }

    protected InputSource getXmlInputSource() throws IOException {
        return new InputSource(config.getXmlStream());
    }

    protected StreamSource getXmlStreamSource() throws IOException {
        return new StreamSource(config.getXmlStream());
    }

    protected StreamSource getXsltStreamSource() throws IOException {
        return new StreamSource(config.getXsltLocation());
    }

    protected abstract void runParsing() throws BenchmarkException;

    protected abstract void runSchemalValidation() throws BenchmarkException;

    protected abstract void runXSLTransform() throws BenchmarkException;

    protected abstract void runXPath() throws BenchmarkException;

}
