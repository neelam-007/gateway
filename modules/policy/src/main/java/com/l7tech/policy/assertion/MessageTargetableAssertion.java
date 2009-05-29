/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/** @author alex */
public abstract class MessageTargetableAssertion extends Assertion implements MessageTargetable, UsesVariables {

    //- PUBLIC

    @Override
    public TargetMessageType getTarget() {
        return targetSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        targetSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return targetSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        targetSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public String getTargetName() {
        return targetSupport.getTargetName(this);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return targetSupport.getVariablesUsed();
    }

    //- PROTECTED

    protected MessageTargetableAssertion() {
        this( TargetMessageType.REQUEST );
    }

    protected MessageTargetableAssertion( final TargetMessageType targetMessageType ) {
        targetSupport = new MessageTargetableSupport(targetMessageType);
    }

    protected void clearTarget() {
        targetSupport.clearTarget();
    }

    protected void copyFrom( final MessageTargetableAssertion other ) {
        this.setTarget( other.getTarget() );
        this.setOtherTargetMessageVariable( other.getOtherTargetMessageVariable() );
    }

    //- PRIVATE

    private MessageTargetableSupport targetSupport;

}
