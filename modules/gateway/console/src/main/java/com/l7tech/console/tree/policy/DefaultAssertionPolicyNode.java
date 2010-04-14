/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.SelectIdentityTargetAction;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default PolicyNode for assertions that don't provide a custom one of their own.
 */
public class DefaultAssertionPolicyNode<AT extends Assertion> extends LeafAssertionTreeNode<AT> {
    private static final Logger logger = Logger.getLogger(DefaultAssertionPolicyNode.class.getName());
    private static final int MAX_CACHED_TIME = 1000 * 60 * 5; // 5 mins
    private static CachedValue<Integer> rhsCachedValue = null;
    private static CachedValue<Integer> lhsCachedValue = null;


    private final Action propertiesAction;

    public DefaultAssertionPolicyNode(AT assertion) {
        super(assertion);
        
        //noinspection unchecked
        Functions.Unary< Action, AssertionTreeNode<AT> > factory =
                (Functions.Unary<Action, AssertionTreeNode<AT>>)
                        assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);
    }

    @Override
    public String getName(final boolean decorate) {
        return getNameFromMeta(asAssertion(), decorate);
    }

    public static <AT extends Assertion> String getNameFromMeta(final AT assertion, final boolean decorate){
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
        return (decorate)? addCommentToDisplayText(assertion, displayText): displayText;
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
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        final String showState = preferences.getString(PolicyEditorPanel.SHOW_COMMENTS);
        final boolean shown = Boolean.parseBoolean(showState);
        if(!shown) return displayText;

        Assertion.Comment comment = assertion.getAssertionComment();

        if(comment == null) return displayText;

        final Map<String,String> props = comment.getProperties();
        final String style = props.get(Assertion.Comment.COMMENT_ALIGN);

        int maxRhsComment = getMaxRhsCommentLength();
        int maxLhsComment = getMaxLhsCommentLength();

        if (style != null && style.equals(Assertion.Comment.LEFT_ALIGN)) {
            return TextUtils.truncateStringAtEnd(comment.getComment(), maxLhsComment) + " " + displayText;
        } else {
            return displayText + " " + TextUtils.truncateStringAtEnd(comment.getComment(), maxRhsComment);
        }
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

    @SuppressWarnings({"unchecked"})
    @Override
    public Action[] getActions() {
        LinkedList<Action> list = new LinkedList<Action>(Arrays.asList(super.getActions()));
        int addIndex = getPreferredAction() == null ? 0 : 1;
        if (asAssertion() instanceof SecurityHeaderAddressable)
            list.add(addIndex, new EditXmlSecurityRecipientContextAction(this));
        if (asAssertion() instanceof PrivateKeyable)
            list.add(addIndex, new EditKeyAliasForAssertion(this));
        if (asAssertion() instanceof MessageTargetable)
            list.add(addIndex, new SelectMessageTargetAction(this));
        if (asAssertion() instanceof IdentityTargetable)
            list.add(addIndex, new SelectIdentityTargetAction((AssertionTreeNode<? extends IdentityTargetable>)this));
        
        return list.toArray(new Action[list.size()]);
    }

    @Override
    public  Action getPreferredAction() {
        return propertiesAction;
    }

    // - PRIVATE

    private static class CachedValue<T> {
        private long lastUpdateTime;
        private T value;

        private CachedValue(T value) {
            this.value = value;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public T getCachedValue() {
            return value;
        }
    }

    private static int getMaxRhsCommentLength() {
        int returnValue = 100;
        if (rhsCachedValue != null) {
            if (rhsCachedValue.getLastUpdateTime() < System.currentTimeMillis() - MAX_CACHED_TIME) {
                //need to updated cached value
                rhsCachedValue = getClusterPropertyValue("policyEditor.maxRhsCommentSize", returnValue);
            }else{
                returnValue = rhsCachedValue.getCachedValue();
            }
        }else {
            rhsCachedValue = getClusterPropertyValue("policyEditor.maxRhsCommentSize", returnValue);
            returnValue = rhsCachedValue.getCachedValue();
        }

        return returnValue;
    }

    private static int getMaxLhsCommentLength() {
        int returnValue = 30;
        if (lhsCachedValue != null) {
            if (lhsCachedValue.getLastUpdateTime() < System.currentTimeMillis() - MAX_CACHED_TIME) {
                //need to updated cached value
                lhsCachedValue = getClusterPropertyValue("policyEditor.maxLhsCommentSize", returnValue);
            }else{
                returnValue = lhsCachedValue.getCachedValue();
            }
        }else {
            lhsCachedValue = getClusterPropertyValue("policyEditor.maxLhsCommentSize", returnValue);
            returnValue = lhsCachedValue.getCachedValue();
        }

        return returnValue;
    }

    private static CachedValue<Integer> getClusterPropertyValue(String value, int defaultValue){
        CachedValue<Integer> returnValue = new CachedValue<java.lang.Integer>(defaultValue);
        try {
            Registry reg = Registry.getDefault();
            ClusterProperty clusterProperty = reg.getClusterStatusAdmin().findPropertyByName(value);

            if(clusterProperty != null){
                returnValue = new CachedValue<java.lang.Integer>(Integer.parseInt(clusterProperty.getValue()));
            }
        } catch (FindException e) {
            logger.log(Level.INFO, "Could not find cluster property '" + value + "': " + ExceptionUtils.getMessage(e));
        }
        catch (Exception e) {
            logger.log(Level.INFO, "Problem with cluster property's value for property '" + value + "': " + ExceptionUtils.getMessage(e));
        }

        return returnValue;
    }
    
}
