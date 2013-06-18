package com.l7tech.policy.assertion.ext.targetable;

import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class for CustomMessageTargetable implementation.
 */
public final class CustomMessageTargetableSupport implements CustomMessageTargetable, Serializable {
    private static final Logger logger = Logger.getLogger(CustomMessageTargetableSupport.class.getName());

    // backwards compatible
    private static final long serialVersionUID = -7171998403662060182L;

    public static final String TARGET_REQUEST = "request";
    public static final String TARGET_RESPONSE = "response";

    //- PUBLIC

    /**
     * Create a instance targeting the request message.
     */
    public CustomMessageTargetableSupport() {
        this(TARGET_REQUEST);
    }

    /**
     * Create a instance targeting the given message variable.
     *
     * @param targetMessageVariable The variable target
     */
    public CustomMessageTargetableSupport(final String targetMessageVariable) {
        setTargetMessageVariable(targetMessageVariable);
    }

    /**
     * Create a instance targeting the given message target.
     *
     * @param messageTargetable The target message to use
     */
    public CustomMessageTargetableSupport(final CustomMessageTargetable messageTargetable) {
        setTargetMessage(messageTargetable);
    }

    /**
     * Create a instance targeting the request message and setting the modification flag.
     *
     * @param targetModifiedByGateway true if the target message might be modified by the server implementation; false if the target message is only read by the server assertion.
     */
    public CustomMessageTargetableSupport(boolean targetModifiedByGateway) {
        this();
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    /**
     * Create a instance targeting the given message variable.
     *
     * @param targetMessageVariable The variable target
     * @param targetModifiedByGateway true if the target message might be modified by the server implementation; false if the target message is only read by the server assertion.
     */
    public CustomMessageTargetableSupport(final String targetMessageVariable, boolean targetModifiedByGateway) {
        this(targetMessageVariable);
        this.targetModifiedByGateway = targetModifiedByGateway;
    }

    public String getTargetMessageVariable() {
        return targetMessageVariable;
    }

    public void setTargetMessageVariable(String targetMessageVariable) {
        this.targetMessageVariable = "".equals(targetMessageVariable) ? null : targetMessageVariable;
    }

    public String getTargetName() {
        return getTargetName(this);
    }

    /**
     * Check whether the target message might be modified by the assertion.
     *
     * @return true if the target message might be modified; false if the target message is only read.
     */
    public boolean isTargetModifiedByGateway() {
        return targetModifiedByGateway;
    }

    private boolean isTargetingRequestOrResponse() {
        return (targetMessageVariable != null && !targetMessageVariable.isEmpty() &&
                (targetMessageVariable.compareToIgnoreCase(TARGET_REQUEST) == 0 ||
                 targetMessageVariable.compareToIgnoreCase(TARGET_RESPONSE) == 0));
    }

    public VariableMetadata[] getVariablesSet() {
        final VariableMetadata varMeta = getTargetVariableMetadata();
        return (varMeta != null) ? new VariableMetadata[]{ varMeta } : new VariableMetadata[0];
    }

    public String[] getVariablesUsed() {
        return (isSourceUsedByGateway() &&
                targetMessageVariable != null &&
                !targetMessageVariable.isEmpty() &&
                !isTargetingRequestOrResponse()) ? new String[]{targetMessageVariable} : new String[0];
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

    public void setTargetMessage( final CustomMessageTargetable messageTargetable ) {
        if ( messageTargetable != null ) {
            setTargetMessageVariable(messageTargetable.getTargetMessageVariable());
            this.targetModifiedByGateway = messageTargetable.isTargetModifiedByGateway();
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomMessageTargetableSupport that = (CustomMessageTargetableSupport) o;

        if (isTargetingRequestOrResponse() != that.isTargetingRequestOrResponse())
            return false;

        if (targetMessageVariable != null ?
                isTargetingRequestOrResponse() ? targetMessageVariable.compareToIgnoreCase(that.targetMessageVariable) != 0 : !targetMessageVariable.equals(that.targetMessageVariable)
                : that.targetMessageVariable != null)
            return false;
        if (targetModifiedByGateway != that.targetModifiedByGateway) return false;
        if (sourceUsedByGateway != that.sourceUsedByGateway) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        //result = (target != null ? target.hashCode() : 0);
        result = (targetMessageVariable != null ? targetMessageVariable.hashCode() : 0);
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
    protected String getTargetName( final CustomMessageTargetable targetable ) {
        String target = null;

        if ( targetable != null && targetMessageVariable != null ) {
            if (targetMessageVariable.compareToIgnoreCase(TARGET_REQUEST) == 0) {
                target = "Request";
            } else if (targetMessageVariable.compareToIgnoreCase(TARGET_RESPONSE) == 0) {
                target = "Response";
            } else {
                target = "${" + targetable.getTargetMessageVariable() + "}";
            }
        }

        return target;
    }

    //- PRIVATE

    /**
     * <p>in order to target default Request set the targetMessageVariable to {@link #TARGET_REQUEST}</p>
     * <p>in order to target default Response set the targetMessageVariable to {@link #TARGET_RESPONSE}</p>
     * <p>any other value will be resolved as context variable message.</p>
     */
    private String targetMessageVariable;

    private boolean targetModifiedByGateway;
    private boolean sourceUsedByGateway = true;

    private VariableMetadata getTargetVariableMetadata() {
        if (getTargetMessageVariable() == null) {
            logger.log(Level.FINE, "TargetMessageVariable is NULL, therefore returning empty variable metadata");
            return null;
        }
        return !isTargetingRequestOrResponse() && isTargetModifiedByGateway()
                ? new VariableMetadata(getTargetMessageVariable(), false, false, null, true, DataType.MESSAGE)
                : null;
    }
}
