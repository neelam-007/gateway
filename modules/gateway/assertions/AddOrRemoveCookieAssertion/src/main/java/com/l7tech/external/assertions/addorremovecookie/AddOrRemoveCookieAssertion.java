package com.l7tech.external.assertions.addorremovecookie;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;

import java.util.logging.Logger;

public class AddOrRemoveCookieAssertion extends Assertion implements UsesVariables {
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(final String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(final String maxAge) {
        this.maxAge = maxAge;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(final boolean secure) {
        this.secure = secure;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(name, value, domain, cookiePath, version, maxAge, comment);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, "Add Or Remove Cookie");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"routing"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    protected static final Logger logger = Logger.getLogger(AddOrRemoveCookieAssertion.class.getName());

    private static final String META_INITIALIZED = AddOrRemoveCookieAssertion.class.getName() + ".metadataInitialized";

    private String name;

    private String value;

    private String domain;

    private String cookiePath;

    private String version;

    private String maxAge;

    private String comment;

    private boolean secure;
}
