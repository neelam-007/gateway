package com.l7tech.adminws.security;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 3, 2003
 *
 * This class is intended to be called by tomcat to enforce digest authentication
 * on the admin service.
 *
 */
public class AuthenticationRealm extends org.apache.catalina.realm.RealmBase {

    public AuthenticationRealm() {
        connector = new GenericAuthenticationConnector();
    }

    /**
     * @return the name of this realm implementation
     */
    protected String getName() {
        return REALM_NAME;
    }

    /**
     * verifies that the passed identity is a member of the group whose
     * name is passed
     *
     * @param principal the user
     * @param groupname the group
     * @return true if user is indeed member of the group
     */
    public boolean hasRole(java.security.Principal principal, String groupname) {
        if (principal instanceof com.l7tech.adminws.security.Principal) {
            Principal localPrincipal = (Principal)principal;
            return connector.userIsMemberOfGroup(localPrincipal.getOid(), groupname);
        }
        return false;
    }

    /**
     * this is only implemented to extend RealmBase
     * it will not be called unless someone tries to do
     * basic authentication
     *
     * @param s not used
     * @return always returns null
     */
    protected String getPassword(String s) {
        // this would only be important for basic authentication
        // the server does not know the user's passwords therefore
        // this method returns null
        return null;
    }

    /**
     * construct a principal based on the user in the internal id provider
     * whose login equals the passed username
     */
    protected java.security.Principal getPrincipal(String login) {
        com.l7tech.identity.User user = connector.findUserByLoginAndRealm(login, null);
        return new com.l7tech.adminws.security.Principal(user);
    }

    /**
     * this is overriden from RealmBase so that we retreive the digest from
     * the internal identity provider MD5(name:realm:passwd) as it is in the database instead of
     * using the real password.
     *
     * this version expects to find the encoded digest in the password property 
     *
     * @return the base64ed value of MD5(name:realm:passwd)
     */
    protected String getDigest(String login, String realm) {
        com.l7tech.identity.User user = connector.findUserByLoginAndRealm(login, realm);
        if (user == null) return "";
        return user.getPassword();
    }

    private static final String REALM_NAME = "L7 Authentication Realm";
    private GenericAuthenticationConnector connector = null;
}
