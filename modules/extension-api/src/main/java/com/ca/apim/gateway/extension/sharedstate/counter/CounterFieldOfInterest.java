package com.ca.apim.gateway.extension.sharedstate.counter;

/**
 * Create the presentation of counter field of interest instead of numbers
 */
public enum CounterFieldOfInterest {
    // do not reorder these!

    NONE("none"),
    SEC("second"),
    MIN("minute"),
    HOUR("hour"),
    DAY("day"),
    MONTH("month");

    private final String name;

    CounterFieldOfInterest(String name) {
        this.name = name;
    }

    public static CounterFieldOfInterest lookUp(CounterFieldOfInterest fieldOfInterest) {
        return values()[fieldOfInterest.ordinal()];
    }

    public String getName() {
        return name;
    }

}
