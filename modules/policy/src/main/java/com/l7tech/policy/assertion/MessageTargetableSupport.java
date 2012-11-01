package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.VariableUseSupport.VariablesSetSupport;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesUsedSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.io.Serializable;

/**
 * Support class for MessageTargetable implementation.
 */
public class MessageTargetableSupport implements MessageTargetable, Serializable {

    //- PUBLIC

    /**
     * Create a instance targetting the request message.
     */
    public MessageTargetableSupport() {
        this( TargetMessageType.REQUEST );
    }

    /**
     * Create a instance targetting the given message.
     *
     * @param targetMessageType The request/response target
     */
    public MessageTargetableSupport( final TargetMessageType targetMessageType ) {
        this.target = targetMessageType;
    }

    /**
     * Create a instance targetting the given message variable.
     *
     * @param otherTargetMessageVariable The variable target
     */
    public MessageTargetableSupport( final String otherTargetMessageVariable ) {
        this.target = TargetMessageType.OTHER;
        this.otherTargetMessageVariable = otherTargetMessageVariable;
    }

    /**
     * Create a instance targetting the given message target.
     *
     * @param messageTargetable The target message to use
     */
    public MessageTargetableSupport( final MessageTargetable messageTargetable ) {
        setTargetMessage( messageTargetable );
    }

    /**
     * Create a instance targetting the request message and setting the modification flag.
     *
     * @param targetModifiedByGateway true if the target message might be modified by the server implementation; false if the target message is only read by the server assertion.
     */
    public MessageTargetableSupport(boolean targetModifiedByGateway) {
        this();
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    /**
     * Create a instance targetting the given message.
     *
     * @param targetMessageType The request/response target
     * @param targetModifiedByGateway true if the target message might be modified by the server implementation; false if the target message is only read by the server assertion.
     */
    public MessageTargetableSupport(TargetMessageType targetMessageType, boolean targetModifiedByGateway) {
        this(targetMessageType);
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    /**
     * Create a instance targetting the given message variable.
     *
     * @param otherTargetMessageVariable The variable target
     * @param targetModifiedByGateway true if the target message might be modified by the server implementation; false if the target message is only read by the server assertion.
     */
    public MessageTargetableSupport(String otherTargetMessageVariable, boolean targetModifiedByGateway) {
        this(otherTargetMessageVariable);
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    @Override
    public TargetMessageType getTarget() {
        return target;
    }

    @Override
    public void setTarget(TargetMessageType target) {
        if (target == null) throw new NullPointerException();
        this.target = target;
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return otherTargetMessageVariable;
    }

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        this.otherTargetMessageVariable = "".equals(otherTargetMessageVariable) ? null : otherTargetMessageVariable;
    }

    @Override
    public String getTargetName() {
        return getTargetName(this);
    }

    /**
     * @see #getMessageTargetVariablesSet
     */
    @Override
    public final VariableMetadata[] getVariablesSet() {
        return getMessageTargetVariablesSet().asArray();
    }

    /**
     * @see #getMessageTargetVariablesUsed
     */
    @Override
    public final String[] getVariablesUsed() {
        return getMessageTargetVariablesUsed().asArray();
    }

    /**
     * Get the variable metadata describing the target message, if it is a message variable is required.
     * <p/>
     * This method returns a VariablesUsed containing the names of the
     * variable for the target message if it is {@link TargetMessageType#OTHER}
     * and the target is required by the gateway, otherwise this method returns
     * an empty VariablesUsed.
     *
     * @return a VariablesUsed containing any variable names for the target message.
     */
    public VariablesUsed getMessageTargetVariablesUsed() {
        return new VariablesUsed( sourceUsedByGateway ? new String[]{ otherTargetMessageVariable } : new String[0] );
    }

    /**
     * Get the variable metadata describing the target message, if it is a message variable that may be modified.
     * <p/>
     * This method returns a VariablesSet containing a VariableMetadata
     * instance describing the target message if it is
     * {@link TargetMessageType#OTHER} and {@link #isTargetModifiedByGateway()}
     * is true otherwise, this method returns an empty VariablesSet.
     *
     * @return a VariablesSet containing any VariableMetadata for the target message.
     */
    public VariablesSet getMessageTargetVariablesSet() {
        return new VariablesSet( new VariableMetadata[]{getOtherTargetVariableMetadata()} );
    }

    public static final class VariablesUsed extends VariablesUsedSupport<VariablesUsed> {
        private VariablesUsed( final String[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesUsed get() {
            return this;
        }
    }

    public static final class VariablesSet extends VariablesSetSupport<VariablesSet> {
        private VariablesSet( final VariableMetadata[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesSet get() {
            return this;
        }
    }

    /**
     * Check whether the target message might be modified by the assertion.
     *
     * @return true if the target message might be modified; false if the target message is only read.
     */
    @Override
    public boolean isTargetModifiedByGateway() {
        return targetModifiedByGateway;
    }

    /**
     * Set whether the target message might be modified by the assertion.
     *
     * @param targetModifiedByGateway true if the target message might be modified; false if the target message is only read.
     */
    public void setTargetModifiedByGateway(boolean targetModifiedByGateway) {
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    /**
     * Check whether the source message is required by the assertion.
     *
     * @return true if the source message is required (so it is a policy error if not available)
     */
    public boolean isSourceUsedByGateway() {
        return sourceUsedByGateway;
    }

    /**
     * Set whether the source message is required by the assertion.
     *
     * @param sourceUsedByGateway true to require the source message
     */
    public void setSourceUsedByGateway( final boolean sourceUsedByGateway ) {
        this.sourceUsedByGateway = sourceUsedByGateway;
    }

    public void setTargetMessage( final MessageTargetable messageTargetable ) {
        if ( messageTargetable != null ) {
            this.target = messageTargetable.getTarget();
            if ( this.target == TargetMessageType.OTHER ) {
                this.otherTargetMessageVariable = messageTargetable.getOtherTargetMessageVariable();
            }
            this.targetModifiedByGateway = messageTargetable.isTargetModifiedByGateway();
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageTargetableSupport that = (MessageTargetableSupport) o;

        if (otherTargetMessageVariable != null ? !otherTargetMessageVariable.equals(that.otherTargetMessageVariable) : that.otherTargetMessageVariable != null)
            return false;
        if (target != that.target) return false;
        if (targetModifiedByGateway != that.targetModifiedByGateway) return false;
        if (sourceUsedByGateway != that.sourceUsedByGateway) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (target != null ? target.hashCode() : 0);
        result = 31 * result + (otherTargetMessageVariable != null ? otherTargetMessageVariable.hashCode() : 0);
        result = 31 * result + (targetModifiedByGateway ? 1231 : 1237);
        result = 31 * result + (sourceUsedByGateway ? 1231 : 1237);
        return result;
    }

    //- PROTECTED

    /**
     * Get the target name for the given MessageTargetable.
     *
     * <p>This will be null if the given MessageTargetable is null, or if the
     * given MessageTargetable has a null target.</p>
     *
     * @param targetable The targetable to check
     * @return The target name or null
     */
    protected String getTargetName( final MessageTargetable targetable ) {
        String target = null;

        if ( targetable != null && targetable.getTarget() != null ) {
            switch(targetable.getTarget()) {
                case REQUEST:
                    target = "Request";
                    break;
                case RESPONSE:
                    target = "Response";
                    break;
                case OTHER:
                    target = "${" + targetable.getOtherTargetMessageVariable() + "}";
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        return target;
    }

    protected void clearTarget() {
        target = null;
    }

    //- PRIVATE

    private TargetMessageType target;
    private String otherTargetMessageVariable;
    private boolean targetModifiedByGateway;
    private boolean sourceUsedByGateway = true;

    private VariableMetadata getOtherTargetVariableMetadata() {
        return TargetMessageType.OTHER.equals(getTarget()) && isTargetModifiedByGateway()
                ? new VariableMetadata(getOtherTargetMessageVariable(), false, false, null, true, DataType.MESSAGE)
                : null;
    }
}
