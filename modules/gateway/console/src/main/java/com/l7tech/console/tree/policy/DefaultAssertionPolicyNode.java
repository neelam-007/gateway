/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.*;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.NamespaceMigratable;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default PolicyNode for assertions that don't provide a custom one of their own.
 */
public class DefaultAssertionPolicyNode<AT extends Assertion> extends LeafAssertionTreeNode<AT> {
    private static final Logger logger = Logger.getLogger(DefaultAssertionPolicyNode.class.getName());
    private Action propertiesAction;

    public DefaultAssertionPolicyNode(AT assertion) {
        super(assertion);
        reloadPropertiesAction();
    }

    @Override
    public String getName(final boolean decorate) {
        return getNameFromMeta(asAssertion(), decorate, true);
    }

    public static <AT extends Assertion> String getNameFromMeta(final AT assertion, final boolean decorate, final boolean withComments) {
        //noinspection unchecked
        AssertionMetadata meta = assertion.meta();
        Object factory = meta.get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        String name = null;
        if (factory instanceof Functions.Unary) {
            //noinspection unchecked
            Functions.Unary<String, Assertion> unary = (Functions.Unary<String, Assertion>)factory;
            name = unary.call(assertion);
        } else if(factory instanceof AssertionNodeNameFactory){
            AssertionNodeNameFactory nameFactory = (AssertionNodeNameFactory) factory;
            name = nameFactory.getAssertionName(assertion, decorate);
        } else if (factory != null && factory instanceof String) {
            name = addFeatureNames(assertion, factory.toString());
        } else {
            Object obj = meta.get(AssertionMetadata.POLICY_NODE_NAME);
            if (obj != null)
                name = addFeatureNames(assertion, obj.toString());
        }

        final String displayText = name != null ? name : assertion.getClass().getName();
        return (decorate && withComments)? addCommentToDisplayText(assertion, displayText): displayText;
    }

    /**
     * Add a comment to the display text for an assertion. If the assertion has no text then nothing is added
     * If the local state of the policy editor panel is configured not to show comments, then displayText will not
     * be modified
     * @param assertion Assertion to modify the display text for
     * @param displayText String the text which represents the assertion parameter
     * @return the updated displayText, which may have had a comment added to the start or end
     */
    public static String addCommentToDisplayText(Assertion assertion, String displayText){
        final PolicyTree policyTree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
        final PolicyEditorPanel pep = policyTree.getPolicyEditorPanel();
        if (pep == null) {
            return displayText;
        }

        final boolean shown = Boolean.parseBoolean(PolicyEditorPanel.getTabSettingFromPolicyTabProperty(
            PolicyEditorPanel.POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS, PolicyEditorPanel.SHOW_COMMENTS,
            "false", pep.getPolicyGoid(), pep.getVersionNumber()));
        if(!shown) return displayText;

        return processComments(assertion, displayText);
    }

    /**
     * Add a comment to the display text for an assertion. If the assertion has no text then nothing is added
     * If the property of the policy version is configured not to show comments, then displayText will not
     * be modified
     * @param assertion Assertion to modify the display text for
     * @param displayText String the text which represents the assertion parameter
     * @param policyVersion The policy version is used to retrieve the property of showing comments.
     * @return the updated displayText, which may have had a comment added to the start or end
     */
    public static String addCommentToDisplayTextByPolicyVersion(Assertion assertion, String displayText, PolicyVersion policyVersion){
        final boolean shown = Boolean.parseBoolean(PolicyEditorPanel.getTabSettingFromPolicyTabProperty(
            PolicyEditorPanel.POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS, PolicyEditorPanel.SHOW_COMMENTS,
            "false", policyVersion.getPolicyGoid(), policyVersion.getOrdinal()));
        if(!shown) return displayText;

        return processComments(assertion, displayText);
    }

    /**
     * Add comments into display text if assertion comments are available.
     * @param assertion: an assertion to be displayed with comments
     * @param displayText: a text to be add comments
     * @return a assertion name with assertion comments if the assertion set comments.
     */
    private static String processComments(Assertion assertion, String displayText) {
        Assertion.Comment comment = assertion.getAssertionComment();

        if(comment == null) return displayText;

        int maxRhsComment = getMaxRhsCommentLength();
        int maxLhsComment = getMaxLhsCommentLength();

        final String leftComment = comment.getAssertionComment(Assertion.Comment.LEFT_COMMENT);
        final boolean hasLeft = leftComment != null && !leftComment.trim().isEmpty() && maxLhsComment > 0;

        final String rightComment = comment.getAssertionComment(Assertion.Comment.RIGHT_COMMENT);
        final boolean hasRight = rightComment != null && !rightComment.trim().isEmpty() && maxRhsComment > 0;

        StringBuilder builder = decorateComment? new StringBuilder("<html>") : new StringBuilder();
        if (hasLeft) {
            final String stringToDisplay;
            if(maxLhsComment < 4){
                stringToDisplay = leftComment.substring(0, maxLhsComment);
            } else{
                stringToDisplay = TextUtils.truncateStringAtEnd(leftComment, maxLhsComment);
            }
            String text = stringToDisplay;
            if (decorateComment) {
                text = TextUtils.escapeHtmlSpecialCharacters(text);
                builder.append("<font color=\"gray\">").append(text).append("</font>&nbsp;");
            } else {
                builder.append(text).append(" ");
            }
        }

        // just in case there's html tags in the display text
        displayText = TextUtils.escapeHtmlSpecialCharacters(displayText);
        builder.append(displayText);

        if (hasRight) {
            final String stringToDisplay;
            if(maxRhsComment < 4){
                stringToDisplay = rightComment.substring(0, maxRhsComment);
            } else{
                stringToDisplay = TextUtils.truncateStringAtEnd(rightComment, maxRhsComment);
            }
            String text = stringToDisplay;
            if (decorateComment) {
                text = TextUtils.escapeHtmlSpecialCharacters(text);
                builder.append("&nbsp;<font color=\"gray\">").append(text).append("</font>");
            } else {
                builder.append(text);
            }
        }

        if (decorateComment) builder.append("</html>");

        return builder.toString();
    }

    /**
     * Add any prefixes and suffixes to a static name based on supported features (like SecurityHeaderAddressable).
     * <p/>
     * This method is never invoked if the name is already a dynamic name, having come from a POLICY_NODE_NAME_FACTORY.
     *
     * @param name
     * @return the name, possibly with one or more prefixes or suffixes added.
     */
    protected static <AT extends Assertion> String addFeatureNames(final AT assertion, final String name) {
        return AssertionUtils.decorateName(assertion, name);
    }

    @Override
    protected String iconResource(boolean open) {
        final String s = (String)asAssertion().meta().get(AssertionMetadata.POLICY_NODE_ICON);
        return s != null ? s : "com/l7tech/console/resources/policy16.gif";
    }

    @Override
    protected String base64EncodedIconImage(boolean open) {
        final AssertionMetadata meta = asAssertion().meta();
        return meta.get(AssertionMetadata.BASE_64_NODE_IMAGE) == null ? null : meta.get(AssertionMetadata.BASE_64_NODE_IMAGE).toString();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Action[] getActions() {
        LinkedList<Action> list = new LinkedList<Action>(Arrays.asList(super.getActions()));
        int addIndex = getPreferredAction() == null ? 0 : 1;
        final Assertion assertion = asAssertion();
        if (assertion instanceof SecurityHeaderAddressable)
            list.add(addIndex, new EditXmlSecurityRecipientContextAction(this));
        if (assertion instanceof PrivateKeyable)
            list.add(addIndex, new EditKeyAliasForAssertion(this));
        if (assertion instanceof MessageTargetable)
            list.add(addIndex, new SelectMessageTargetAction(this));
        if (assertion instanceof IdentityTargetable)
            list.add(addIndex, new SelectIdentityTargetAction((AssertionTreeNode<? extends IdentityTargetable>)this));
        if (assertion instanceof NamespaceMigratable)
            list.add(addIndex, new MigrateNamespacesAction(this));
        
        return list.toArray(new Action[list.size()]);
    }

    @Override
    public  Action getPreferredAction() {
        return propertiesAction;
    }

    /**
     * Reload the Action used to display the properties dialog for the assertion.
     */
    public void reloadPropertiesAction() {
        //noinspection unchecked
        Functions.Unary< Action, AssertionTreeNode<AT> > factory =
                (Functions.Unary<Action, AssertionTreeNode<AT>>)
                        assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);
    }

    // - PRIVATE

    private static int getMaxRhsCommentLength() {
        final SsmPreferences ssmPreferences = TopComponents.getInstance().getPreferences();
        final String sMaxRightComment = ssmPreferences.getString(SsmPreferences.NUM_SSG_MAX_RIGHT_COMMENT, "100");
        if(sMaxRightComment != null && !sMaxRightComment.equals("")){
            try{
                return Integer.parseInt(sMaxRightComment);
            }catch(NumberFormatException nfe){
                //Swallow - incorrectly set property
                //don't need to set, it's has an internal default value
                logger.log( Level.FINE, "Ignoring invalid maximum right comment value ''{0}''.", sMaxRightComment);
            }
        }

        return SsmPreferences.DEFAULT_MAX_RIGHT_COMMENT;
    }

    private static int getMaxLhsCommentLength() {
        final SsmPreferences ssmPreferences = TopComponents.getInstance().getPreferences();
        final String sMaxLeftComment = ssmPreferences.getString(SsmPreferences.NUM_SSG_MAX_LEFT_COMMENT, "30");
        if(sMaxLeftComment != null && !sMaxLeftComment.equals("")){
            try{
                return Integer.parseInt(sMaxLeftComment);
            }catch(NumberFormatException nfe){
                //Swallow - incorrectly set property
                //don't need to set, it's has an internal default value
                logger.log( Level.FINE, "Ignoring invalid maximum left comment value ''{0}''.", sMaxLeftComment);
            }
        }

        return SsmPreferences.DEFAULT_MAX_LEFT_COMMENT;

    }
}