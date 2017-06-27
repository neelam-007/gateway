package com.l7tech.external.assertions.ldapwrite.server;

import com.l7tech.external.assertions.ldapwrite.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import static javax.naming.directory.DirContext.*;

/**
 * Server side implementation of the LdapWriteAssertion.
 *
 * @see com.l7tech.external.assertions.ldapwrite.LdapWriteAssertion
 */
public class ServerLdapWriteAssertion extends AbstractServerAssertion<LdapWriteAssertion> {

    private static final String CONVERT_TO_BINARY_OPTION = ";converttobinary"; // set to lower case for comparision
                                                                                // in the UI it is: convertToBinary

    private enum AttributeModifyType {
        NONE, ADD, DELETE, REPLACE
    }

    private static final Logger logger = Logger.getLogger(ServerLdapWriteAssertion.class.getName());
    private final IdentityProviderFactory identityProviderFactory;
    final String[] variablesUsed;

    public ServerLdapWriteAssertion(final LdapWriteAssertion assertion,
                                    final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        variablesUsed = assertion.getVariablesUsed();
        identityProviderFactory = applicationContext.getBean("identityProviderFactory", IdentityProviderFactory.class);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext policyEnforcementContext)
            throws IOException, PolicyAssertionException {

        DirContext dirContext = null;

        try {

            final Map<String, Object> varMap = policyEnforcementContext.getVariableMap(variablesUsed, getAudit());
            final LdapIdentityProvider identityProvider = getLdapIdentityProvider();

            final String writeBase;
            // Only proceed with this assertion if the LDAP Identity Provider writable.
            if (identityProvider.getConfig().isWritable()){

                writeBase = identityProvider.getConfig().getWriteBase();

                if (StringUtils.isEmpty(writeBase)) {
                    throw new LdapException("The LDAP Provider selected does not have permission to write to the LDAP Identity Provider.");
                }
            } else {
                throw new LdapException("The LDAP Provider selected does not have permission to write to the LDAP Identity Provider.");
            }

            dirContext = identityProvider.getBrowseContext();

            updateLdapServer(varMap, dirContext, writeBase);

        } catch (FindException | NamingException | LdapException e) {

            policyEnforcementContext.setVariable(assertion.getVariablePrefix() + LdapWriteAssertion.VARIABLE_OUTPUT_SUFFIX_ERROR_MSG,
                    e.toString());
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Failed to perform LDAP operation:" + ExceptionUtils.getMessage(e));

            return AssertionStatus.FALSIFIED;

        } finally {

            ResourceUtils.closeQuietly(dirContext);
        }

        policyEnforcementContext.setVariable(assertion.getVariablePrefix() + LdapWriteAssertion.VARIABLE_OUTPUT_SUFFIX_ERROR_MSG, "Success");

        return AssertionStatus.NONE;
    }


    private LdapIdentityProvider getLdapIdentityProvider() throws FindException {

        // get identity provider
        final IdentityProvider identityProvider = identityProviderFactory.getProvider(assertion.getLdapProviderId());
        if ((identityProvider == null) || !(identityProvider instanceof LdapIdentityProvider)) {
            throw new FindException("The ldap identity provider attached to this LDAP Query assertion cannot be found. " +
                    "It may have been deleted since the assertion was created. " + assertion.getLdapProviderId());
        }
        return (LdapIdentityProvider) identityProvider;

    }


    private void updateLdapServer(final Map<String, Object> varMap,
                                  final DirContext dirContext,
                                  final String writeBase) throws NamingException, IOException, LdapException {

        // REPLACE any context variables in the DN.
        if (StringUtils.isEmpty(assertion.getDn())) {
            throw new LdapException("No DN was provided.");
        }

        final String resolvedDn = ExpandVariables.process(assertion.getDn(), varMap, getAudit());

        if (!isDnPermitted(resolvedDn, writeBase)) {
            throw new LdapException("The LDAP server is not permitted to MODIFY the DN specified:" + resolvedDn);
        }

        switch (assertion.getChangetype()) {

            case ADD:
                addAttributes(varMap, dirContext, resolvedDn, assertion.getAttributeList());
                break;
            case MODIFY:
                modifyAttributes(varMap, dirContext, resolvedDn, assertion.getAttributeList());
                break;
            case DELETE:
                deleteEntry(dirContext, resolvedDn);
                break;
            case MODRDN:
                modRdn(varMap, dirContext, writeBase, resolvedDn, assertion.getAttributeList());
                break;
            default:
                throw new IllegalArgumentException("Invalid LDIF operation.");
        }
    }

    // This function verifies if the DN specified is within the LDAP Identity Provider's writeBase.
    // This is to ensure LDAP updates are permitted for the DN.
    private boolean isDnPermitted(final String dn, final String writeBase) throws InvalidNameException {

        if (StringUtils.isEmpty(dn)) {
            return false;
        }

        final LdapName dnLdapName = new LdapName(dn);
        final LdapName writeBaseLdapName = new LdapName(writeBase);

        return dnLdapName.startsWith(writeBaseLdapName);
    }


    // This function verifies if the RDN specified is within the LDAP Identity Provider's writeBase.
    // The modRdn operation can take a RDN or a fully qualified DN as the target. Much insure in each
    // case that it is permitted.
    private boolean isRdnPermitted(final String rdn, final String writeBase) throws InvalidNameException {

        if (StringUtils.isEmpty(rdn)) {
            return false;
        }

        // Need to distinquish if this DN is a fully qualified DN or a RDN.
        final LdapName ldapName = new LdapName(rdn);
        final List<Rdn> rdns = ldapName.getRdns();
        // Check if it has only 1 RDN
        if (rdns.size() == 1) {
            return rdns.get(0).toString().equals(rdn);
        } else {
            // it is a fully qualified domain
            return isDnPermitted(rdn, writeBase);
        }
    }

    private void addAttributes(final Map<String, Object> varMap,
                               final DirContext dirContext,
                               final String dn,
                               final List<LdifAttribute> attributeList) throws NamingException, LdapException {

        if (attributeList.isEmpty()) {
            throw new LdapException("No attribute type specified for changetype:ADD");
        }

        final HashMap<String, BasicAttribute> attrKeyBasicAttrMap = new HashMap<>();//used to handle multiple values per attribute.
        final BasicAttributes attributes = new BasicAttributes(true); // true specifies to ignore case.
        final BasicAttribute objectClass = new BasicAttribute("objectClass");

        String attrKey;
        String attrValue;

        for (final LdifAttribute ldifAttribute : attributeList) {

            attrKey = ldifAttribute.getKey();
            attrValue = ldifAttribute.getValue();

            // resolve any context variables that may exist:
            final String resolvedVal = ExpandVariables.process(attrValue, varMap, getAudit());

            if ("objectClass".equalsIgnoreCase(attrKey)) {
                objectClass.add(resolvedVal);

            } else {

                // check if value needs to be converted to binary.
                if (attrKey.toLowerCase(Locale.ENGLISH).endsWith(CONVERT_TO_BINARY_OPTION)) {

                    attrKey = attrKey.substring(0, attrKey.toLowerCase(Locale.ENGLISH).lastIndexOf(CONVERT_TO_BINARY_OPTION));
                    final byte[] buf = HexUtils.decodeBase64(resolvedVal);
                    attributes.put(new BasicAttribute(attrKey, buf));

                } else {

                    // add attribute.  Must handle multivalued attributes. e.g description: line1; description: line2;
                    // Check map if BasicAttribute already exists.  If so, attribute was created already.  Re-use the
                    // BasicAttribute object and add next value.
                    final BasicAttribute basicAttrFromMap = attrKeyBasicAttrMap.get(attrKey.toLowerCase(Locale.ENGLISH));

                    if (basicAttrFromMap == null) {
                        final BasicAttribute basicAttribute = new BasicAttribute(attrKey, resolvedVal);
                        attributes.put(basicAttribute);
                        attrKeyBasicAttrMap.put(attrKey.toLowerCase(Locale.ENGLISH), basicAttribute);

                    } else {
                        // BasicAttribute for the specified attribute key was previously created with another value.
                        // Add this value to it.
                        basicAttrFromMap.add(resolvedVal);
                    }
                }
            }
        }

        if (objectClass.size() > 0) {
            attributes.put(objectClass);
        }

        DirContext result = null;
        try {

            result = dirContext.createSubcontext(dn, attributes);

        } finally {

            // Required to remove reference to the BasicAttribute. If not, it could prevent connection
            // from returning to the connection pool to be reused.
            final NamingEnumeration namingEnumeration = attributes.getAll();
            namingEnumeration.close();

            if (result != null) {
                result.close(); // Required to remove reference from DirContext.  Otherwise it will prevent
                // connection from returning to the connection pool to be reused.
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Added entry {0}", dn);
        }
    }


    private void modifyAttributes(final Map<String, Object> varMap,
                                  final DirContext dirContext,
                                  final String dn,
                                  final List<LdifAttribute> attributeList) throws NamingException, IOException, LdapException {

        if (attributeList.isEmpty()) {
            throw new LdapException("No attribute type specified for changetype:MODIFY");
        }

        final List<LdifModificationItem> modificationList = new ArrayList<>();

        AttributeModifyType attributeType = AttributeModifyType.NONE;
        String attrKey;
        String attrValue;
        String attributeName = "";
        boolean bExpectingValue = false;

        for (int i = 0; i < attributeList.size(); i++) {

            attrKey = attributeList.get(i).getKey();
            attrValue = attributeList.get(i).getValue();

            // Determine if the key is a attribute_type: add,replace, or delete.
            if ("add".equalsIgnoreCase(attrKey)) {
                attributeType = AttributeModifyType.ADD;
                attributeName = attrValue;
                bExpectingValue = true;

            } else if ("replace".equalsIgnoreCase(attrKey)) {
                attributeType = AttributeModifyType.REPLACE;
                attributeName = attrValue;
                bExpectingValue = true;

            } else if ("delete".equalsIgnoreCase(attrKey)) {
                attributeType = AttributeModifyType.DELETE;
                attributeName = attrValue;
                bExpectingValue = true;

                // look ahead to see if there is a value to delete
                // If the next line is a dash or if there are no other lines, just delete.
                if (((i + 1) < attributeList.size()) && (attributeList.get(i + 1).getKey().equals("-")) ||
                        ((i + 1) == attributeList.size())) {

                    // no value specified, so just delete the attribute.
                    modificationList.add(new LdifModificationItem(REMOVE_ATTRIBUTE, new LdifAttribute(attributeName, ""), false));
                    bExpectingValue = false;
                }
            } else if ("-".equals(attrKey)) {

                if (bExpectingValue) {
                    throw new LdapException("Invalid LDIF syntax");
                }
            } else {

                boolean binaryConversion = false;
                // check if value needs to be converted to binary.
                if (attrKey.toLowerCase(Locale.ENGLISH).endsWith(CONVERT_TO_BINARY_OPTION)) {
                    attrKey = attrKey.substring(0, attrKey.toLowerCase(Locale.ENGLISH).lastIndexOf(CONVERT_TO_BINARY_OPTION));
                    binaryConversion = true;
                }
                if (attributeName.equalsIgnoreCase(attrKey)) {

                    if (attributeType == AttributeModifyType.ADD) {
                        modificationList.add(new LdifModificationItem(ADD_ATTRIBUTE, new LdifAttribute(attributeName, attrValue), binaryConversion));
                        bExpectingValue = false;
                    } else if (attributeType == AttributeModifyType.REPLACE) {
                        modificationList.add(new LdifModificationItem(REPLACE_ATTRIBUTE, new LdifAttribute(attributeName, attrValue), binaryConversion));
                        bExpectingValue = false;
                    } else if (attributeType == AttributeModifyType.DELETE) {
                        modificationList.add(new LdifModificationItem(REMOVE_ATTRIBUTE, new LdifAttribute(attributeName, attrValue), false));
                        bExpectingValue = false;
                    }
                } else {
                    throw new LdapException("Invalid LDIF syntax");
                }
            }
        }

        // combine all of the arraylists to the modificationItems[] for input into the DirContext modifyAttributes() API.
        // Must know the total number of modifications up front before creating the ModificationItem[]
        final ModificationItem[] modificationItems;

        modificationItems = modificationListToModificationItems(varMap, modificationList);

        dirContext.modifyAttributes(dn, modificationItems);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Successfully modified entry: {0}", dn);
        }
    }


    private void deleteEntry(final DirContext dirContext, final String dn) throws NamingException, IOException {

        dirContext.destroySubcontext(dn);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Successfully deleted entry: {0}", dn);
        }
    }

    private void modRdn(final Map<String, Object> varMap,
                        final DirContext dirContext,
                        final String writeBase,
                        final String oldDn,
                        final List<LdifAttribute> attributeList) throws NamingException, IOException, LdapException {

        if (attributeList.isEmpty()) {
            throw new LdapException("No attribute type specified for changetype:MODRDN");
        }
        String newDn;

        // must have 1 or 2 lines:
        //    newrdn: val
        //    deleteoldrdn: 1 or 0 (not supported by this assertion but valid LDIF syntax)

        // array length of 2 or 4, every 2 elements represents an attribute/value pair
        if ((attributeList.size() == 1) || (attributeList.size() == 2)) {

            final LdifAttribute ldifAttribute = attributeList.get(0);

            if ("newrdn".equalsIgnoreCase(ldifAttribute.getKey())) {

                // resolve context variable
                newDn = ExpandVariables.process(ldifAttribute.getValue(), varMap, getAudit());

                // check if the Rdn it is permitted.
                if (isRdnPermitted(newDn, writeBase)) {
                    dirContext.rename(oldDn, newDn);
                } else {
                    throw new LdapException("The LDAP server is not permitted to Modrdn to the new DN specified:" + newDn);
                }

            } else {
                throw new LdapException("Invalid LDIF syntax for MODRDN operation.");
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Successfully modrdn the entry:{0} to:{1}", new String[]{oldDn, newDn});
            }
        } else {
            throw new LdapException("Invalid LDIF syntax for MODRDN operation.");
        }
    }

    // This method returns an array of ModificationItem given a list of LdifModificationItems.
    // The ModificationItem[] is required by the DirContext() JNDI API to perform modifications.
    private ModificationItem[] modificationListToModificationItems(final Map<String, Object> varMap,
                                                                   final List<LdifModificationItem> modificationList) throws UnsupportedEncodingException {

        ModificationItem[] modificationItems = new ModificationItem[modificationList.size()];

        LdifAttribute ldifAttribute;
        int index = 0;
        for (final LdifModificationItem ldifModificationItem : modificationList) {

            BasicAttribute basicAttribute;

            ldifAttribute = ldifModificationItem.getLdifAttribute();
            if (ldifAttribute.getValue().isEmpty()) {
                // handle the case when the value is empty. e.g. REMOVE_ATTRIBUTE
                basicAttribute = new BasicAttribute(ldifModificationItem.getLdifAttribute().getKey());
            } else {
                // Resolve any context variables in the value.
                final String resolvedVal = ExpandVariables.process(ldifAttribute.getValue(), varMap, getAudit());

                if (ldifModificationItem.isBinaryEnabled()) {
                    // binary
                    final byte[] buf = HexUtils.decodeBase64(resolvedVal);
                    basicAttribute = new BasicAttribute(ldifAttribute.getKey(), buf);
                } else {
                    // string
                    basicAttribute = new BasicAttribute(ldifAttribute.getKey(), resolvedVal);
                }
            }

            modificationItems[index] = new ModificationItem(ldifModificationItem.getAttributeType(), basicAttribute);
            index++;
        }

        return modificationItems;
    }


    //    This class represents a LDIF Modification Item.
//    Created by chaja24 on 3/28/2017.
    private class LdifModificationItem {

        final private int attributeType;
        final private LdifAttribute ldifAttribute;
        final private boolean binaryEnabled;

        public LdifModificationItem(int attributeType, LdifAttribute ldifAttribute, boolean binaryEnabled) {
            this.attributeType = attributeType;
            this.ldifAttribute = ldifAttribute;
            this.binaryEnabled = binaryEnabled;
        }

        public LdifAttribute getLdifAttribute() {
            return ldifAttribute;
        }

        public boolean isBinaryEnabled() {
            return binaryEnabled;
        }

        public int getAttributeType() {
            return attributeType;
        }

    }

    /**
     * LdapException is used to represent exceptions on input data used in the creation of LDIF requests.
     * Created by chaja24 on 3/20/2017.
     */
    private class LdapException extends Exception {

        public LdapException(String message) {
            super(message);
        }
    }

}
