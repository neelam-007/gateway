package com.l7tech.external.assertions.ldapwrite;

import java.io.Serializable;

/**
 * This class was created to represent the Attribute name/value pair and is used as one the assertion attributes.
 * This class is used instead of the Pair class to overcome the limitation of Pair class not being able to be
 * serialized when the assertion is saved/loaded.
 * <p/>
 * Created by chaja24 on 3/15/2017.
 */
public class LdifAttribute implements Serializable {

    private String key;
    private String value;


    public LdifAttribute() {
        // create emtpy LdifAttribute.  Values can be set later.
    }

    public LdifAttribute(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    // used to un-serialize the attribute when loading the policy
    public void setKey(final String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    // used to un-serialize the attribute when loading the policy
    public void setValue(final String value) {
        this.value = value;
    }

}
