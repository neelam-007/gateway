package com.l7tech.identity.ldap;

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
public class MemberStrategy {
    public boolean equals(Object obj) {
        if (!(obj instanceof MemberStrategy)) return false;
        MemberStrategy otherone = (MemberStrategy)obj;
        if (otherone.val == this.val) return true;
        return false;
    }

    public int hashCode() {
        return val;
    }
    private MemberStrategy(int val) {
        this.val = val;
    }
    private int val;

    // for serialization purposes only
    public MemberStrategy() {
    }
    // for serialization purposes only
    public int getVal() {
        return val;
    }
    // for serialization purposes only
    public void setVal(int val) {
        this.val = val;
    }
    public static final MemberStrategy MEMBERS_ARE_DN = new MemberStrategy(0);
    public static final MemberStrategy MEMBERS_ARE_LOGIN = new MemberStrategy(1);
    public static final MemberStrategy MEMBERS_ARE_NVPAIR = new MemberStrategy(2);
    public static final MemberStrategy MEMBERS_BY_OU = new MemberStrategy(3);
}
