/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 19, 2009
 * Time: 5:49:08 PM
 */
package com.l7tech.gateway.standardreports;

interface JasperValidLongExpression {
    /**
     * Get the string representation of this variable type, to be placed directly with absoutely no escaping
     * into a java source file, which will be compiled and ran at runtime
     *
     * @return String representing the variable type
     */
    String getVariableString();
}

/**
 * This class represents simple Jasper long variables which are a complete java string
 * e.g. $V{TOTAL}
 */
final class SimpleJavaLongExpression implements JasperValidLongExpression {

    enum PLAIN_STRING_VARIABLE {
        TOTAL("$V{TOTAL}"),
        ROW_OPERATION_TOTAL("$V{ROW_OPERATION_TOTAL}"),
        ROW_SERVICE_TOTAL("$V{ROW_SERVICE_TOTAL}"),
        ROW_REPORT_TOTAL("$V{ROW_REPORT_TOTAL}"),
        SERVICE_AND_OR_OPERATION_TOTAL("$V{SERVICE_AND_OR_OPERATION_TOTAL}"),
        SERVICE_ONLY_TOTAL("$V{SERVICE_ONLY_TOTAL}"),
        GRAND_TOTAL("$V{GRAND_TOTAL}");

        PLAIN_STRING_VARIABLE(String varName) {
            this.varName = varName;
        }

        String getVarName() {
            return varName;
        }

        private final String varName;
    }

    /**
     * This constructor is for all variables which can be represented with a single String
     *
     * @param variable
     */
    SimpleJavaLongExpression(final PLAIN_STRING_VARIABLE variable) {
        this.variable = variable;
    }

    /**
     * Get the string representation of this variable type, to be placed directly with absoutely no escaping
     * into a java source file, which will be compiled and ran at runtime
     *
     * @return String representing the variable type
     */
    final public String getVariableString() {
        return variable.getVarName();
    }

    private final PLAIN_STRING_VARIABLE variable;
}

/**
 * This class represents simple Jasper long variables which are a complete java string plus an index
 * e.g. "$V{COLUMN_REPORT_" + (i + 1) + "}"
 */
final class SimpleIndexJavaLongExpression implements JasperValidLongExpression {

    enum INDEX_MISSING_VARIABLE {
        COLUMN_REPORT_("$V{COLUMN_REPORT_"),
        COLUMN_MAPPING_TOTAL_("$V{COLUMN_MAPPING_TOTAL_");

        INDEX_MISSING_VARIABLE(String varName) {
            this.varName = varName;
        }

        String getVarName() {
            return varName;
        }

        private final String varName;
    }

    /**
     * This constructor is for all variables which can be represented with a single String
     *
     * @param variable enum representing a possible String var name
     * @param index    index to append to create unique variable name
     */
    SimpleIndexJavaLongExpression(final INDEX_MISSING_VARIABLE variable, final int index) {
        this.variable = variable;
        this.index = index;
    }

    /**
     * Get the string representation of this variable type, to be placed directly with absoutely no escaping
     * into a java source file, which will be compiled and ran at runtime
     *
     * @return String representing the variable type
     */
    final public String getVariableString() {
        //"$V{COLUMN_REPORT_" + (i + 1) + "}"
        return variable.getVarName() + index + "}";
    }

    private final INDEX_MISSING_VARIABLE variable;
    private final int index;
}

/**
 * This class represents Jasper long variables expression which use the java tertiary operator
 * e.g. "($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}"
 * and ($V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "} == null || $V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}"
 */
final class TertiaryJavaLongExpression implements JasperValidLongExpression {

    enum TERNARY_STRING_VARIABLE {
        COLUMN_("$V{COLUMN_"),
        COLUMN_OPERATION_("$V{COLUMN_OPERATION_"),
        COLUMN_SERVICE_("$V{COLUMN_SERVICE_"),
        COLUMN_SERVICE_TOTAL_("$V{COLUMN_SERVICE_TOTAL_");

        TERNARY_STRING_VARIABLE(String varName) {
            this.varName = varName;
        }

        String getVarName() {
            return varName;
        }

        private final String varName;
    }

    TertiaryJavaLongExpression(final TERNARY_STRING_VARIABLE variable, final int index) {
        this.variable = variable;
        this.index = index;
    }

    final public String getVariableString() {
        switch (variable) {
            case COLUMN_SERVICE_TOTAL_:
                //"($V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "} == null || $V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}"
                return "(" + variable.getVarName() + index + "} == null || " + variable.getVarName() + index + "}.intValue() == 0)?new Long(0):" + variable.getVarName() + index + "}";
            default:
                //"($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}"
                return "(" + variable.getVarName() + index + "} == null)?new Long(0):" + variable.getVarName() + index + "}";
        }
    }

    private final TERNARY_STRING_VARIABLE variable;
    private final int index;
}

