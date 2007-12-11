/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class Policy extends NamedEntityImp {
    private static final Logger logger = Logger.getLogger(Policy.class.getName());

    private String xml;
    private PolicyType type;
    private boolean tarariWanted;
    private boolean wssInPolicy;
    private boolean soap;

    private transient Assertion assertion;
    private static final AllAssertion DISABLED_POLICY = new AllAssertion(Arrays.asList(new CommentAssertion("Policy disabled"), new FalseAssertion()));
    private static final String DISABLED_POLICY_XML = WspWriter.getPolicyXml(DISABLED_POLICY).trim();

    @Deprecated // For Serialization and persistence only
    public Policy() {
    }

    public Policy(PolicyType type, String name, String xml, boolean soap) {
        this.type = type;
        this._name = name;
        this.xml = xml;
        this.soap = soap;
    }

    /**
     * Parses the policy if necessary, and returns the {@link Assertion} at its root.
     *
     * @return the {@link Assertion} at the root of the policy. May be null.
     * @throws IOException if the policy cannot be deserialized
     */
    public synchronized Assertion getAssertion() throws IOException {
        if (xml == null || xml.length() == 0) {
            logger.warning(MessageFormat.format("Policy #{0} ({1}) has an invalid or empty policy_xml field.  Using null policy.", _oid, _name));
            return FalseAssertion.getInstance();
        }

        if (assertion == null) {
            assertion = WspReader.getDefault().parsePermissively(xml);
            assertion.setOwnerPolicyOid(getOid());
            updatePolicyHints(assertion);
        }

        return assertion;
    }

    /* Caller must hold lock */
    private void updatePolicyHints(Assertion rootAssertion) {
        // TODO split request/response into separate flags
        tarariWanted = false;
        Iterator i = rootAssertion.preorderIterator();
        while (i.hasNext()) {
            Assertion ass = (Assertion) i.next();
            if (ass instanceof SchemaValidation || ass instanceof XslTransformation) {
                tarariWanted = true;
            } else if ( Assertion.isRequest(ass) && Assertion.isWSSecurity(ass)) {
                wssInPolicy = true;
            }
        }
        rootAssertion.setOwnerPolicyOid(getOid());
    }

    /**
     * @return true if at least one assertion in this service's policy strongly prefers to use Tarari rather than use a
     * pre-parsed DOM tree.
     */
    public boolean isTarariWanted() {
        return tarariWanted;
    }

    /**
     * @return true if there is a WSS assertion in this services policy, in which case DOM would likely be better than
     * using Tarari.
     */
    public boolean isWssInPolicy() {
        return wssInPolicy;
    }

    public synchronized void forceRecompile() {
        assertion = null;
    }

    public String getXml() {
        return xml;
    }

    public synchronized void setXml(String xml) {
        this.xml = xml;
        this.assertion = null;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public boolean isSoap() {
        return soap;
    }

    public void setSoap(boolean soap) {
        this.soap = soap;
    }

    /**
     * Disable this policy.
     * Currently this will replace the policy XML with a version that always fails.
     * To reenable a disabled policy you currently must set the policy XML to something that works.
     */
    public void disable() {
        // TODO find better way to disable policies
        setXml(DISABLED_POLICY_XML);
    }

    /**
     * Detect if this policy has been disabled by calling {@link #disable}.
     * @return true if this policy is currently disabled.  To reenable it, set a different policy xml.
     */
    public boolean isDisabled() {
        String pxml = getXml();
        return pxml == null || pxml.trim().equals(DISABLED_POLICY_XML); // trim() currently not optional
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Policy policy = (Policy) o;

        if (type != policy.type) return false;
        if (xml != null ? !xml.equals(policy.xml) : policy.xml != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (xml != null ? xml.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("<policy ");
        sb.append("oid=\"").append(_oid).append("\" ");
        sb.append("type=\"").append(type).append("\" ");
        sb.append("name=\"").append(_name == null ? "" : _name).append("\"/>");
        return sb.toString();
    }
}
