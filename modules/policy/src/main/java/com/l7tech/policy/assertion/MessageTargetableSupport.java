package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.io.*;

/**
 * Support class for MessageTargetable implementation.
 */
public class MessageTargetableSupport implements MessageTargetable, UsesVariables, Serializable {

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
     * Create a instance targetting the given message target.
     *
     * @param messageTargetable The target message to use
     */
    public MessageTargetableSupport( final MessageTargetable messageTargetable ) {
        setTargetMessage( messageTargetable );
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

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (target != null ? target.hashCode() : 0);
        result = 31 * result + (otherTargetMessageVariable != null ? otherTargetMessageVariable.hashCode() : 0);
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

}
