package com.l7tech.test.performance.xmlbenchmark;

/**
 * Allowed operations to be performed on the benchmark
 *
 * User: vchan
 */
public enum BenchmarkOperation {

    P("Parse"),
    V("Schema Validation"),
    T("XSL Transformation"),
    XP("XPath");

    String desc;
    private BenchmarkOperation(String description) {
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

    public static BenchmarkOperation[] all() {
        return new BenchmarkOperation[] { P, V, T, XP };
    }

}
