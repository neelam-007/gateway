package com.l7tech.identity.ldap;

import com.l7tech.common.util.HexUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.server.ServerConfig;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides partially configured LdapIdenityProviderConfig to simplify the addition of a new connector.
 * <p/>
 * Once the template is chosen, it can be used to create a new config object.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 */
public class LdapConfigTemplateManager {

    public LdapConfigTemplateManager() {
        synchronized (templates) {
            if (templates.size() < 1)
                populateTemplatesFromFile();
        }
        // buildHardCodedTemplates();
    }

    /**
     * returns the names of the templates
     *
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

    public LdapIdentityProviderConfig[] getTemplates() {
        Collection values = templates.values();
        LdapIdentityProviderConfig[] output = new LdapIdentityProviderConfig[values.size()];
        int i = 0;
        for (Iterator it = values.iterator(); it.hasNext(); i++) {
            output[i] = (LdapIdentityProviderConfig)it.next();
        }
        return output;
    }

    /**
     * get a template, given its name. the actual template is not returned but a new config object
     * based on this template.
     * 
     * @return the config object if found, null if not found
     */
    public LdapIdentityProviderConfig getTemplate(String templateName) throws IOException {
        // clone the template, so it's not affected by whatever the user is doing with it
        LdapIdentityProviderConfig cfg = (LdapIdentityProviderConfig)templates.get(templateName);
        if (cfg == null) return null;
        return new LdapIdentityProviderConfig(cfg);
    }

    private void populateTemplatesFromFile() {
        String rootPath = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_LDAP_TEMPLATES);
        File rootFile = new File(rootPath);
        if (!rootFile.exists()) {
            logger.warning("templates not available!");
            return;
        }
        String[] output = rootFile.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                int length = name.length();
                if (length < 4) return false;
                String extension = name.substring(length - 4, length);
                if (!extension.equals(".xml")) return false;
                return true;
            }
        });
        for (int i = 0; i < output.length; i++) {
            LdapIdentityProviderConfig template = new LdapIdentityProviderConfig();
            template.setName(output[i].substring(0, output[i].length() - 4));

            String fulltemplatefilename = rootPath + File.separatorChar + output[i];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fulltemplatefilename);
            } catch (FileNotFoundException e) {
                logger.log(Level.WARNING, "could not open file " + fulltemplatefilename, e);
                fis = null;
            }
            if (fis != null) {
                String properties = null;
                try {
                    byte[] data = HexUtils.slurpStream(fis, 32768);
                    properties = new String(data);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "cannot slurp file " + fulltemplatefilename, e);
                }
                template.setSerializedProps(properties);
                template.setTemplateName(template.getName());
                templates.put(template.getName(), template);
                logger.finest("added template " + template.getName());
            }
        }
    }

    /*private void buildHardCodedTemplates() {
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
        usrMap.setPasswdType(PasswdStrategy.CLEAR);
        template.setUserMapping(usrMap);
        // add the posixGroup
        GroupMappingConfig grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("memberUid");
        grpConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_LOGIN);
        grpConfig.setNameAttrName("cn");
        grpConfig.setObjClass("posixGroup");
        template.setGroupMapping(grpConfig);
        // add the groupOfUniqueNames
        grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("uniqueMember");
        grpConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_NVPAIR);
        grpConfig.setNameAttrName("cn");
        grpConfig.setObjClass("groupOfUniqueNames");
        template.setGroupMapping(grpConfig);
        // add the organizationalUnit
        grpConfig = new GroupMappingConfig();
        grpConfig.setMemberAttrName("");
        grpConfig.setMemberStrategy(MemberStrategy.MEMBERS_BY_OU);
        grpConfig.setNameAttrName("ou");
        grpConfig.setObjClass("organizationalUnit");
        template.setGroupMapping(grpConfig);

        templates.put(template.getName(), template);

    }*/

    private static final HashMap templates = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());
}
