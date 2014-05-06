package com.l7tech.external.assertions.replacetagcontent.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.replacetagcontent.ReplaceTagContentAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side assertion implementation of ReplaceTagContentAssertion which searches and replaces text within specified tags.
 */
public class ServerReplaceTagContentAssertion extends AbstractMessageTargetableServerAssertion<ReplaceTagContentAssertion> {
    private static final String FIRST_PART = "first part";
    private static final String IGNORE_CASE_REGEX = "(?i)";
    private final String[] variablesUsed;
    private static final String SEPARATOR = ",";

    public ServerReplaceTagContentAssertion(final @NotNull ReplaceTagContentAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        if (assertion.getSearchFor() == null) {
            throw new PolicyAssertionException(assertion, "SearchFor text cannot be null");
        }
        if (assertion.getReplaceWith() == null) {
            throw new PolicyAssertionException(assertion, "Replacement text cannot be null");
        }
        if (assertion.getTagsToSearch() == null) {
            throw new PolicyAssertionException(assertion, "Tags to search cannot be null");
        }
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message, final String messageDescription, final AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        final String searchFor = ExpandVariables.process(assertion.getSearchFor(), variableMap, getAudit());
        // search for text should not be completely empty - but other whitespace is allowed
        if (searchFor.isEmpty()) {
            logAndAudit(AssertionMessages.EMPTY_SEARCH_TEXT);
            return AssertionStatus.FALSIFIED;
        }
        final String commaSeparatedTags = ExpandVariables.process(assertion.getTagsToSearch(), variableMap, getAudit());
        if (StringUtils.isBlank(commaSeparatedTags)) {
            logAndAudit(AssertionMessages.EMPTY_TAGS_TEXT);
            return AssertionStatus.FALSIFIED;
        }
        // empty string is valid for the replacement
        final String replaceWith = ExpandVariables.process(assertion.getReplaceWith(), variableMap, getAudit());
        try {
            replaceByTag(message, searchFor, replaceWith, getUniqueTagsToSearch(commaSeparatedTags));
        } catch (final NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, assertion.getTargetName(), FIRST_PART);
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private void replaceByTag(final Message message, final String searchFor, final String replaceWith, final Set<String> tagsToSearch) throws IOException, NoSuchPartException {
        final PartInfo firstPart = message.getMimeKnob().getFirstPart();
        final String charset = firstPart.getContentType().getEncoding().toString();
        final Reader reader = new BufferedReader(new InputStreamReader(firstPart.getInputStream(false), charset));
        final Source source = new Source(reader);
        final OutputDocument output = new OutputDocument(source);
        boolean atLeastOneReplace = false;
        for (final String tagToSearch : tagsToSearch) {
            final List<Element> elements = source.getAllElements(tagToSearch);
            if (elements != null && !elements.isEmpty()) {
                for (final Element element : elements) {
                    final String elementContent = element.toString();
                    if ((assertion.isCaseSensitive() && elementContent.contains(searchFor)) ||
                            (!assertion.isCaseSensitive() && StringUtils.containsIgnoreCase(elementContent, searchFor))) {
                        final String replacement = assertion.isCaseSensitive() ?
                                elementContent.replace(searchFor, replaceWith) :
                                elementContent.replaceAll(IGNORE_CASE_REGEX + Pattern.quote(searchFor), Matcher.quoteReplacement(replaceWith));
                        output.replace(element, replacement);
                        atLeastOneReplace = true;
                    }
                }
            } else {
                logAndAudit(AssertionMessages.TAG_NOT_FOUND, tagToSearch);
            }
        }
        if (atLeastOneReplace) {
            logger.log(Level.FINEST, "Re-initializing message " + message);
            message.initialize(firstPart.getContentType(), output.toString().getBytes());
        } else {
            logAndAudit(AssertionMessages.NO_REPLACEMENTS);
        }
    }

    private Set<String> getUniqueTagsToSearch(final String commaSeparatedTags) {
        final Set<String> uniqueTags = new HashSet<>();
        final String[] tags = StringUtils.split(commaSeparatedTags, SEPARATOR);
        for (final String tag : tags) {
            if (StringUtils.isNotBlank(tag)) {
                uniqueTags.add(tag.trim().toLowerCase());
            } else {
                logAndAudit(AssertionMessages.EMPTY_TAG_TEXT);
            }
        }
        return uniqueTags;
    }
}
