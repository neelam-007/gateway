package com.l7tech.test.performance.xmlbenchmark;

import java.util.List;
import java.util.ArrayList;

/**
 * This class encapsulate the benchmark results.  It will contain basic information such as the results
 * of each test run (Fail/Pass) and the XSLT and XPath results.
 *
 * User: dlee
 * Date: Apr 16, 2008
 */
public class BenchmarkResults {

    private boolean parsingTestPassed;
    private boolean schemaValidationTestPassed;
    private boolean xsltTestPassed;
    private boolean xpathTestPassed;

    private List<String> xPathResults;
    private String xsltResults;

    /**
     * Default constructor which initializes all tests to be FAILED.
     */
    public BenchmarkResults() {
        this.parsingTestPassed = false;
        this.schemaValidationTestPassed = false;
        this.xsltTestPassed = false;
        this.xpathTestPassed = false;
        this.xPathResults = new ArrayList<String>();
        this.xsltResults = "";
    }

    /* Getters and setters */

    public boolean getParsingTestPassed() {
        return parsingTestPassed;
    }

    public void setParsingTestPassed(boolean parsingTestPassed) {
        this.parsingTestPassed = parsingTestPassed;
    }

    public boolean getXpathTestPassed() {
        return xpathTestPassed;
    }

    public void setXpathTestPassed(boolean xpathTestPassed) {
        this.xpathTestPassed = xpathTestPassed;
    }

    public boolean getXsltTestPassed() {
        return xsltTestPassed;
    }

    public void setXsltTestPassed(boolean xsltTestPassed) {
        this.xsltTestPassed = xsltTestPassed;
    }

    public boolean getSchemaValidationTestPassed() {
        return schemaValidationTestPassed;
    }

    public void setSchemaValidationTestPassed(boolean schemaValidationTestPassed) {
        this.schemaValidationTestPassed = schemaValidationTestPassed;
    }

    public List<String> getXPathResults() {
        return xPathResults;
    }

    public void setXPathResults(List<String> xPathResults) {
        this.xPathResults = xPathResults;
    }

    public String getXsltResults() {
        return xsltResults;
    }

    public void setXsltResults(String xsltResults) {
        this.xsltResults = xsltResults;
    }
}
