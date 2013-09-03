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
public class MemberStrategy implements Serializable {
    @XmlSafe
    public boolean equals(Object obj) {
        if (!(obj instanceof MemberStrategy)) return false;
        MemberStrategy otherone = (MemberStrategy)obj;
        if (otherone.val == this.val) return true;
        return false;
    }

    @XmlSafe
    public int hashCode() {
        return val;
    }

    @XmlSafe
    private MemberStrategy(int val) {
        this.val = val;
    }
    private int val;

    // for serialization purposes only
    @XmlSafe
    public MemberStrategy() {
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
    public static final MemberStrategy MEMBERS_ARE_DN = new MemberStrategy(0);
    public static final MemberStrategy MEMBERS_ARE_LOGIN = new MemberStrategy(1);
    public static final MemberStrategy MEMBERS_ARE_NVPAIR = new MemberStrategy(2);
    public static final MemberStrategy MEMBERS_BY_OU = new MemberStrategy(3);
}
