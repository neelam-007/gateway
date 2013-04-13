package com.l7tech.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Flushable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * @author alex
 */
@XmlRootElement
public class Policy extends ZoneableNamedEntityImp implements Flushable, HasFolder {
    private static final Logger logger = Logger.getLogger(Policy.class.getName());

    private static WspReader.Visibility defaultVisibility = WspReader.INCLUDE_DISABLED;

    private String guid;
    private String xml;
    private PolicyType type;
    private boolean soap;
    private String internalTag;
    private Folder folder;

    private long versionOrdinal;   // Not persisted -- filled in by admin layer
    private boolean versionActive; // Not persisted -- filled in by admin layer

    private transient WspReader.Visibility visibility; // transient since should depend on environment (authoring vs. production), not on the policy itself
    private transient Assertion assertion;

    private static final AllAssertion DISABLED_POLICY = new AllAssertion(Arrays.asList(new CommentAssertion("Policy disabled"), new FalseAssertion()));
    private static final String DISABLED_POLICY_XML = WspWriter.getPolicyXml(DISABLED_POLICY).trim();

    @Deprecated // For Serialization and persistence only
    protected Policy() { }

    public Policy(PolicyType type, String name, String xml, boolean soap) {
        this.type = type;
        this._name = name;
        this.xml = xml;
        this.soap = soap;
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
        this(policy, null, false, null);
    }

    /**
     * Create a copy of the given policy, optionally overridding its visibility setting and/or locking it.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param policy The policy to duplicate.
     * @param visibility visibility setting to use for new policy, or null to use the setting from the copied policy.
     * @param lock true to create a read-only policy
     */
    public Policy(final Policy policy, @Nullable WspReader.Visibility visibility, final boolean lock, @Nullable DesignTimeEntityProvider entityProvider) {
        super(policy);
        setSoap(policy.isSoap());
        setType(policy.getType());
        setInternalTag(policy.getInternalTag());
        setVersionActive(isVersionActive());
        setVersionOrdinal(getVersionOrdinal());
        setXml(policy.getXml());
        setGuid(policy.getGuid());
        setFolder(policy.getFolder());
        setVisibility(visibility != null ? visibility : policy.getVisibility());
        if (entityProvider != null)
            provideEntitiesToAssertionBeans(entityProvider);
        if ( lock ) lock();
    }

    /**
     * Parses the policy if necessary, and returns the {@link Assertion} at its root.
     *
     * @return the {@link Assertion} at the root of the policy. May be null.
     * @throws IOException if the policy cannot be deserialized
     */
    public synchronized @Nullable Assertion getAssertion() throws IOException {
        if (xml == null || xml.length() == 0) {
            logger.warning(MessageFormat.format("Policy #{0} ({1}) has an invalid or empty policy_xml field.  Using null policy.", _oid, _name));
            return FalseAssertion.getInstance();
        }
        // Warning: ESM migration depends on the cached instance of assertion being available after the policy XML has been parsed.
        if (assertion == null) {
            WspReader.Visibility v = visibility != null ? visibility : defaultVisibility;
            assertion = WspReader.getDefault().parsePermissively(xml, v);
            if (assertion != null) {
                assertion.ownerPolicyOid(getOid());
                if (isLocked())
                    assertion.lock();
            }
        }

        return assertion;
    }

    /**
     * Set the policy XML based on the policies root Assertion.
     *
     * @throws IOException If an error occurs
     */
    @Override
    public synchronized void flush() throws IOException {
        checkLocked();
        if ( assertion != null ) {
            setXml( WspWriter.getPolicyXml(assertion) );
        }
    }

    public synchronized void forceRecompile() {
        assertion = null;
    }

    @Size(min = 1, max = 255)
    @Override
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Size(min=1,max=5242880)
    @Migration(resolver = PropertyResolver.Type.POLICY)
    public String getXml() {
        return xml;
    }

    public synchronized void setXml(String xml) {
        checkLocked();
        assert xml == null || xml.length() < 1 || xml.trim().length() < 1 || xml.trim().charAt(0) == xml.charAt(0);
        this.xml = xml;
        this.assertion = null;
    }

    //TODO: move guid property into an interface identifies entities with GUIDS as is done for the entity managers
    @NotNull
    @Size(min=36,max=36)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @NotNull
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
     * Check whether the first call to {@link #getAssertion()} on this Policy instance will include disabled and
     * comment assertions in the returned assertion tree.
     *
     * @return assertion visibility setting to use for subsequent policy XML parsing, or null to indicate that the then-default value ({@link #defaultVisibility}) will be used.
     */
    public WspReader.Visibility getVisibility() {
        return visibility;
    }

    /**
     * Set the visibility setting for subsequent calls to {@link #getAssertion()} that trigger policy XML to be parsed.
     * This should normally be set before the first call to {@link #getAssertion()}.
     * <p/>
     * Changes to this value will not be serialized.
     * <p/>
     * Changing this setting has no effect once an assertion tree has already been produced unless a reparse
     * is forced by a change to the policy XML.
     *
     * @param visibility assertion visibility setting to use for subsequent policy XML parsing, or null to indicate that the then-default value ({@link #defaultVisibility}) should be used.
     */
    @XmlTransient
    @Transient
    public void setVisibility(@Nullable WspReader.Visibility visibility) {
        checkLocked();
        this.visibility = visibility;
    }

    /**
     * When {@link #getType()} is {@link PolicyType#INTERNAL}, this field can be used
     * to distinguish between different types of internal policy.  Try to avoid stuffing too much data into the tag,
     * as it's limited in the database to 64 characters.
     *
     * Suggested naming convention: moduleName-type[-subtype], e.g. esm-notification
     */
    @Size(min=1,max=64)
    @Pattern(regexp="message-received|pre-security|pre-service|post-security|post-service|message-completed", groups={GlobalPolicyValidationGroup.class})
    public String getInternalTag() {
        return internalTag;
    }

    public void setInternalTag(String internalTag) {
        checkLocked();
        this.internalTag = internalTag;
    }

    @Override
    @ManyToOne
    @JoinColumn(name="folder_oid")
    @Migration(mapName = NONE, mapValue = NONE)
    public Folder getFolder() {
        return folder;
    }

    @Override
    public void setFolder(Folder folder) {
        this.folder = folder;
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
        if (securityZone != null ? !securityZone.equals(policy.securityZone) : policy.securityZone != null) return false;

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
    protected void lock() {
        if (assertion != null)
            assertion.lock();
        super.lock();
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
     * Get the default visibility of disabled and comment assertions in the current environment.
     *
     * @return assertion visibility setting to use when parsing policy XML, if not specified for a particular Policy instance.
     */
    public static WspReader.Visibility getDefaultVisibility() {
        return defaultVisibility;
    }

    /**
     * Set the default visibility of disabled assertions and comment assertions in the current environment.
     * <p/>
     * The default is to include such assertions in the tree returned by {@link #getAssertion()}.
     *
     * @param visibility assertion visibility setting to use when parsing policy XML, if not specified for a particular Policy instance.  Required.
     */
    public static void setDefaultVisibility(@NotNull WspReader.Visibility visibility) {
        defaultVisibility = visibility;
    }

    /**
     * Recursively simplify the given assertion.
     *
     * <p>This will simplify the given assertion in a manner that is compatible
     * with the constraints of a policy (such as root must be an All).</p>
     *
     * <p>This method is only for full policies, so the given assertion must not
     * have a parent.</p>
     *
     * @param assertion The assertion to simplify (must not be null)
     * @param includeComments boolean, if true then comments will be left on Assertions
     * @return The simplified assertion (which is always the given assertion)
     * @throws IllegalArgumentException if the given assertion has a parent
     * @see Assertion#simplify(com.l7tech.policy.assertion.Assertion,boolean,boolean) Assertion.simplify
     */
    public static Assertion simplify(final Assertion assertion, boolean includeComments) {
        if (assertion == null) throw new IllegalArgumentException("assertion must not be null");
        if (!(assertion instanceof AllAssertion)) throw new IllegalArgumentException("assertion must be AllAssertion");
        if (assertion.getParent() != null) throw new IllegalArgumentException("assertion has a parent");

        final Assertion simplified = Assertion.simplify( assertion, true, includeComments );
        if ( simplified != assertion ) {
            // then we may need to re-add to the root
            if ( simplified == null ) {
                ((AllAssertion) assertion).clearChildren();
            } else if ( simplified.getParent() != assertion ) {
                AllAssertion policyRoot = (AllAssertion) assertion;
                policyRoot.clearChildren();
                policyRoot.addChild( simplified );
            }
        }

        return assertion;
    }

    /**
     * Validation group with additional constraints for Global policies.
     */
    public interface GlobalPolicyValidationGroup {}

    /**
     * Make a best-effort to provide entities to assertion beans using the specified entity provider.
     * <p/>
     * This method will ignore policy parsing errors -- they will presumably be handled properly next time someone attempts
     * to use the policy assertions.
     * <p/>
     * This method will log errors that occur while providing entities declared as needed at design time by the assertion beans
     * but will otherwise ignore them and continue.
     *
     * @param entityProvider entity provider to use.  Required.
     */
    private void provideEntitiesToAssertionBeans(@NotNull DesignTimeEntityProvider entityProvider) {
        try {
            final Assertion rootAssertion = getAssertion();
            if (rootAssertion != null) {
                Iterator<Assertion> it = rootAssertion.preorderIterator();
                while (it.hasNext()) {
                    Assertion ass = it.next();
                    if (ass instanceof UsesEntitiesAtDesignTime) {
                        UsesEntitiesAtDesignTime entityUser = (UsesEntitiesAtDesignTime) ass;
                        try {
                            entityProvider.provideNeededEntities(entityUser, null);
                        } catch (FindException e) {
                            logger.log(Level.WARNING, "Unable to prepare entity for assertion ordinal " + ass.getOrdinal() + " of policy GUID " + getGuid() + " (" + getName() + "): " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore and handle it later
        }
    }
}
