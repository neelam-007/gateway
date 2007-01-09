package com.l7tech.policy.assertion;

import java.io.Serializable;

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
public class HttpPassthroughRule implements Serializable {


    public HttpPassthroughRule() {
    }

    public HttpPassthroughRule(String name, boolean usesCustomizedValue, String customizeValue) {
        this.name = name;
        this.usesCustomizedValue = usesCustomizedValue;
        this.customizeValue = customizeValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUsesCustomizedValue() {
        return usesCustomizedValue;
    }

    public void setUsesCustomizedValue(boolean usesCustomizedValue) {
        this.usesCustomizedValue = usesCustomizedValue;
    }

    public String getCustomizeValue() {
        return customizeValue;
    }

    public void setCustomizeValue(String customizeValue) {
        this.customizeValue = customizeValue;
    }

    private String name;
    private boolean usesCustomizedValue;
    private String customizeValue;
}
