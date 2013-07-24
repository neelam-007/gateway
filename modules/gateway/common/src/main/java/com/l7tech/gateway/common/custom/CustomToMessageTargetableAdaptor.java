/*
 * Copyright (C) 2013 Layer 7 Technologies Inc.
 */

package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.variable.VariableMetadata;

import org.jetbrains.annotations.NotNull;

/**
 * Converts CustomMessageTargetable into MessageTargetable object.
 */
public final class CustomToMessageTargetableAdaptor implements MessageTargetable {

    /**
     * Input {@link CustomMessageTargetable} object
     */
    private final CustomMessageTargetable customMessageTargetable;
    final public CustomMessageTargetable getCustomMessageTargetable() {
        return customMessageTargetable;
    }

    /**
     * Constructor
     *
     * @param customMessageTargetable {@link CustomMessageTargetable} object to convert. Cannot be null.
     */
    public CustomToMessageTargetableAdaptor(@NotNull final CustomMessageTargetable customMessageTargetable) {
        this.customMessageTargetable = customMessageTargetable;
    }

    /**
     * Convert CustomTargetMessage variable into {@link TargetMessageType}
     * @see CustomMessageTargetableSupport#targetMessageVariable
     *
     * @param messageVariableName custom message target to convert
     * @return {@link TargetMessageType} type from messageTarget.
     */
    static private TargetMessageType convertToTargetMessageType(final String messageVariableName) {
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
     * Convert {@link TargetMessageType} into custom TargetMessage variable
     * @see CustomMessageTargetableSupport#targetMessageVariable
     *
     * @param targetMessageType {@link TargetMessageType} type to convert
     * @return {@link CustomMessageTargetableSupport#TARGET_REQUEST},
     *         {@link CustomMessageTargetableSupport#TARGET_RESPONSE} or empty string if targetMessageType is
     *         {@link TargetMessageType#REQUEST}, {@link TargetMessageType#RESPONSE} or {@link TargetMessageType#OTHER} respectively
     */
    static private String convertToMessageVariableName(final TargetMessageType targetMessageType) {
        if (targetMessageType == TargetMessageType.REQUEST) {
            return CustomMessageTargetableSupport.TARGET_REQUEST;
        } else if (targetMessageType == TargetMessageType.RESPONSE) {
            return CustomMessageTargetableSupport.TARGET_RESPONSE;
        }
        return "";
    }

    @Override
    public TargetMessageType getTarget() {
        return convertToTargetMessageType(getCustomMessageTargetable().getTargetMessageVariable());
    }

    @Override
    public void setTarget(TargetMessageType target) {
        getCustomMessageTargetable().setTargetMessageVariable(convertToMessageVariableName(target));
    }

    @Override
    public String getOtherTargetMessageVariable() {
        final String messageTarget = getCustomMessageTargetable().getTargetMessageVariable();
        if (messageTarget == null ||
                messageTarget.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_REQUEST) == 0 ||
                messageTarget.compareToIgnoreCase(CustomMessageTargetableSupport.TARGET_RESPONSE) == 0) {
            return null;
        }
        return messageTarget;
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        getCustomMessageTargetable().setTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return getCustomMessageTargetable().getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return getCustomMessageTargetable().isTargetModifiedByGateway();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return getCustomMessageTargetable().getVariablesSet();
    }

    @Override
    public String[] getVariablesUsed() {
        return getCustomMessageTargetable().getVariablesUsed();
    }
}

