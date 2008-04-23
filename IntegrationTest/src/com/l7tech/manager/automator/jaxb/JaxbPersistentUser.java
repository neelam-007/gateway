package com.l7tech.manager.automator.jaxb;

import com.l7tech.identity.PersistentUser;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 15, 2008
 * Time: 10:25:31 AM
 * This class stores Group membership information required after unmarshalling of an Internal user
 * so that we can look up it's group membership and provide this Set of IdentityProviders when
 * creating the User via IdentityAdmin.
 */
@XmlRootElement
public class JaxbPersistentUser {

    public JaxbPersistentUser(){

    }

    public JaxbPersistentUser(PersistentUser iUser, Set<String> groupNamesSet){
        this.persistentUser = iUser;
        this.groupNamesSet = groupNamesSet;
    }

    public Set<String> getGroupNamesSet() {
        return groupNamesSet;
    }

    public void setGroupNames(Set<String>  groupNames) {
        this.groupNamesSet = groupNames;
    }

    public PersistentUser getPersistentUser() {
        return persistentUser;
    }

    public void setPersistentUser(PersistentUser persistentUser) {
        this.persistentUser = persistentUser;
    }

    private PersistentUser persistentUser;
    @XmlElement
    private Set<String>  groupNamesSet;
}
