package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This is used to convert a goid to and from a string for xml transformations.
 *
 * @author Victor Kazakov
 */
public class GoidAdapter extends XmlAdapter<String, Goid> {
    @Override
    public Goid unmarshal(String goid) throws Exception {
        return new Goid(goid);
    }

    @Override
    public String marshal(Goid goid) throws Exception {
        return goid.toString();
    }
}
