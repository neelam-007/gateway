package com.l7tech.adminws.service;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 * Admin Web service for all that has to do with identities
 */
public class Identity {
    public Identity(){
    }
    // identity provider config
    public Header[] findAlllIdentityProviderConfig(){

        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
    public Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize){
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey( long oid ){
        return null;
    }
    public long saveIdentityProviderConfig( IdentityProviderConfig identityProviderConfig ){
        return 0;
    }
    public void deleteIdentityProviderConfig(long oid){
    }
    // user manager
    public User findUserByPrimaryKey(long identityProviderConfigId, long userId){
        return null;
    }
    public void deleteUser(long identityProviderConfigId, long userId){
    }
    public long saveUser(long identityProviderConfigId, User user){
        return 0;
    }
    public Header[] findAllUsers(long identityProviderConfigId){
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
    public Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize){
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
    // group manager
    public Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId){
        return null;
    }
    public void deleteGroup(long identityProviderConfigId, long groupId){
    }
    public long saveGroup(long identityProviderConfigId, Group group){
        return 0;
    }
    public Header[] findAllGroups(long identityProviderConfigId){
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
    public Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize){
        // test
        Header[] res = new Header[3];
        res[0] = new Header(321, "blahtype1", "blahname1");
        res[1] = new Header(322, "blahtype2", "blahname2");
        res[2] = new Header(322, "blahtype3", "blahname3");
        return res;
        //
    }
}
