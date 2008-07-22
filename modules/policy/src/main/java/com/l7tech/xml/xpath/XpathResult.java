/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

/**
 * Represents the result of passing a {@link com.l7tech.xml.xpath.CompiledXpath} to {@link com.l7tech.xml.ElementCursor#getXpathResult(com.l7tech.xml.xpath.CompiledXpath)}.
 */
public interface XpathResult {
    public static final short TYPE_NODESET = 1;
    public static final short TYPE_BOOLEAN = 2;
    public static final short TYPE_NUMBER  = 3;
    public static final short TYPE_STRING  = 4;

    /** A utility result that is Boolean.TRUE. */
    XpathResult RESULT_TRUE = new Truth();

    /** A utility result that is Boolean.FALSE. */
    XpathResult RESULT_FALSE = new Falsehood();

    /** A utility result that is always an empty nodeset. */
    XpathResult RESULT_EMPTY = new XpathResultAdapter() {
        public short getType() {
            return TYPE_NODESET;
        }

        public XpathResultNodeSet getNodeSet() {
            return XpathResultNodeSet.EMPTY_NODESET;
        }
    };

    /**
     * Quickly check if the expression matched anything, depending on the result type.
     *
     * @return true if the result is one of the following: a number, a string, a true boolean, or a non-empty nodeset.
     */
    boolean matches();

    /**
     * Get the type of object represented by these results.
     *
     * @return one of {@link #TYPE_NODESET}, {@link #TYPE_BOOLEAN}, {@link #TYPE_NUMBER},
     *         and {@link #TYPE_STRING}.  Never null.
     */
    short getType();

    /** @return the value of this result as a String, or null if it is not {@link #TYPE_STRING}. */
    String getString();

    /** @return the value of this result as a boolean, or false if it is not {@link #TYPE_BOOLEAN}. */
    boolean getBoolean();

    /** @return the value of this result as a double, or 0 if it is not {@link #TYPE_NUMBER}. */
    double getNumber();

    /** @return the value of this result as a {@link XpathResultNodeSet}, or null if it is not {@link #TYPE_NODESET}. */
    XpathResultNodeSet getNodeSet();

    /** A result with no type that always returns null/zero/false. */
    static abstract class XpathResultAdapter implements XpathResult {
        protected XpathResultAdapter() {}

        public boolean matches() {
            switch (getType()) {
                case TYPE_NODESET:
                    final XpathResultNodeSet nodeSet = getNodeSet();
                    return nodeSet != null && !nodeSet.isEmpty();
                case TYPE_BOOLEAN:
                    return getBoolean();
                case TYPE_NUMBER:
                case TYPE_STRING:
                    return true;
                default:
                    // cant't happen
                    return false;
            }
        }

        public String getString() {
            return null;
        }

        public boolean getBoolean() {
            return false;
        }

        public double getNumber() {
            return 0;
        }

        public XpathResultNodeSet getNodeSet() {
            return null;
        }
    }

    /** A boolean result that is true. */
    static class Truth extends XpathResultAdapter {
        private Truth() {}

        public short getType() {
            return TYPE_BOOLEAN;
        }

        public boolean getBoolean() {
            return true;
        }
    }

    /** A boolean result that is false. */
    static class Falsehood extends XpathResultAdapter {
        private Falsehood() {}

        public short getType() {
            return TYPE_BOOLEAN;
        }

        public boolean getBoolean() {
            return false;
        }
    }
}
