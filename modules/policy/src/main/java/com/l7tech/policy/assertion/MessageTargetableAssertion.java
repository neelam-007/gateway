/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/** @author alex */
public abstract class MessageTargetableAssertion extends Assertion implements MessageTargetable, UsesVariables, SetsVariables {

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
    public boolean isTargetModifiedByGateway() {
        return targetSupport.isTargetModifiedByGateway();
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return targetSupport.getVariablesUsed();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return mergeVariablesSet(null);
    }

    @Override
    public MessageTargetableAssertion clone() {
        MessageTargetableAssertion mta = (MessageTargetableAssertion) super.clone();
        mta.targetSupport = new MessageTargetableSupport( targetSupport );
        return mta;
    }
    
    //- PROTECTED

    protected MessageTargetableAssertion() {
        this( TargetMessageType.REQUEST );
    }

    protected MessageTargetableAssertion( final TargetMessageType targetMessageType ) {
        targetSupport = new MessageTargetableSupport(targetMessageType);
    }

    protected MessageTargetableAssertion( boolean targetModifiedByGateway ) {
        this( TargetMessageType.REQUEST, targetModifiedByGateway );
    }

    protected MessageTargetableAssertion( final TargetMessageType targetMessageType, boolean targetModifiedByGateway ) {
        targetSupport = new MessageTargetableSupport(targetMessageType, targetModifiedByGateway);
    }

    protected void clearTarget() {
        targetSupport.clearTarget();
    }

    protected void copyFrom( final MessageTargetableAssertion other ) {
        this.setTarget( other.getTarget() );
        this.setOtherTargetMessageVariable( other.getOtherTargetMessageVariable() );
    }

    protected void setTargetModifiedByGateway(boolean targetModifiedByGateway) {
        targetSupport.setTargetModifiedByGateway(targetModifiedByGateway);
    }

    protected VariableMetadata getOtherTargetVariableMetadata() {
        return targetSupport.getOtherTargetVariableMetadata();
    }

    /**
     * Prepends an additional entry for the target message, if {@link #isTargetModifiedByGateway()} is true and {@link #getTarget()} is {@link TargetMessageType#OTHER}.
     *
     * @param otherVariablesSet  variables set, or null.  Null is treated as equivalent to empty (and is faster than creating a new zero-length array just for this purpose).
     * @return variables with getOtherTargetVariableMetadata() prepended, if applicable.  Never null, but may be empty.
     */
    protected VariableMetadata[] mergeVariablesSet(VariableMetadata[] otherVariablesSet) {
        return targetSupport.mergeVariablesSet(otherVariablesSet);
    }

    //- PRIVATE

    private MessageTargetableSupport targetSupport;

}
