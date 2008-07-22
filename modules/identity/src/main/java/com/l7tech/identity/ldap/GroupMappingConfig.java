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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMappingConfig)) return false;

        final GroupMappingConfig groupMappingConfig = (GroupMappingConfig) o;

        if (memberAttrName != null ? !memberAttrName.equals(groupMappingConfig.memberAttrName) : groupMappingConfig.memberAttrName != null) return false;
        if (memberStrategy != null ? !memberStrategy.equals(groupMappingConfig.memberStrategy) : groupMappingConfig.memberStrategy != null) return false;
        if (nameAttrName != null ? !nameAttrName.equals(groupMappingConfig.nameAttrName) : groupMappingConfig.nameAttrName != null) return false;
        if (objClass != null ? !objClass.equals(groupMappingConfig.objClass) : groupMappingConfig.objClass != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (objClass != null ? objClass.hashCode() : 0);
        result = 29 * result + (nameAttrName != null ? nameAttrName.hashCode() : 0);
        result = 29 * result + (memberAttrName != null ? memberAttrName.hashCode() : 0);
        result = 29 * result + (memberStrategy != null ? memberStrategy.hashCode() : 0);
        return result;
    }
}