package com.l7tech.identity.ldap;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * [class_desc]
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 22, 2004<br/>
 * $Id$<br/>
 *
 */
@XmlSafe
public class PasswdStrategy implements Serializable {

    @XmlSafe
    public boolean equals(Object obj) {
        if (!(obj instanceof PasswdStrategy)) return false;
        PasswdStrategy otherone = (PasswdStrategy)obj;
        if (otherone.val == this.val) return true;
        return false;
    }

    @XmlSafe
    public int hashCode() {
        return val;
    }

    @XmlSafe
    private PasswdStrategy(int val) {
        this.val = val;
    }

    private int val;

    // for serialization purposes only
    @XmlSafe
    public PasswdStrategy() {
    }
    // for serialization purposes only
    @XmlSafe
    public int getVal() {
        return val;
    }
    // for serialization purposes only
    @XmlSafe
    public void setVal(int val) {
        this.val = val;
    }
    public static final PasswdStrategy CLEAR = new PasswdStrategy(0);
    public static final PasswdStrategy HASHED = new PasswdStrategy(1);
}
