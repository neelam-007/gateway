/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.Aliasable;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author alex
 */
@XmlRootElement
public class Policy extends NamedEntityImp  implements HasFolder{
    private static final Logger logger = Logger.getLogger(Policy.class.getName());

    private String guid;
    private String xml;
    private PolicyType type;
    private boolean soap;
    private String internalTag;
    private Long folderOid;

    private long versionOrdinal;   // Not persisted -- filled in by admin layer
    private boolean versionActive; // Not persisted -- filled in by admin layer

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

        if(this.type == PolicyType.INCLUDE_FRAGMENT || this.type == PolicyType.INTERNAL) {
            folderOid = new Long(-5002);
        }
    }

    /**
     * Create a copy of the given policy.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param policy The policy to duplicate.
     */
    public Policy(final Policy policy) {
        this(policy, false);
    }

    /**
     * Create a copy of the given policy.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param policy The policy to duplicate.
     * @param lock true to create a read-only policy
     */
    public Policy(final Policy policy, final boolean lock) {
        super(policy);
        setSoap(policy.isSoap());
        setType(policy.getType());
        setInternalTag(policy.getInternalTag());
        setVersionActive(isVersionActive());
        setVersionOrdinal(getVersionOrdinal());
        setXml(policy.getXml());
        setGuid(policy.getGuid());
        setFolderOid(policy.getFolderOid());
        if ( lock ) lock();
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
            assertion.ownerPolicyOid(getOid());
        }

        return assertion;
    }

    public synchronized void forceRecompile() {
        assertion = null;
    }

    public String getXml() {
        return xml;
    }

    public synchronized void setXml(String xml) {
        checkLocked();
        this.xml = xml;
        this.assertion = null;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        checkLocked();
        this.type = type;
    }

    public boolean isSoap() {
        return soap;
    }

    public void setSoap(boolean soap) {
        checkLocked();
        this.soap = soap;
    }

    private void checkLocked() {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked policy");
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

    /**
     * The version ordinal, or zero if one is not set.
     * This is used for display purposes only.  It is not persisted to the database;
     * instead it is filled in when policies are returned by the admin layer.
     *
     * @return the version ordinal, or zero if not set.
     */
    public long getVersionOrdinal() {
        return versionOrdinal;
    }

    /**
     * The version ordinal, or zero if one is not set.
     * This is used for display purposes only.  It is not persisted to the database;
     * instead it is filled in by the admin layer when a single policy is returned and
     * by the SSM UI as needed.
     *
     * @param versionOrdinal the version ordinal, or zero to clear it.
     */
    public void setVersionOrdinal(long versionOrdinal) {
        checkLocked();
        this.versionOrdinal = versionOrdinal;
    }

    /**
     * Check the "Version Active" flag for this policy.
     * This is used for display purposes only.  It is not persisted to the database;
     * instead it is filled in by the admin layer when a single policy is returned and
     * by the SSM UI as needed.
     *
     * @return the "Version Active" flag
     */
    public boolean isVersionActive() {
        return versionActive;
    }

    /**
     * Set a "Version Active" flag for this policy.
     * This is used for display purposes only.  It is not persisted to the database;
     * instead it is filled in by the admin layer when a single policy is returned and
     * by the SSM UI as needed.
     *  
     * @param active the new state of the VersionActive flag
     */
    public void setVersionActive(boolean active) {
        checkLocked();
        this.versionActive = active;
    }

    /**
     * When {@link #getType()} is {@link PolicyType#INTERNAL}, this field can be used
     * to distinguish between different types of internal policy.  Try to avoid stuffing too much data into the tag,
     * as it's limited in the database to 64 characters.
     *
     * Suggested naming convention: moduleName-type[-subtype], e.g. esm-notification
     */
    public String getInternalTag() {
        return internalTag;
    }

    public void setInternalTag(String internalTag) {
        checkLocked();
        this.internalTag = internalTag;
    }

    /*
    * @return Long can be null as not all policies are associated with a folder, only policies of type
    * INCLUDE_FRAGMENT or INTERNAL are. 
    * */
    public Long getFolderOid() {
        return folderOid;
    }

    /*
    * @param folderOid, can be null as not all policies are associated with a folder, only policies of type
    * INCLUDE_FRAGMENT or INTERNAL are.
    * */
    public void setFolderOid(Long folderOid) {
        this.folderOid = folderOid;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Policy policy = (Policy)o;

        if (guid != null ? !guid.equals(policy.guid) : policy.guid != null) return false;
        if (internalTag != null ? !internalTag.equals(policy.internalTag) : policy.internalTag != null) return false;
        if (type != policy.type) return false;
        if (xml != null ? !xml.equals(policy.xml) : policy.xml != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (xml != null ? xml.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (internalTag != null ? internalTag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<policy ");
        sb.append("oid=\"").append(_oid).append("\" ");
        sb.append("type=\"").append(type).append("\" ");
        sb.append("name=\"").append(_name == null ? "" : _name).append("\"/>");
        return sb.toString();
    }

    /**
     * Recursively simplify the given assertion.
     *
     * <p>This will simplify the given assertion in a manner that is compatible
     * with the constraints of a policy (such as root must be an All).</p>
     *
     * <p>This method is only for full polcies, so the given assertion must not
     * have a parent.</p>
     *
     * @param assertion The assertion to simplify (must not be null)
     * @return The simplified assertion (which is always the given assertion)
     * @throws IllegalArgumentException if the given assertion has a parent
     * @see Assertion#simplify(Assertion,boolean) Assertion.simplify
     */
    public static Assertion simplify( final Assertion assertion ) {
        if (assertion == null) throw new IllegalArgumentException("assertion must not be null");
        if (!(assertion instanceof AllAssertion)) throw new IllegalArgumentException("assertion must be AllAssertion");
        if (assertion.getParent() != null) throw new IllegalArgumentException("assertion has a parent");

        Assertion simplified = Assertion.simplify( assertion, true );
        if ( simplified != assertion ) {
            // then we may need to re-add to the root
            if ( simplified.getParent() != assertion ) {
                AllAssertion policyRoot = (AllAssertion) assertion;
                policyRoot.clearChildren();
                policyRoot.addChild( simplified );
            }
        }

        return assertion;
    }
}
