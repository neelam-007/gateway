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

    public String getFieldsAsVariables() {
        String SEP = " ";
        StringBuffer sb = new StringBuffer();
        if (getDestQName() != null) sb.append(getDestQName()).append(SEP);
        if (getJndiUrl() != null) sb.append(getJndiUrl()).append(SEP);
        if (getIcfName() != null) sb.append(getIcfName()).append(SEP);
        if (getQcfName() != null) sb.append(getQcfName()).append(SEP);
        if (getReplytoQName() != null) sb.append(getReplytoQName()).append(SEP);
        return sb.toString();
    }
}
