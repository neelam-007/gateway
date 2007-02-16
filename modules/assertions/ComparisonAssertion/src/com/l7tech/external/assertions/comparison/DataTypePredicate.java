/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import com.l7tech.policy.variable.DataType;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Evaluates whether the lvalue is, or can be converted to, one of the classes described by
 * {@link DataType#getValueClasses()}. 
 * @author alex
 */
public class DataTypePredicate extends Predicate {
    private DataType type;

    public DataTypePredicate() {
    }

    public DataTypePredicate(DataType type) {
        if (type == DataType.UNKNOWN)
            throw new IllegalArgumentException("Can't create a DataTypePredicate with type == UNKNOWN");
        this.type = type;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setNegated(boolean negated) {
        if (negated) throw new IllegalArgumentException("Can't negate a DataTypePredicate");
        super.setNegated(negated);
    }

    public String getSimpleName() {
        return "dataType";
    }

    public String toString() {
        final ResourceBundle res = ComparisonAssertion.resources;
        final String prefix = "dataTypePredicate." + type.getShortName();
        String desc = res.getString(prefix + ".desc");
        String whichArticle = res.getString(prefix + ".article");
        String article = res.getString(whichArticle);
        return MessageFormat.format(res.getString("dataTypePredicate.desc"), res.getString("verb.is"), article, desc);
    }
}
