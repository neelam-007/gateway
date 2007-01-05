package com.l7tech.policy.assertion;

/**
 * A rule that determines if an http header or parameter should be forwarded, and if so,
 * which value should it have.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 5, 2007<br/>
 */
public class HttpPassthroughRule {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean usesCustomizedValue() {
        return useCustomizedValue;
    }

    public void setUsesCustomizedValue(boolean useCustomizedValue) {
        this.useCustomizedValue = useCustomizedValue;
    }

    public String getCustomizeValue() {
        return customizeValue;
    }

    public void setCustomizeValue(String customizeValue) {
        this.customizeValue = customizeValue;
    }

    private String name;
    private boolean useCustomizedValue;
    private String customizeValue;
}
