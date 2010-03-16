package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (otherTargetMessageVariable != null) return new String[] { otherTargetMessageVariable };
        return new String[0];
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return mergeVariablesSet(null);
    }

    /**
     * Check whether the target message might be modified by the assertion.
     *
     * @return true if the target message might be modified; false if the target message is only read.
     */
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
     * Get the variable metadata describing the target message, if it is a message variable that may be modified.
     * <p/>
     * This method returns a VariableMetadata instance describing the target message if it is
     * {@link TargetMessageType#OTHER} and {@link #isTargetModifiedByGateway()} is true;
     * otherwise, this method returns null.
     *
     * @return a VariableMetadata instance describing a target Message variable that may be modified in-place, or null.
     */
    public VariableMetadata getOtherTargetVariableMetadata() {
        return TargetMessageType.OTHER.equals(getTarget()) && isTargetModifiedByGateway()
                ? new VariableMetadata(getOtherTargetMessageVariable(), false, false, null, true, DataType.MESSAGE)
                : null;
    }

    /**
     * Prepends an additional entry to a list of variables set representing a modified target message variable, if any.
     *
     * @param otherVariablesSet  variables set, or null.  Null is treated as equivalent to empty.
     * @return variables with getOtherTargetVariableMetadata() prepended, if applicable.  Never null, but may be empty.
     */
    public VariableMetadata[] mergeVariablesSet(VariableMetadata[] otherVariablesSet) {
        List<VariableMetadata> ret = new ArrayList<VariableMetadata>();
        VariableMetadata targetVariableMetadata = getOtherTargetVariableMetadata();
        if (targetVariableMetadata != null)
            ret.add(targetVariableMetadata);
        if (otherVariablesSet != null && otherVariablesSet.length > 0)
            ret.addAll(Arrays.asList(otherVariablesSet));
        return ret.toArray(new VariableMetadata[ret.size()]);
    }

    public void setTargetMessage( final MessageTargetable messageTargetable ) {
        if ( messageTargetable != null ) {
            this.target = messageTargetable.getTarget();
            if ( this.target == TargetMessageType.OTHER ) {
                this.otherTargetMessageVariable = messageTargetable.getOtherTargetMessageVariable();
            }
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

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (target != null ? target.hashCode() : 0);
        result = 31 * result + (otherTargetMessageVariable != null ? otherTargetMessageVariable.hashCode() : 0);
        result = 31 * result + (targetModifiedByGateway ? 1231 : 1237);
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
}
