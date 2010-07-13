/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetableSupport;
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
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getVariablesUsed();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return messageTargetableSupport.getVariablesSet();
    }

    @Override
    public WssCredentialSourceAssertion clone() {
        WssCredentialSourceAssertion wssca = (WssCredentialSourceAssertion) super.clone();
        wssca.messageTargetableSupport = new MessageTargetableSupport( messageTargetableSupport );
        return wssca;
    }

    //- PRIVATE

    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport();
}
