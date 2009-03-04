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
    protected TargetMessageType target = TargetMessageType.REQUEST;
    protected String otherTargetMessageVariable;

    public TargetMessageType getTarget() {
        return target;
    }

    public void setTarget(TargetMessageType target) {
        if (target == null) throw new NullPointerException();
        this.target = target;
    }

    public String getOtherTargetMessageVariable() {
        return otherTargetMessageVariable;
    }

    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        this.otherTargetMessageVariable = otherTargetMessageVariable;
    }

    public String getTargetName() {
        switch(target) {
            case REQUEST:
                return "request";
            case RESPONSE:
                return "response";
            case OTHER:
                return "${" + otherTargetMessageVariable + "}";
            default:
                throw new IllegalStateException();
        }
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (otherTargetMessageVariable != null) return new String[] { otherTargetMessageVariable };
        return new String[0];
    }
}
