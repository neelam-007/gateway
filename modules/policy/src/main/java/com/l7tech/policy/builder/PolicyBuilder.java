package com.l7tech.policy.builder;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builder which allows you to build up xml Documents that represent policy.
 */
public class PolicyBuilder {
    // templates
    private static final String BASE_PATH = "com/l7tech/policy/builder/";
    private static final String ENCODE = BASE_PATH + "encodeTemplate.xml";
    private static final String UPDATE_COOKIE = BASE_PATH + "updateCookieTemplate.xml";
    private static final String REPLACE_TAG_CONTENT = BASE_PATH + "replaceTagContentTemplate.xml";
    private static final String COMPARE = BASE_PATH + "compareTemplate.xml";
    private static final String ASSERTION_COMMENT = BASE_PATH + "assertionCommentTemplate.xml";

    // namespaces
    private static final String XML_SOAP_NS = "http://schemas.xmlsoap.org/ws/2002/12/policy";
    private static final String L7_NS = "http://www.layer7tech.com/ws/policy";
    private static final String L7P_PREFIX = "L7p:";

    // generic assertion constants
    private static final String ALL = "All";
    private static final String STRING_VALUE = "stringValue";
    private static final String BOOLEAN_VALUE = "booleanValue";
    private static final String ENTRY = "entry";
    private static final String KEY = "Key";
    private static final String TARGET = "Target";
    private static final String OTHER_TARGET_NAME = "OtherTargetMessageVariable";
    private static final String VALUE = "Value";
    private static final String BINARY = "binary";
    private static final String INCLUDED = "included";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String ITEM = "item";
    private static final String ENABLED = "Enabled";
    private static final String PROPERTIES = "Properties";

    // cookie
    private static final String COOKIE_ATTRIBUTES = "CookieAttributes";
    private static final String COOKIE_CRITERIA = "CookieCriteria";
    private static final String USE_ORIGINAL_VALUE = "UseOriginalValue";
    private static final String DOMAIN = "domain";
    private static final String NAME = "name";
    private static final String TEMP_COOKIE_NAME_VAR = "l7_tmp_cname";
    private static final String COOKIENAME = "cookiename";

    // html
    private static final String SEARCH_FOR = "SearchFor";
    private static final String REPLACE_WITH = "ReplaceWith";
    private static final String TAGS_TO_SEARCH = "TagsToSearch";
    private static final String HTML = "html";

    // encode
    private static final String SOURCE_VARIABLE_NAME = "SourceVariableName";
    private static final String TARGET_VARIABLE_NAME = "TargetVariableName";

    // compare
    private static final String EXPRESSION1 = "Expression1";
    private static final String PREDICATES = "Predicates";
    private static final String OPERATOR = "Operator";
    private static final String RIGHT_VALUE = "RightValue";
    private static final String CONTAINS = "CONTAINS";
    private static final String EMPTY = "EMPTY";
    private static final String NEGATED = "Negated";
    private static final String CASE_SENSITIVE = "CaseSensitive";

    // header
    private static final String TEMP_HEADER_VAR = "l7_tmp_header";

    private Document policyDoc = null;

    /**
     * Sets up a basic policy document which only contains an AllAssertion.
     */
    public PolicyBuilder() {
        policyDoc = WspWriter.getPolicyDocument(new AllAssertion());
    }

    /**
     * @return the currently built policy Document.
     */
    public Document getPolicy() {
        return policyDoc;
    }

    /**
     * Append the given assertion to the Document.
     *
     * @param assertion the assertion to append.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder appendAssertion(@NotNull final Assertion assertion) {
        return appendAssertion(assertion, null);
    }

    /**
     * Append the given assertion to the Document.
     *
     * @param assertion the assertion to append.
     * @param comment   an optional comment to set on the assertion.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder appendAssertion(@NotNull final Assertion assertion, @Nullable final String comment) {
        addRightComment(comment, assertion);
        final Document assertionDoc = WspWriter.getPolicyDocument(assertion);
        addNode(assertionDoc.getDocumentElement().getFirstChild());
        return this;
    }

    /**
     * Append a CommentAssertion to the Document.
     *
     * @param comment the comment to add.
     * @param enable  false if the comment should be disabled.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder comment(@NotNull final String comment, final boolean enable) {
        final CommentAssertion com = new CommentAssertion(comment);
        com.setEnabled(enable);
        appendAssertion(com);
        return this;
    }

    /**
     * Append a set variable assertion (string) to the document.
     *
     * @param name  the context variable to set.
     * @param value the value to set.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder setContextVariable(@NotNull final String name, @NotNull final String value) throws IOException {
        return setContextVariable(name, value, DataType.STRING, null);
    }

    /**
     * Append a set variable assertion to the document.
     *
     * @param name        the context variable to set.
     * @param value       the value to set.
     * @param dataType    the DataType of the variable to set.
     * @param contentType if the dataType is {@link DataType#MESSAGE}, the contentType of the message to set.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder setContextVariable(@NotNull final String name, @NotNull final String value, @NotNull final DataType dataType, @Nullable final String contentType) throws IOException {
        if (dataType == DataType.MESSAGE && contentType == null) {
            throw new IllegalArgumentException("ContentType must be specified for dataType=" + DataType.MESSAGE);
        }
        final SetVariableAssertion setVar = new SetVariableAssertion();
        setVar.setVariableToSet(name);
        setVar.setExpression(value);
        setVar.setDataType(dataType);
        if (dataType == DataType.MESSAGE) {
            setVar.setContentType(contentType);
        }
        appendAssertion(setVar);
        return this;
    }

    /**
     * Append multiple set variable assertions (string) to the document in an 'all' folder.
     *
     * @param vars    the name=value variables to set.
     * @param comment an optional comment to set on the 'all' folder.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder setContextVariables(@NotNull final Map<String, String> vars, @Nullable final String comment) throws IOException {
        final AllAssertion all = new AllAssertion();
        addRightComment(comment, all);
        for (final Map.Entry<String, String> entry : vars.entrySet()) {
            all.addChild(new SetVariableAssertion(entry.getKey(), entry.getValue()));
        }
        appendAssertion(all);
        return this;
    }

    /**
     * Appends a regex assertion to the document.
     *
     * @param targetMessage          the {@link TargetMessageType} to target.
     * @param otherTargetMessageName if targetMessage is {@link TargetMessageType#OTHER}, the name of the other message variable.
     * @param regexPattern           the regex pattern to match.
     * @param replacement            the optional replacement if the regex matches.
     * @param enable                 false if the appended policy should be disabled.
     * @param comment                an optional comment to add to the regex.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder regex(@NotNull final TargetMessageType targetMessage, @Nullable final String otherTargetMessageName,
                               @NotNull final String regexPattern, @Nullable final String replacement,
                               final boolean enable, @Nullable final String comment) {
        validateTarget(targetMessage, otherTargetMessageName);
        final Regex regex = new Regex();
        regex.setTarget(targetMessage);
        regex.setAutoTarget(otherTargetMessageName == null);
        regex.setOtherTargetMessageVariable(otherTargetMessageName);
        regex.setRegex(regexPattern);
        regex.setPatternContainsVariables(Syntax.isAnyVariableReferenced(regexPattern));
        regex.setReplace(replacement != null);
        regex.setReplacement(replacement);
        regex.setEnabled(enable);
        addRightComment(comment, regex);
        appendAssertion(regex);
        return this;
    }

    /**
     * Appends an encode/decode assertion configured to url encode to the Document.
     *
     * @param srcVarName    the name of the context variable to encode.
     * @param targetVarName the name of the target context variable in which to store the encoded value.
     * @param comment       an optional comment to add to the assertion.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder urlEncode(@NotNull final String srcVarName, @NotNull final String targetVarName, @Nullable final String comment) throws IOException {
        final Document encodeDoc = readResource(ENCODE);
        final Element srcVarNameElement = XmlUtil.findFirstChildElementByName(encodeDoc.getDocumentElement(), L7_NS, SOURCE_VARIABLE_NAME);
        srcVarNameElement.setAttribute(STRING_VALUE, srcVarName);
        final Element targetVarNameElement = XmlUtil.findFirstChildElementByName(encodeDoc.getDocumentElement(), L7_NS, TARGET_VARIABLE_NAME);
        targetVarNameElement.setAttribute(STRING_VALUE, targetVarName);
        createAndAppendCommentElement(encodeDoc, comment);
        addNode(encodeDoc.getDocumentElement());
        return this;
    }

    /**
     * Appends a ManageCookie assertion which replaces whole cookie domain attribute values to the Document.
     *
     * @param targetMessage          the {@link TargetMessageType} to target.
     * @param otherTargetMessageName if targetMessage is {@link TargetMessageType#OTHER}, the name of the other message variable.
     * @param domainToMatch          the cookie domain to replace.
     * @param domainToSet            the cookie domain replacement to set if a match is found.
     * @param enable                 false if the appended policy should be disabled.
     * @param comment                an optional comment to add to the assertion.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder replaceHttpCookieDomains(@NotNull final TargetMessageType targetMessage, @Nullable final String otherTargetMessageName,
                                                  @NotNull final String domainToMatch, @NotNull final String domainToSet,
                                                  final boolean enable, @Nullable final String comment) throws IOException {
        validateTarget(targetMessage, otherTargetMessageName);
        final Document cookieDoc = readResource(UPDATE_COOKIE);
        createAndAppendCommentElement(cookieDoc, comment);
        addCookieAttributeValue(DOMAIN, domainToSet, cookieDoc);
        addSingleCookieCriteriaValue(DOMAIN, domainToMatch, cookieDoc);
        final Element target = XmlUtil.findFirstChildElementByName(cookieDoc.getDocumentElement(), L7_NS, TARGET);
        target.setAttribute(TARGET.toLowerCase(), targetMessage.name());
        if (otherTargetMessageName != null) {
            createAndAppendOtherTargetNameElement(otherTargetMessageName, cookieDoc);
        }
        if (!enable) {
            createAndAppendDisabledElement(cookieDoc);
        }
        addNode(cookieDoc.getDocumentElement());
        return this;
    }

    /**
     * Appends policy which replaces whole or partial cookie names to the Document.
     *
     * @param targetMessage          the {@link TargetMessageType} to target.
     * @param otherTargetMessageName if targetMessage is {@link TargetMessageType#OTHER}, the name of the other message variable.
     * @param toSearch               the text to replace in the cookie name.
     * @param toReplace              the replacement text if a match is found.
     * @param enable                 false if the appended policy should be disabled.
     * @param comment                an optional comment to add to the policy.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder replaceHttpCookieNames(@NotNull final TargetMessageType targetMessage, @Nullable final String otherTargetMessageName,
                                                @NotNull final String toSearch, @NotNull final String toReplace,
                                                final boolean enable, @Nullable final String comment) throws IOException {
        validateTarget(targetMessage, otherTargetMessageName);
        final OneOrMoreAssertion oneOrMore = new OneOrMoreAssertion();
        oneOrMore.addChild(new AllAssertion());
        oneOrMore.addChild(new TrueAssertion());
        String loopVarName = targetMessage == TargetMessageType.OTHER ? otherTargetMessageName : targetMessage.name().toLowerCase();
        loopVarName = loopVarName + ".http.cookienames";
        final ForEachLoopAssertion forEach = new ForEachLoopAssertion();
        forEach.setLoopVariableName(loopVarName);
        forEach.setVariablePrefix(COOKIENAME);
        forEach.addChild(oneOrMore);
        forEach.setEnabled(enable);
        addRightComment(comment, forEach);
        final Document forEachDoc = WspWriter.getPolicyDocument(forEach);
        final Element allElement = XmlUtil.findFirstDescendantElement(forEachDoc.getDocumentElement(), XML_SOAP_NS, ALL);

        final SetVariableAssertion setVar = new SetVariableAssertion();
        setVar.setVariableToSet(TEMP_COOKIE_NAME_VAR);
        setVar.setExpression("${" + COOKIENAME + ".current}");
        final Document setVarDoc = WspWriter.getPolicyDocument(setVar);
        allElement.appendChild(forEachDoc.importNode(setVarDoc.getDocumentElement().getFirstChild(), true));

        final Map<String, Pair<String, String>> itemElements = new HashMap<>();
        itemElements.put(OPERATOR, new Pair(OPERATOR.toLowerCase(), CONTAINS));
        itemElements.put(RIGHT_VALUE, new Pair(STRING_VALUE, toSearch));
        final Document compareDoc = createComparisonAssertionDoc("${" + TEMP_COOKIE_NAME_VAR + "}", itemElements);
        allElement.appendChild(forEachDoc.importNode(compareDoc.getDocumentElement(), true));

        final Regex regex = new Regex();
        regex.setRegex(toSearch);
        regex.setPatternContainsVariables(Syntax.isAnyVariableReferenced(toSearch));
        regex.setReplace(true);
        regex.setReplacement(toReplace);
        regex.setAutoTarget(false);
        regex.setTarget(TargetMessageType.OTHER);
        regex.setOtherTargetMessageVariable(TEMP_COOKIE_NAME_VAR);
        final Document regexDoc = WspWriter.getPolicyDocument(regex);
        allElement.appendChild(forEachDoc.importNode(regexDoc.getDocumentElement().getFirstChild(), true));

        final Document cookieDoc = readResource(UPDATE_COOKIE);
        addCookieAttributeValue(NAME, "${" + TEMP_COOKIE_NAME_VAR + "}", cookieDoc);
        addSingleCookieCriteriaValue(NAME, "${" + COOKIENAME + ".current}", cookieDoc);
        final Element target = XmlUtil.findFirstChildElementByName(cookieDoc.getDocumentElement(), L7_NS, TARGET);
        target.setAttribute(TARGET.toLowerCase(), targetMessage.name());
        if (otherTargetMessageName != null) {
            createAndAppendOtherTargetNameElement(otherTargetMessageName, cookieDoc);
        }
        allElement.appendChild(forEachDoc.importNode(cookieDoc.getDocumentElement(), true));

        addNode(forEachDoc.getDocumentElement().getFirstChild());
        return this;
    }

    /**
     * Appends an HttpRoutingAssertion to the Document.
     *
     * @param url               the url to route to.
     * @param failOnErrorStatus true if the route should fail if an error status is received.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder routeForwardAll(@NotNull final String url, final boolean failOnErrorStatus) throws IOException {
        final HttpPassthroughRule[] emptyRules = {};
        final HttpRoutingAssertion route = new HttpRoutingAssertion();
        route.setFailOnErrorStatus(failOnErrorStatus);
        route.setProtectedServiceUrl(url);
        route.getRequestHeaderRules().setForwardAll(true);
        route.getRequestHeaderRules().setRules(emptyRules);
        route.getRequestParamRules().setForwardAll(true);
        route.getRequestParamRules().setRules(emptyRules);
        route.getResponseHeaderRules().setForwardAll(true);
        route.getResponseHeaderRules().setRules(emptyRules);
        appendAssertion(route);
        return this;
    }

    /**
     * Appends policy which rewrites whole or partial header values to the Document.
     *
     * @param targetMessage          the {@link TargetMessageType} to target.
     * @param otherTargetMessageName if targetMessage is {@link TargetMessageType#OTHER}, the name of the other message variable.
     * @param headerName             the name of the header to rewrite.
     * @param patternToMatch         the header value pattern to match.
     * @param replacement            the replacement if the pattern matches.
     * @param enable                 false if the appended policy should be disabled.
     * @param comment                an optional comment to add to the policy.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder rewriteHeader(@NotNull final TargetMessageType targetMessage, @Nullable final String otherTargetMessageName,
                                       @NotNull final String headerName, @NotNull final String patternToMatch,
                                       @NotNull final String replacement, final boolean enable, @Nullable final String comment) throws IOException {
        validateTarget(targetMessage, otherTargetMessageName);
        final String targetName = targetMessage == TargetMessageType.OTHER ? otherTargetMessageName : targetMessage.name().toLowerCase();
        final String headerContextVariable = "${" + targetName + ".http.header." + headerName + "}";

        final SetVariableAssertion setVarAssertion = new SetVariableAssertion();
        setVarAssertion.setVariableToSet(TEMP_HEADER_VAR);
        setVarAssertion.setExpression(headerContextVariable);

        final Regex regexAssertion = new Regex();
        regexAssertion.setPatternContainsVariables(true);
        regexAssertion.setRegex(patternToMatch);
        regexAssertion.setReplace(true);
        regexAssertion.setReplacement(replacement);
        regexAssertion.setOtherTargetMessageVariable(TEMP_HEADER_VAR);
        regexAssertion.setTarget(TargetMessageType.OTHER);
        regexAssertion.setAutoTarget(false);

        final AddHeaderAssertion headerAssertion = new AddHeaderAssertion();
        headerAssertion.setHeaderName(headerName);
        headerAssertion.setRemoveExisting(true);
        headerAssertion.setHeaderValue("${" + TEMP_HEADER_VAR + "}");
        headerAssertion.setTarget(targetMessage);
        if (targetMessage == TargetMessageType.OTHER) {
            headerAssertion.setOtherTargetMessageVariable(otherTargetMessageName);
        }

        final AllAssertion all = new AllAssertion();
        all.addChild(setVarAssertion);
        all.addChild(regexAssertion);
        all.addChild(headerAssertion);

        final OneOrMoreAssertion oneOrMore = new OneOrMoreAssertion();
        oneOrMore.addChild(all);
        oneOrMore.addChild(new TrueAssertion());
        oneOrMore.setEnabled(enable);
        addRightComment(comment, oneOrMore);

        final Map<String, Pair<String, String>> itemElements = new HashMap<>();
        itemElements.put(NEGATED, new Pair(BOOLEAN_VALUE, TRUE));
        itemElements.put(OPERATOR, new Pair(OPERATOR.toLowerCase(), EMPTY));
        final Document compareDoc = createComparisonAssertionDoc(headerContextVariable, itemElements);

        final Document oneOrMoreDoc = WspWriter.getPolicyDocument(oneOrMore);
        final Element allElement = XmlUtil.findFirstChildElementByName(oneOrMoreDoc.getDocumentElement().getFirstChild(), XML_SOAP_NS, ALL);
        allElement.insertBefore(oneOrMoreDoc.importNode(compareDoc.getDocumentElement(), true), allElement.getFirstChild());
        addNode(oneOrMoreDoc.getDocumentElement().getFirstChild());

        return this;
    }

    /**
     * Appends policy which rewrites response HTML to the Document.
     *
     * @param targetMessage          the {@link TargetMessageType} to target.
     * @param otherTargetMessageName if targetMessage is {@link TargetMessageType#OTHER}, the name of the other message variable.
     * @param searchFor              the text(s) to replace.
     * @param replaceWith            the text to use as a replacement if matches are found.
     * @param tagsToSearch           the HTML tags to search within.
     * @param comment                an optional comment to add to the policy.
     * @return the PolicyBuilder.
     */
    public PolicyBuilder rewriteHtml(@NotNull final TargetMessageType targetMessage, @Nullable final String otherTargetMessageName,
                                     @NotNull final Set<String> searchFor, @NotNull final String replaceWith,
                                     @NotNull final String tagsToSearch, @Nullable final String comment) throws IOException {
        validateTarget(targetMessage, otherTargetMessageName);
        final Map<String, Pair<String, String>> itemElements = new HashMap<>();
        itemElements.put(CASE_SENSITIVE, new Pair(BOOLEAN_VALUE, FALSE));
        itemElements.put(OPERATOR, new Pair(OPERATOR.toLowerCase(), CONTAINS));
        itemElements.put(RIGHT_VALUE, new Pair(STRING_VALUE, HTML));
        String targetName = targetMessage == TargetMessageType.OTHER ? otherTargetMessageName : targetMessage.name().toLowerCase();
        final Document compareDoc = createComparisonAssertionDoc("${" + targetName + ".http.header.content-type}", itemElements);

        final OneOrMoreAssertion oneOrMore = new OneOrMoreAssertion();
        oneOrMore.addChild(new AllAssertion());
        oneOrMore.addChild(new TrueAssertion());
        addRightComment(comment, oneOrMore);

        final Document oneOrMoreDoc = WspWriter.getPolicyDocument(oneOrMore);
        final Element all = XmlUtil.findFirstChildElementByName(oneOrMoreDoc.getDocumentElement().getFirstChild(), XML_SOAP_NS, ALL);
        all.appendChild(oneOrMoreDoc.importNode(compareDoc.getDocumentElement(), true));

        for (final String searchStr : searchFor) {
            final Document htmlDoc = readResource(REPLACE_TAG_CONTENT);
            final Element search = XmlUtil.findFirstChildElementByName(htmlDoc.getDocumentElement(), L7_NS, SEARCH_FOR);
            search.setAttribute(STRING_VALUE, searchStr);
            final Element replace = XmlUtil.findFirstChildElementByName(htmlDoc.getDocumentElement(), L7_NS, REPLACE_WITH);
            replace.setAttribute(STRING_VALUE, replaceWith);
            final Element tags = XmlUtil.findFirstChildElementByName(htmlDoc.getDocumentElement(), L7_NS, TAGS_TO_SEARCH);
            tags.setAttribute(STRING_VALUE, tagsToSearch);
            final Element target = XmlUtil.findFirstChildElementByName(htmlDoc.getDocumentElement(), L7_NS, TARGET);
            target.setAttribute(TARGET.toLowerCase(), targetMessage.name());
            if (targetMessage == TargetMessageType.OTHER) {
                createAndAppendOtherTargetNameElement(otherTargetMessageName, htmlDoc);
            }
            all.appendChild(oneOrMoreDoc.importNode(htmlDoc.getDocumentElement(), true));
        }
        addNode(oneOrMoreDoc.getDocumentElement().getFirstChild());
        return this;
    }

    private void createAndAppendDisabledElement(final Document doc) {
        final Element targetName = doc.createElementNS(L7_NS, L7P_PREFIX + ENABLED);
        targetName.setAttribute(BOOLEAN_VALUE, "false");
        doc.getDocumentElement().appendChild(targetName);
    }

    private void createAndAppendOtherTargetNameElement(final String otherTargetMessageName, final Document doc) {
        final Element targetName = doc.createElementNS(L7_NS, L7P_PREFIX + OTHER_TARGET_NAME);
        targetName.setAttribute(STRING_VALUE, otherTargetMessageName);
        doc.getDocumentElement().appendChild(targetName);
    }

    private void createAndAppendCommentElement(final Document doc, final String comment) throws IOException {
        if (comment != null) {
            final Document commentDoc = readResource(ASSERTION_COMMENT);
            final Element propertiesElement = XmlUtil.findFirstChildElementByName(commentDoc.getDocumentElement(), L7_NS, PROPERTIES);
            final Element entryElement = XmlUtil.findFirstChildElementByName(propertiesElement, L7_NS, ENTRY);
            final Element valueElement = XmlUtil.findFirstChildElementByName(entryElement, L7_NS, VALUE.toLowerCase());
            valueElement.setAttribute(STRING_VALUE, comment);
            doc.getDocumentElement().appendChild(doc.importNode(commentDoc.getDocumentElement(), true));
        }
    }

    private void validateTarget(TargetMessageType targetMessage, String otherTargetMessageName) {
        if (targetMessage == TargetMessageType.OTHER && otherTargetMessageName == null) {
            throw new IllegalArgumentException("OtherTargetMessageName must be specified for targetMessage=" + TargetMessageType.OTHER);
        }
    }

    private Document createComparisonAssertionDoc(final String expression, final Map<String, Pair<String, String>> itemElements) throws IOException {
        final Document compareDoc = readResource(COMPARE);
        final Element expression1 = XmlUtil.findFirstChildElementByName(compareDoc.getDocumentElement(), L7_NS, EXPRESSION1);
        expression1.setAttribute(STRING_VALUE, expression);
        final Element item = createPredicateItem(compareDoc, itemElements);
        final Element predicates = XmlUtil.findFirstChildElementByName(compareDoc.getDocumentElement(), L7_NS, PREDICATES);
        predicates.appendChild(item);
        return compareDoc;
    }

    private Element createPredicateItem(final Document doc, final Map<String, Pair<String, String>> elements) {
        final Element item = doc.createElementNS(L7_NS, L7P_PREFIX + ITEM);
        item.setAttribute(BINARY, INCLUDED);
        for (final Map.Entry<String, Pair<String, String>> entry : elements.entrySet()) {
            final Element element = doc.createElementNS(L7_NS, L7P_PREFIX + entry.getKey());
            element.setAttribute(entry.getValue().getKey(), entry.getValue().getValue());
            item.appendChild(element);
        }
        return item;
    }

    private void addNode(final Node node) {
        final Element allElement = XmlUtil.findFirstChildElementByName(policyDoc.getDocumentElement(), XML_SOAP_NS, ALL);
        allElement.appendChild(policyDoc.importNode(node, true));
    }

    private Document readResource(final String resource) throws IOException {
        final InputStream resourceAsStream = PolicyBuilder.class.getClassLoader().getResourceAsStream(resource);
        if (resourceAsStream == null) {
            throw new IOException("Resource file does not exist: " + resource);
        }
        try {
            return XmlUtil.parse(resourceAsStream);
        } catch (final SAXException e) {
            throw new IOException(e);
        }
    }

    private void addCookieAttributeValue(final String attributeName, final String attributeValue, final Document cookieDoc) {
        final Element mapElement = XmlUtil.findFirstChildElementByName(cookieDoc.getDocumentElement(), L7_NS, COOKIE_ATTRIBUTES);
        appendCookieMapEntry(attributeName, "nameValuePair", attributeValue, cookieDoc, mapElement);
    }

    private void addSingleCookieCriteriaValue(final String attributeName, final String attributeValue, final Document cookieDoc) {
        final Element mapElement = XmlUtil.findFirstChildElementByName(cookieDoc.getDocumentElement(), L7_NS, COOKIE_CRITERIA);
        appendCookieMapEntry(attributeName, "cookieCriteria", attributeValue, cookieDoc, mapElement);
    }

    private void appendCookieMapEntry(final String key, final String valueType, final String value, final Document doc, Element mapElement) {
        final Element entryElement = doc.createElementNS(L7_NS, L7P_PREFIX + ENTRY);
        final Element mapKeyElement = doc.createElementNS(L7_NS, L7P_PREFIX + KEY.toLowerCase());
        mapKeyElement.setAttribute(STRING_VALUE, key);
        final Element mapValueElement = doc.createElementNS(L7_NS, L7P_PREFIX + VALUE.toLowerCase());
        mapValueElement.setAttribute(valueType, INCLUDED);
        final Element pairKeyElement = doc.createElementNS(L7_NS, L7P_PREFIX + KEY);
        pairKeyElement.setAttribute(STRING_VALUE, key);
        final Element pairValueElement = doc.createElementNS(L7_NS, L7P_PREFIX + VALUE);
        pairValueElement.setAttribute(STRING_VALUE, value);

        mapValueElement.appendChild(pairKeyElement);
        mapValueElement.appendChild(pairValueElement);
        entryElement.appendChild(mapKeyElement);
        entryElement.appendChild(mapValueElement);
        mapElement.appendChild(entryElement);
    }

    private void addRightComment(@Nullable final String comment, @NotNull final Assertion assertion) {
        if (comment != null) {
            final Assertion.Comment com = new Assertion.Comment();
            com.setComment(comment, Assertion.Comment.RIGHT_COMMENT);
            assertion.setAssertionComment(com);
        }
    }
}