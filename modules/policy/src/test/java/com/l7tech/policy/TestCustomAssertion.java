package com.l7tech.policy;

import com.l7tech.policy.assertion.ext.CustomAssertion;

import java.util.Map;

/** This class is for testing custom assertion serialization compatibility with 2.1.  Do not change this class! */
class TestCustomAssertion implements CustomAssertion {
    private int int1;
    private String String1;
    private Map map1;

    private static final long serialVersionUID = -6253600978668874984L;

    public TestCustomAssertion(int int1, String string1, Map map1) {
        this.int1 = int1;
        String1 = string1;
        this.map1 = map1;
    }

    public String getName() {
        return "Test Assertion";
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public String getString1() {
        return String1;
    }

    public void setString1(String string1) {
        String1 = string1;
    }

    public Map getMap1() {
        return map1;
    }

    public void setMap1(Map map1) {
        this.map1 = map1;
    }
}
