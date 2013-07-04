package com.l7tech.policy;

import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Map;

/**
 * MessageTargetableCustomAssertion Testing class.
 * It's best to keep this class unmodified.
 */
@SuppressWarnings("UnusedDeclaration")
public class TestCustomMessageTargetable implements CustomAssertion, CustomMessageTargetable {
    private static final long serialVersionUID = -4151848438981589828L;

    public static final String NAME = "My Message Targetable Custom Assertion";
    private static final String PROP1_DEFAULT_VALUE = "prop1_value";
    private static final int PROP2_DEFAULT_VALUE = 10;
    private static final boolean PROP3_DEFAULT_VALUE = false;

    private CustomMessageTargetableSupport sourceTarget;
    private CustomMessageTargetableSupport destinationTarget;

    private String prop1;
    private int prop2;
    private boolean prop3;
    private Map prop4;

    public TestCustomMessageTargetable() {
        this(PROP1_DEFAULT_VALUE, PROP2_DEFAULT_VALUE, PROP3_DEFAULT_VALUE, null);
    }

    public TestCustomMessageTargetable(final String prop1, final int prop2, final boolean prop3, final Map prop4) {
        this(prop1, prop2, prop3, prop4,
                new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_REQUEST),
                new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_RESPONSE)
        );
    }

    public TestCustomMessageTargetable(final CustomMessageTargetableSupport sourceTarget, final CustomMessageTargetableSupport destinationTarget) {
        this(PROP1_DEFAULT_VALUE, PROP2_DEFAULT_VALUE, PROP3_DEFAULT_VALUE, null, sourceTarget, destinationTarget);
    }

    public TestCustomMessageTargetable(final String sourceTargetMessage, final String destinationTargetMessage) {
        this(new CustomMessageTargetableSupport(sourceTargetMessage), new CustomMessageTargetableSupport(destinationTargetMessage));
    }

    public TestCustomMessageTargetable(final String prop1, final int prop2, final boolean prop3, final Map prop4,
                                       final CustomMessageTargetableSupport sourceTarget, final CustomMessageTargetableSupport destinationTarget)
    {
        this.prop1 = prop1;
        this.prop2 = prop2;
        this.prop3 = prop3;
        this.prop4 = prop4;

        this.sourceTarget = sourceTarget;
        this.destinationTarget = destinationTarget;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TestCustomMessageTargetable stub = (TestCustomMessageTargetable)obj;
        return prop1.equals(stub.prop1) && prop2 == stub.prop2 && prop3 == stub.prop3 && prop4.equals(stub.prop4) &&
                sourceTarget.equals(stub.sourceTarget) && destinationTarget.equals(stub.destinationTarget);
    }

    public String getProp1() {
        return prop1;
    }
    public void setProp1(final String value) {
        this.prop1 = value;
    }

    public int getProp2() {
        return prop2;
    }
    public void setProp2(final int value) {
        this.prop2 = value;
    }

    public boolean getProp3() {
        return prop3;
    }
    public void setProp3(final boolean value) {
        this.prop3 = value;
    }

    public Map getProp4() {
        return prop4;
    }
    public void setProp4(final Map value) {
        this.prop4 = value;
    }

    public CustomMessageTargetableSupport getSourceTarget() {
        return sourceTarget;
    }

    public void setSourceTarget(final CustomMessageTargetableSupport  sourceTarget) {
        this.sourceTarget = sourceTarget;
    }

    public CustomMessageTargetableSupport getDestinationTarget() {
        return destinationTarget;
    }

    public void setDestinationTarget(final CustomMessageTargetableSupport destinationTarget) {
        this.destinationTarget = destinationTarget;
    }

    @Override
    public String getTargetMessageVariable() {
        return sourceTarget.getTargetMessageVariable();
    }

    @Override
    public void setTargetMessageVariable(final String otherMessageVariable) {
        sourceTarget.setTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return sourceTarget.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return sourceTarget.isTargetModifiedByGateway();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return sourceTarget.getVariablesSet();
    }

    @Override
    public String[] getVariablesUsed() {
        return sourceTarget.getVariablesUsed();
    }
}
