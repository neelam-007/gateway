package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public enum DepthComparisonOperator {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("="),
    GREATER_THAN(">"),
    GREATOR_THAN_OR_EQUAL(">=");

    private String displayValue;

    private DepthComparisonOperator(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public boolean evaluate(int value, int testValue) {
        switch(this) {
            case LESS_THAN:
                return value < testValue;
            case LESS_THAN_OR_EQUAL:
                return value <= testValue;
            case EQUAL:
                return value == testValue;
            case GREATER_THAN:
                return value > testValue;
            case GREATOR_THAN_OR_EQUAL:
                return value >= testValue;
        }

        return false;
    }
}
