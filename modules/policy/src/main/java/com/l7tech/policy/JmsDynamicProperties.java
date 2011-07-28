package com.l7tech.policy;

/**
 * User: megery
 * Date: 2-Mar-2010
 * Time: 8:13:23 AM
 */
public class JmsDynamicProperties {
    private String jndiUrl;
    private String qcfName;
    private String icfName;
    private String destQName;
    private String replytoQName;

    //constructor
    public JmsDynamicProperties() {
    }

    public String getJndiUrl() {
        return jndiUrl;
    }

    public void setJndiUrl(String jndiUrl) {
        this.jndiUrl = jndiUrl;
    }

    public String getQcfName() {
        return qcfName;
    }

    public void setQcfName(String qcfName) {
        this.qcfName = qcfName;
    }

    public String getIcfName() {
        return icfName;
    }

    public void setIcfName(String icfName) {
        this.icfName = icfName;
    }

    public String getDestQName() {
        return destQName;
    }

    public void setDestQName(String destQName) {
        this.destQName = destQName;
    }

    public String getReplytoQName() {
        return replytoQName;
    }

    public void setReplytoQName(String replytoQName) {
        this.replytoQName = replytoQName;
    }

    /**
     * Get the variable expressions.
     *
     * @return An array containing the possibly null variable expressions.
     */
    public String[] getVariableExpressions() {
        return new String[] {
            getDestQName(),
            getJndiUrl(),
            getIcfName(),
            getQcfName(),
            getReplytoQName()
        };
    }
}
