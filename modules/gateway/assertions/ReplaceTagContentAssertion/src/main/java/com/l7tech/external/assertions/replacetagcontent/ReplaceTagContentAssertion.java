package com.l7tech.external.assertions.replacetagcontent;

import com.l7tech.policy.assertion.*;
import org.jetbrains.annotations.NotNull;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;

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
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/SAMLAttributeStatement.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(POLICY_NODE_NAME_FACTORY, NODE_NAME_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private final AssertionNodeNameFactory<ReplaceTagContentAssertion> NODE_NAME_FACTORY = new AssertionNodeNameFactory<ReplaceTagContentAssertion>() {
        @Override
        public String getAssertionName(final ReplaceTagContentAssertion assertion, final boolean decorate) {
            final StringBuilder sb = new StringBuilder();
            if (decorate && assertion.getSearchFor() != null && assertion.getTagsToSearch() != null && assertion.getReplaceWith() != null) {
                sb.append("Replace ");
                sb.append(assertion.getSearchFor());
                sb.append(" in ");
                sb.append(assertion.getTagsToSearch());
                sb.append(" with ");
                sb.append(assertion.getReplaceWith());
            } else {
                sb.append(BASE_NAME);
            }
            return decorate ? AssertionUtils.decorateName(assertion, sb) : sb.toString();
        }
    };
}
