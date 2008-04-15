package com.l7tech.test.performance.xmlbenchmark;

import com.l7tech.test.performance.xmlbenchmark.cfg.TestConfiguration;
import com.l7tech.test.performance.xmlbenchmark.cfg.XPathQuery;

import java.util.List;
import java.util.ArrayList;

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
        this.operations = new Operation[] {Operation.P, Operation.V, Operation.T, Operation.XP};
    }

    public String getXmlMessage() {
        return xmlMessage;
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
        this.xmlMessage = testCfg.getXmlMessage().getData();
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
}
