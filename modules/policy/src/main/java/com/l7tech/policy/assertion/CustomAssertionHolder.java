/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.assertion.ext.validator.CustomPolicyValidator;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.common.io.ClassLoaderObjectInputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The custom assertion holder is a placeholder <code>Assertion</code>
 * that wraps a bean instance that implements the <code>CustomAssertion</code>
 * The <code>customAssertionBean</code> property is required to
 * be serializable and must offer JavaBean style get/set operations.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 * @see CustomAssertion
 */
public class CustomAssertionHolder extends Assertion implements UsesVariables, SetsVariables, MessageTargetable {
    /**
     * Serialization id, maintain to indicate serialization compatibility
     * with a previous versions of the  class.
     */
    private static final long serialVersionUID = 7410439507802944818L;

    private static final Logger logger = Logger.getLogger(CustomAssertionHolder.class.getName());
    protected static final String CUSTOM_ASSERTION = "Custom Assertion";

    private CustomAssertion customAssertion;
    private Category category;
    private String descriptionText;
    private String paletteNodeName;
    private String policyNodeName;
    private boolean isUiAutoOpen;

    public CustomAssertionHolder() {
        this.parent = null;
    }

    /**
     * @return the custom assertion bean
     */
    public CustomAssertion getCustomAssertion() {
        return customAssertion;
    }

    /**
     * @return the custom assertion category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Set the custom assertion category
     *
     * @param category the new category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Set the custome assertion bean
     *
     * @param ca the new custom assertion bean
     */
    public void setCustomAssertion(CustomAssertion ca) {
        this.customAssertion = ca;
    }

    @Override
    public String toString() {
        if (customAssertion == null) {
            return "[ CustomAssertion = null ]";
        }
        return "[ CustomAssertion = " + customAssertion.toString() + ", Category = " + category + " ]";
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }

    public String getPaletteNodeName() {
        return paletteNodeName;
    }

    public void setPaletteNodeName(String paletteNodeName) {
        this.paletteNodeName = paletteNodeName;
    }

    public String getPolicyNodeName() {
        return policyNodeName;
    }

    public void setPolicyNodeName(String policyNodeName) {
        this.policyNodeName = policyNodeName;
    }

    public boolean getIsUiAutoOpen() {
        return isUiAutoOpen;
    }

    public void setIsUiAutoOpen(boolean isUiAutoOpen) {
        this.isUiAutoOpen = isUiAutoOpen;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (customAssertion == null || !(customAssertion instanceof UsesVariables) ) {
            return new String[0];
        }
        UsesVariables usesVariables = (UsesVariables) customAssertion;
        return usesVariables.getVariablesUsed();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (customAssertion == null || !(customAssertion instanceof SetsVariables) ) {
            return new VariableMetadata[0];
        }
        SetsVariables setsVariables = (SetsVariables) customAssertion;
        return setsVariables.getVariablesSet();
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        CustomAssertionHolder clone = (CustomAssertionHolder) super.clone();
        clone.category = this.category;
        clone.descriptionText = this.descriptionText;
        clone.customAssertion = this.customAssertion; // in case deep copy fails

        if ( this.customAssertion != null ) {
            // Attempt serialization round trip to avoid returning the same instance
            // every time ... note that custom assertion data objects are NOT cloneable
            ObjectInputStream in = null;
            ObjectOutputStream out = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(baos);
                out.writeObject(this.customAssertion);
                out.flush();
                in = new ClassLoaderObjectInputStream(
                        new ByteArrayInputStream(baos.toByteArray()),
                        this.customAssertion.getClass().getClassLoader());
                clone.customAssertion  = (CustomAssertion) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                logger.log( Level.FINE, "Error serializing assertion.", e);
            } catch (SecurityException se) {
                logger.log( Level.FINE, "Permission denied when serializing assertion.");
            } catch (RuntimeException re) {
                logger.log( Level.FINE, "Unexpected error when serializing assertion.", re);
            } finally {
                ResourceUtils.closeQuietly(in);
                ResourceUtils.closeQuietly(out);
            }
        }

        return clone;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, customAssertion == null ? CUSTOM_ASSERTION : customAssertion.getName());
        meta.put(DESCRIPTION, descriptionText == null ? CUSTOM_ASSERTION : descriptionText);
        meta.put(POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<CustomAssertionHolder>(){
            @Override
            public String getAssertionName( CustomAssertionHolder assertion, boolean decorate) {
                final CustomAssertion ca = getCustomAssertion();

                String policyNodeName = assertion.getPolicyNodeName();
                // If there is no Policy Node Name setup, then get name from the Custom Assertion
                if (policyNodeName == null) policyNodeName = ca.getName();

                if (policyNodeName == null) {
                    policyNodeName = "Unspecified custom assertion (class '" + ca.getClass() + "'";
                }
                return (decorate) ? AssertionUtils.decorateName(assertion, policyNodeName) : policyNodeName;
            }
        });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/custom.gif");

        if (customAssertion instanceof CustomPolicyValidator) {
            meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.console.util.CustomAssertionHolderValidator");
        }

        if (this.getIsUiAutoOpen()) {
            meta.put(POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.CustomAssertionHolderAdvice");
        }

        return meta;
    }
    /**
     * This is duplicate of com.l7tech.gateway.common.custom.CustomToMessageTargetableConverter#convertToTargetMessageType.
     * <p/>
     * !!NOTE!!
     * <p/>
     * CustomToMessageTargetableConverter is used in both <i>layer7-gateway-console</i> and <i>layer7-gateway-server</i> modules.
     * Since CustomAssertionHolder is inside <i>layer7-policy</i> module, the only module common to all 3 is <i>layer7-policy-exporter</i>.
     * However, <i>layer7-policy-exporter</i> doesn't look like a right place for CustomToMessageTargetableConverter class.
     *
     * For POC purpose, we'll going to keep duplicate functions and change them afterwards.
     */
    private TargetMessageType convertToTargetMessageType(final String messageVariableName) {
        if (messageVariableName == null) {
            return TargetMessageType.OTHER;
        } else if (messageVariableName.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_REQUEST) == 0) {
            return TargetMessageType.REQUEST;
        } else if (messageVariableName.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_RESPONSE) == 0) {
            return TargetMessageType.RESPONSE;
        }
        return TargetMessageType.OTHER;
    }

    /**
     * This is duplicate of com.l7tech.gateway.common.custom.CustomToMessageTargetableConverter#convertToMessageVariableName.
     *
     * !!NOTE!!
     * CustomToMessageTargetableConverter is used in both <i>layer7-gateway-console</i> and <i>layer7-gateway-server</i> modules.
     * Since CustomAssertionHolder is inside <i>layer7-policy</i> module, the only module common to all 3 is <i>layer7-policy-exporter</i>.
     * However, <i>layer7-policy-exporter</i> doesn't look like a right place for CustomToMessageTargetableConverter class.
     *
     * For POC purpose, we'll going to keep duplicate functions and change them afterwards.
     */
    private String convertToMessageVariableName(final TargetMessageType targetMessageType) {
        if (targetMessageType == TargetMessageType.REQUEST) {
            return CustomMessageTargetableSupport.TARGET_REQUEST;
        } else if (targetMessageType == TargetMessageType.RESPONSE) {
            return CustomMessageTargetableSupport.TARGET_RESPONSE;
        }
        return "";
    }

    @Override
    public TargetMessageType getTarget() {
        if (customAssertion instanceof CustomMessageTargetable) {
            return convertToTargetMessageType(((CustomMessageTargetable) customAssertion).getTargetMessageVariable());
        } else {
            return TargetMessageType.REQUEST;
        }
    }

    @Override
    public void setTarget(TargetMessageType target) {
        if (customAssertion instanceof CustomMessageTargetable) {
            ((CustomMessageTargetable) customAssertion).setTargetMessageVariable(convertToMessageVariableName(target));
        }
    }

    @Override
    public String getOtherTargetMessageVariable() {
        if (customAssertion instanceof CustomMessageTargetable) {
            final String messageTarget = ((CustomMessageTargetable) customAssertion).getTargetMessageVariable();
            if (messageTarget == null ||
                    messageTarget.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_REQUEST) == 0 ||
                    messageTarget.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_RESPONSE) == 0) {
                return null;
            }
            return messageTarget;
        } else {
            return null;
        }
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        if (customAssertion instanceof CustomMessageTargetable) {
            ((CustomMessageTargetable) customAssertion).setTargetMessageVariable(otherMessageVariable);
        }
    }

    @Override
    public String getTargetName() {
        if (customAssertion instanceof CustomMessageTargetable) {
            return ((CustomMessageTargetable) customAssertion).getTargetName();
        } else {
            return null;
        }
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return customAssertion instanceof CustomMessageTargetable && ((CustomMessageTargetable) customAssertion).isTargetModifiedByGateway();
    }
}

