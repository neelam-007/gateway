package com.l7tech.identity.ldap;

import com.l7tech.util.XmlSafe;

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
@XmlSafe
public class GroupMappingConfig implements Serializable {
    @XmlSafe
    public String getObjClass() {
        return objClass;
    }

    @XmlSafe
    public void setObjClass(String objClass) {
        this.objClass = objClass;
    }

    @XmlSafe
    public String getNameAttrName() {
        return nameAttrName;
    }

    @XmlSafe
    public void setNameAttrName(String nameAttrName) {
        this.nameAttrName = nameAttrName;
    }

    @XmlSafe
    public String getMemberAttrName() {
        return memberAttrName;
    }

    @XmlSafe
    public void setMemberAttrName(String memberAttrName) {
        this.memberAttrName = memberAttrName;
    }

    @XmlSafe
    public MemberStrategy getMemberStrategy() {
        return memberStrategy;
    }

    @XmlSafe
    public void setMemberStrategy(MemberStrategy memberStrategy) {
        this.memberStrategy = memberStrategy;
    }

    private String objClass;
    private String nameAttrName;
    private String memberAttrName;
    private MemberStrategy memberStrategy;

    @XmlSafe
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

    @XmlSafe
    public int hashCode() {
        int result;
        result = (objClass != null ? objClass.hashCode() : 0);
        result = 29 * result + (nameAttrName != null ? nameAttrName.hashCode() : 0);
        result = 29 * result + (memberAttrName != null ? memberAttrName.hashCode() : 0);
        result = 29 * result + (memberStrategy != null ? memberStrategy.hashCode() : 0);
        return result;
    }
}