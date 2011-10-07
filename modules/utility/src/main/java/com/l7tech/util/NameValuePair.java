package com.l7tech.util;

/**
 * A non-generic MutablePair that holds two String values.
 */
public class NameValuePair extends MutablePair<String, String> {
    public NameValuePair() {
    }

    public NameValuePair(String left, String right) {
        super(left, right);
    }

    @Override
    public String getKey() {
        return super.getKey();
    }

    @Override
    public String setKey(String key) {
        return super.setKey(key);
    }

    @Override
    public String getValue() {
        return super.getValue();
    }

    @Override
    public String setValue(String value) {
        return super.setValue(value);
    }
}
