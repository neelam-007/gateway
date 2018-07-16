package com.l7tech.external.assertions.gatewaymetrics;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 3/13/13
 * Time: 1:44 PM
 * To change this template use File | Settings | File Templates.
 */
public enum IntervalTimeUnit {

    SECONDS {
        @Override
        public String toString() {
            return "Seconds";
        }
    },

    MINUTES {
        @Override
        public String toString() {
            return "Minutes";
        }
    },

    HOURS {
        @Override
        public String toString() {
            return "Hours";
        }
    },

    DAYS {
        @Override
        public String toString() {
            return "Days";
        }
    };
}