package com.l7tech.identity.ldap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.io.IOException;

/**
 * Provides partially configured LdapIdenityProviderConfig to simplify the addition of a new connector.
 *
 * Once the template is chosen, it can be used to create a new config object.
 *
 * Todo, read and persist those templates to database, allow to add new templates
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapConfigTemplateManager {

    public LdapConfigTemplateManager() {
        buildHardCodedTemplates();
    }

    /**
     * returns the names of the templates
     * @return an array of template names that can be used in getTemplate()
     */
    public String[] getTemplateNames() {
        Set keys = templates.keySet();
        String[] output = new String[keys.size()];
        int i = 0;
        for (Iterator it = keys.iterator(); it.hasNext(); i++) {
            output[i] = (String)it.next();
        }
        return output;
    }

    /**
     * get a template, given its name
     * @return the config object if found, null if not found
     */
    public final LdapIdentityProviderConfig getTemplate(String templateName) throws IOException {
        return new LdapIdentityProviderConfig((LdapIdentityProviderConfig)templates.get(templateName));
    }

    private void buildHardCodedTemplates() {
        // build the standard LDAP template
        LdapIdentityProviderConfig template = new LdapIdentityProviderConfig();
        template.setName("Standard LDAP Template");
        template.setLdapUrl("ldap_url");
        template.setSearchBase("search_base");
        template.setDescription("template configuration for standard ldap connector");
        // add the inetOrgPerson type
        UserMappingConfig usrMap = new UserMappingConfig();
        usrMap.setEmailNameAttrName("mail");
        usrMap.setFirstNameAttrName("givenName");
        usrMap.setLastNameAttrName("sn");
        usrMap.setLoginAttrName("uid");
        usrMap.setNameAttrName("cn");
        usrMap.setObjClass("inetOrgPerson");
        usrMap.setPasswdAttrName("userPassword");
        usrMap.setPasswdType(UserMappingConfig.CLEAR);
        template.setUserMapping(usrMap);
        // add the posixGroup
        GroupMappingConfig grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("memberUid");
        grpConfig.setMemberStrategy(GroupMappingConfig.MEMBERS_ARE_LOGIN);
        grpConfig.setNameAttrName("cn");
        grpConfig.setObjClass("posixGroup");
        template.setGroupMapping(grpConfig);
        // add the groupOfUniqueNames
        grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("uniqueMember");
        grpConfig.setMemberStrategy(GroupMappingConfig.MEMBERS_ARE_NVPAIR);
        grpConfig.setNameAttrName("cn");
        grpConfig.setObjClass("groupOfUniqueNames");
        template.setGroupMapping(grpConfig);
        // add the organizationalUnit
        grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("");
        grpConfig.setMemberStrategy(GroupMappingConfig.MEMBERS_BY_OU);
        grpConfig.setNameAttrName("ou");
        grpConfig.setObjClass("organizationalUnit");
        template.setGroupMapping(grpConfig);

        templates.put(template.getName(), template);
    }

    private final HashMap templates = new HashMap();
}
