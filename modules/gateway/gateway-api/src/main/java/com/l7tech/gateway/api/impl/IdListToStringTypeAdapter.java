package com.l7tech.gateway.api.impl;

import com.l7tech.util.Functions;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Arrays;
import java.util.List;

/**
 * This marshals and unmarshals a list of string to a comma separated String.
 */
public class IdListToStringTypeAdapter extends XmlAdapter<String, List<String>> {
    @Override
    public String marshal(final List<String> list) throws Exception {
        return (list == null || list.isEmpty()) ? null : Functions.reduce(list, "", new Functions.Binary<String, String, String>() {
            @Override
            public String call(String s, String s2) {
                return s.concat(",").concat(s2);
            }
        }).substring(1);
    }

    @Override
    public List<String> unmarshal(final String stringList) throws Exception {
        return (stringList == null || stringList.isEmpty()) ? null : Arrays.asList(stringList.split(","));
    }
}
