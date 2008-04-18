package com.l7tech.test.performance.xmlbenchmark;

import java.util.List;

/**
 * This is the interface that defines the all operations that can be executed during the XML performance benchmarking tests.
 *
 * @user: vchan
 */
public abstract class XMLBenchmarking {

    /** The configuration for one benchmark test */
    protected BenchmarkConfig config;

    /** XML message instance to test against */
    protected String xmlMessage;

    protected BenchmarkResults testResults;   //holds test results

    /**
     * Constructor.
     *
     * @param cfg configuration for the benchmark test
     */
    public XMLBenchmarking(BenchmarkConfig cfg) {
        super();

        this.config = cfg;
        this.xmlMessage = cfg.getXmlMessage();
    }

    /**
     * Runs the test.
     *
     * @throws BenchmarkException when errors are encountered during execution
     */
    public final void run() throws BenchmarkException {

        initialize();

        for (BenchmarkConfig.Operation op : config.getOperations())
        {
            if (op.isParse()) {
                this.runParsing();
            }
            else if (op.isValidate()) {
                runSchemalValidation();
            }
            if (op.isTransform()) {
                runXSLTransform();
            }
            if (op.isXPath()) {
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

        for (BenchmarkConfig.Operation op : config.getOperations() ){
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

    protected abstract void runParsing() throws BenchmarkException;

    protected abstract void runSchemalValidation() throws BenchmarkException;

    protected abstract void runXSLTransform() throws BenchmarkException;

    protected abstract void runXPath() throws BenchmarkException;




}
