package com.l7tech.policy;

/**
 * User: megery
 * Date: 2-Mar-2010
 * Time: 8:13:23 AM
 */
public class JmsDynamicProperties {
    private String jndiUrl;
    private String jndiUserName;
    private String jndiPassword;
    private String qcfName;
    private String icfName;
    private String destQName;
    private String destUserName;
    private String destPassword;
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

    public String getJndiUserName() {
        return jndiUserName;
    }
    public void setJndiUserName(String jndiUserName) {
        this.jndiUserName = jndiUserName;
    }

    public String getJndiPassword() {
        return jndiPassword;
    }
    public void setJndiPassword(String jndiPassword) {
        this.jndiPassword = jndiPassword;
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

    public String getDestUserName() {
        return destUserName;
    }
    public void setDestUserName(String destUserName) {
        this.destUserName = destUserName;
    }

    public String getDestPassword() {
        return destPassword;
    }
    public void setDestPassword(String destPassword) {
        this.destPassword = destPassword;
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
            getDestUserName(),
            getDestPassword(),
            getJndiUrl(),
            getJndiUserName(),
            getJndiPassword(),
            getIcfName(),
            getQcfName(),
            getReplytoQName()
        };
    }
}
