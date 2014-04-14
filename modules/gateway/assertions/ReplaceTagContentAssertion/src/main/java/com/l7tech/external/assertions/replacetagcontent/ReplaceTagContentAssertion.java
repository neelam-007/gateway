package com.l7tech.external.assertions.replacetagcontent;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import org.jetbrains.annotations.NotNull;

/**
 * Assertion which can search and replace inside specified tags.
 */
public class ReplaceTagContentAssertion extends MessageTargetableAssertion implements UsesVariables {
    private String searchFor;
    private String replaceWith;
    private String tagsToSearch;
    private boolean caseSensitive = true;

    /**
     * @return the text to search for.
     */
    public String getSearchFor() {
        return searchFor;
    }

    /**
     * @param searchFor the text to search for.
     */
    public void setSearchFor(@NotNull final String searchFor) {
        this.searchFor = searchFor;
    }

    /**
     * @return the replacement text.
     */
    public String getReplaceWith() {
        return replaceWith;
    }

    /**
     * @param replaceWith the replacement text.
     */
    public void setReplaceWith(@NotNull String replaceWith) {
        this.replaceWith = replaceWith;
    }

    /**
     * @return a comma-separated list of tags to search within.
     */
    public String getTagsToSearch() {
        return tagsToSearch;
    }

    /**
     * @param tagsToSearch a comma-separated list of tags to search within.
     */
    public void setTagsToSearch(@NotNull String tagsToSearch) {
        this.tagsToSearch = tagsToSearch;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(searchFor, replaceWith, tagsToSearch);
    }

    private static final String META_INITIALIZED = ReplaceTagContentAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Replace Tag Content";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Search and replace text within specified tags.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/SearchIdentityProvider16x16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
