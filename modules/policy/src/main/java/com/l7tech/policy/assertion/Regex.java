/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * @author emil
 */
public class Regex extends MessageTargetableAssertion implements UsesVariables, SetsVariables {
    private String regex;
    private String replacement;
    private boolean caseInsensitive;
    private boolean replace;
    private boolean findAll = false;
    private int replaceRepeatCount = 0;
    private boolean includeEntireExpressionCapture = true;
    private boolean patternContainsVariables = false;
    /**
     * Which MIME part to do the replacement in. Use null to indicate the first (SOAP)
     * part or any other Integer (zero-based) for a specific MIME part.
     */
    private int mimePart = 0;
    private boolean proceedIfPatternMatches = true;
    private String encoding;
    private @Nullable String regexName;
    private String captureVar = null;
    private boolean autoTarget = true;

    /**
     * Check whether the assertion should use pre-4.7 style automatic targeting of request/response messages.
     * <p/>
     * New regex assertions created in the UI post-4.6 will always use message targeting, but assertions
     * created before 4.7 should preserve the old behavior when thawed from an older policy XML.
     *
     * @return true if this assertion should automatically target request or response, ignoring
     *              the message target; false if the message target should be used instead.
     */
    public boolean isAutoTarget() {
        return autoTarget;
    }

    /**
     * Set whether the assertion should use MessageTargetable, or should ignore the message targeting
     * and behave as Regex did pre-4.7.
     *
     * @param autoTarget true if this assertion should automatically target request or response, ignoring
     *              the message target; false if the message target should be used instead.
     */
    public void setAutoTarget(boolean autoTarget) {
        this.autoTarget = autoTarget;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Test whether the replace has been requested
     * @return boolean true if replace requested, false otherwise
     */
    public boolean isReplace() {
        return replace;
    }

    /**
     * Set the boolean tooggle that enables replace
     * @param replace true to do search and replace.  False to just do search.
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
        setTargetModifiedByGateway(replace);
    }

    public int getReplaceRepeatCount() {
        return replaceRepeatCount;
    }

    /**
     * Set to a value higher than zero to repeatedly re-apply a search and replace regex until it either fails
     * to match or until we have applied it the specified number of times.  Any values captured by capture
     * groups will be appended to the output variable each time the regex is repeated.
     *
     * @param replaceRepeatCount number of times to repeat a search and replace regex.  Ignored unless replace is true.
     */
    public void setReplaceRepeatCount(int replaceRepeatCount) {
        this.replaceRepeatCount = replaceRepeatCount;
    }

    public boolean isIncludeEntireExpressionCapture() {
        return includeEntireExpressionCapture;
    }

    /**
     * Set whether the zeroth capture group result, matching the entire section of the target variable matched by
     * the regex, is included in the output.  If true (default), then a zeroth value of the output will always
     * be present that acts as though the entire expression were surrounded by parens.  If false, only
     * capture groups explicitly written into the regex will produce output values.
     *
     * @param includeEntireExpressionCapture true to include entire matched substring as zeroth output value.  False to include only explicitly captured substrings as output values.
     */
    public void setIncludeEntireExpressionCapture(boolean includeEntireExpressionCapture) {
        this.includeEntireExpressionCapture = includeEntireExpressionCapture;
    }

    public boolean isFindAll() {
        return findAll;
    }

    /**
     * Set whether to keep searching to the end of the target string, even after a successful match, to collect additional
     * capture group values.
     *
     * @param findAll if true, the assertion will scan the entire input for matches, even after a successful match.
     *                if false, the assertion will stop after the first successful match.
     */
    public void setFindAll(boolean findAll) {
        this.findAll = findAll;
    }

    public boolean isPatternContainsVariables() {
        return patternContainsVariables;
    }

    /**
     * If true, ExpandVariables will be called on the pattern before it is compiled for each request.
     * If false, the pattern will be compiled only once, when the server assertion is constructed,
     * and will not support variable expansion.
     *
     * @param patternContainsVariables true to expand context variables in the regex pattern
     */
    public void setPatternContainsVariables(boolean patternContainsVariables) {
        this.patternContainsVariables = patternContainsVariables;
    }

    /**
     * Defaults to true; sets whether the server assertion should
     * pass if pattern matches or not. This does not have any effect
     * if replace is set.
     *
     * @return true if server assertion should pass, false if not
     */
    public boolean isProceedIfPatternMatches() {
        return proceedIfPatternMatches;
    }

    /**
     * Set whether the proceed if pattern matches.
     *
     * @param proceedIfPatternMatches true if assertion should succeed if match succeeds; false if assertion should fail if match succeeds
     */
    public void setProceedIfPatternMatches(boolean proceedIfPatternMatches) {
        this.proceedIfPatternMatches = proceedIfPatternMatches;
    }


    /**
     * @return the mime part of the this regex will match against
     */
    public int getMimePart() {
        return mimePart;
    }

    /**
     * Set the mime part index of the message that this regex will match against
     * @param mimePart message part index (0=main part, 1=first attachment, 2=second attachment, etc)
     */
    public void setMimePart(int mimePart) {
        this.mimePart = mimePart;
    }

    /**
     * @return the name of the character encoding to use when translating the selected {@link #mimePart}'s
     * bytes into a String, and vice versa.  If null, the charset parameter of the MIME part's declared
     * Content-Type header will be used.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the name of the character encoding to use when translating the selected {@link #mimePart}'s
     * bytes into a String, and vice versa.  Set to null to use the charset parameter from the MIME part's
     * Content-Type header.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Get the name for this regular expression
     *
     * @return the name or null if none is set
     */
    @Nullable
    public String getRegexName() {
        return regexName;
    }

    /**
     * Set the name for this regular expression
     *
     * @param regexName the name or null for none
     */
    public void setRegexName(@Nullable String regexName) {
        this.regexName = regexName;
    }

    /**
     * @return variable to which to save captured groups, or null to skip doing so.
     */
    public String getCaptureVar() {
        return captureVar;
    }

    /**
     * Set the variable prefix to be used for capture groups caught by the regex.
     *
     * @param captureVar the prefix for captures, or null or empty string to disable capturing the result of capture groups.
     */
    public void setCaptureVar(String captureVar) {
        if (captureVar != null && captureVar.trim().length() < 1)
            captureVar = null;
        this.captureVar = captureVar;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        Set<String> varsUsed = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        varsUsed.addAll(Arrays.asList(super.getVariablesUsed()));
        if (replacement != null)
            varsUsed.addAll(Arrays.asList(Syntax.getReferencedNames(replacement)));
        if (patternContainsVariables && regex != null)
            varsUsed.addAll(Arrays.asList(Syntax.getReferencedNames(regex)));
        return varsUsed.toArray(new String[varsUsed.size()]);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] set = captureVar == null ? null : new VariableMetadata[] {
                new VariableMetadata(captureVar, false, true, captureVar, true, DataType.STRING),
        };
        return mergeVariablesSet(set);
    }

    private final static String baseName = "Evaluate Regular Expression";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<Regex>(){
        @Override
        public String getAssertionName( final Regex assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder nameBuffer = new StringBuilder(256);
            nameBuffer.append(baseName);

            if (assertion.getRegexName() != null) {
                nameBuffer.append(" - ");
                nameBuffer.append(assertion.getRegexName());
            } else{
                //if display name is not set, default to the regular expression
                nameBuffer.append(" - ");
                nameBuffer.append(assertion.getRegex());
            }

            return AssertionUtils.decorateName( assertion, nameBuffer);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Evaluate a regular expression against the target message.");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/regex16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.RegexPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Regular Expression Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");
        return meta;
    }
}
