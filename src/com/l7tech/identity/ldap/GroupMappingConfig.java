package com.l7tech.identity.ldap;

import java.io.Serializable;

/**
 * Tells the ldap provider how to use a specific object class to describe a group.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class GroupMappingConfig implements Serializable {
    public String getObjClass() {
        return objClass;
    }

    public void setObjClass(String objClass) {
        this.objClass = objClass;
    }

    public String getNameAttrName() {
        return nameAttrName;
    }

    public void setNameAttrName(String nameAttrName) {
        this.nameAttrName = nameAttrName;
    }

    public String getMemberAttrName() {
        return memberAttrName;
    }

    public void setMemberAttrName(String memberAttrName) {
        this.memberAttrName = memberAttrName;
    }

    public MemberStrategy getMemberStrategy() {
        return memberStrategy;
    }

    public void setMemberStrategy(MemberStrategy memberStrategy) {
        this.memberStrategy = memberStrategy;
    }

    private String objClass;
    private String nameAttrName;
    private String memberAttrName;
    private MemberStrategy memberStrategy;
}