/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.VariableUseSupport;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * @author alex
 * @version $Revision$
 */
@RequiresSOAP(wss=true)
public abstract class WssCredentialSourceAssertion extends SecurityHeaderAddressableSupport implements MessageTargetable {

    protected WssCredentialSourceAssertion() {
        messageTargetableSupport.setTargetModifiedByGateway(false);
    }

    //- PUBLIC

    /**
     * Always an credential source
     *
     * @return always true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public void setTarget(final TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(final String otherTargetMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public final String[] getVariablesUsed() {
        return doGetVariablesUsed().asArray();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return messageTargetableSupport.getMessageTargetVariablesSet().asArray();
    }

    @Override
    public WssCredentialSourceAssertion clone() {
        WssCredentialSourceAssertion wssca = (WssCredentialSourceAssertion) super.clone();
        wssca.messageTargetableSupport = new MessageTargetableSupport( messageTargetableSupport );
        return wssca;
    }

    protected VariablesUsed doGetVariablesUsed(boolean includeOtherVariable) {
        final VariablesUsed variablesUsed;
        if (includeOtherVariable) {
            variablesUsed = new VariablesUsed(messageTargetableSupport.getMessageTargetVariablesUsed().asArray());
        } else {
            variablesUsed = new VariablesUsed();
        }

        return variablesUsed;
    }

    protected VariablesUsed doGetVariablesUsed() {
        return doGetVariablesUsed(true);
    }

    protected static final class VariablesUsed extends VariableUseSupport.VariablesUsedSupport<VariablesUsed> {
        private VariablesUsed(){
        }

        private VariablesUsed( final String[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesUsed get() {
            return this;
        }
    }

    //- PRIVATE

    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport();
}
