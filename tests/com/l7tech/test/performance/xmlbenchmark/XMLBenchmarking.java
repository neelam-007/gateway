package com.l7tech.test.performance.xmlbenchmark;

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
    }

    protected void initialize() {
        /* Placeholder for any initialization code  */
    }

    protected abstract void runParsing() throws BenchmarkException;

    protected abstract void runSchemalValidation() throws BenchmarkException;

    protected abstract void runXSLTransform() throws BenchmarkException;

    protected abstract void runXPath() throws BenchmarkException;

}
