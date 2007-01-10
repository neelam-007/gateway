package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * Set of rules for forwarding or backwarding http headers or parameters.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 8, 2007<br/>
 */
public class HttpPassthroughRuleSet implements Serializable  {
    private boolean forwardAll;
    private HttpPassthroughRule[] rules;


    public HttpPassthroughRuleSet() {
        this.forwardAll = false;
        this.rules = new HttpPassthroughRule[]{};
    }

    public HttpPassthroughRuleSet(boolean forwardAll, HttpPassthroughRule[] rules) {
        this.forwardAll = forwardAll;
        this.rules = rules;
        if (rules == null) throw new IllegalArgumentException("dont pass null arrays");
    }

    public boolean isForwardAll() {
        return forwardAll;
    }

    public void setForwardAll(boolean forwardAll) {
        this.forwardAll = forwardAll;
    }

    public HttpPassthroughRule[] getRules() {
        return rules;
    }

    public void setRules(HttpPassthroughRule[] rules) {
        this.rules = rules;
    }

    /**
     * remove a customized header/parameter
     * @param name the name of the header/parameter to remove
     */
    public void remove(String name) {
        if (rules != null) {
            int deleted = 0;
            for (int i = 0; i < rules.length; i++) {
                if (rules[i].getName().compareToIgnoreCase(name) == 0) {
                    rules[i] = null;
                    ++deleted;
                }
            }
            if (deleted > 0) {
                HttpPassthroughRule[] tmp = new HttpPassthroughRule[rules.length-deleted];
                int j = 0;
                for (int i = 0; i < tmp.length; i++) {
                    while (rules[j] == null) j++;
                    tmp[i] = rules[j];
                    j++;
                }
                rules = tmp;
            }
        }
    }
}
