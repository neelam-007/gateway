package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesSetSupport;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesUsedSupport;
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
    public final String[] getVariablesUsed() {
        return doGetVariablesUsed().asArray();
    }

    @Override
    public final VariableMetadata[] getVariablesSet() {
        return doGetVariablesSet().asArray();
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Object clone() {
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

    protected void setSourceUsedByGateway( final boolean sourceUsedByGateway ) {
        targetSupport.setSourceUsedByGateway( sourceUsedByGateway );
    }

    protected VariablesUsed doGetVariablesUsed() {
        return new VariablesUsed( targetSupport.getMessageTargetVariablesUsed().asArray() );
    }

    protected VariablesSet doGetVariablesSet() {
        return new VariablesSet( targetSupport.getMessageTargetVariablesSet().asArray() );
    }

    protected static final class VariablesUsed extends VariablesUsedSupport<VariablesUsed> {
        private VariablesUsed( final String[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesUsed get() {
            return this;
        }
    }

    protected static final class VariablesSet extends VariablesSetSupport<VariablesSet> {
        private VariablesSet( final VariableMetadata[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesSet get() {
            return this;
        }
    }

    //- PRIVATE

    private MessageTargetableSupport targetSupport;

}
