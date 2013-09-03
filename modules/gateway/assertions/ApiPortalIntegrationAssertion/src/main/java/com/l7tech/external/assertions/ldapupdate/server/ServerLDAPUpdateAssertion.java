package com.l7tech.external.assertions.ldapupdate.server;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.external.assertions.ldapupdate.LDAPUpdateAssertion;
import com.l7tech.external.assertions.ldapupdate.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.identity.ldap.LdapUtils;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.*;
import org.springframework.beans.factory.BeanFactory;

import javax.naming.directory.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * Performs LDAP Update operations
 *
 * @author rraquepo
 */
public class ServerLDAPUpdateAssertion extends AbstractServerAssertion<LDAPUpdateAssertion> {

    //- PUBLIC
    public ServerLDAPUpdateAssertion(final LDAPUpdateAssertion assertion,
                                     final BeanFactory context, final JAXBResourceUnmarshaller unmarshaller) {
        super(assertion);
        this.identityProviderFactory = context.getBean("identityProviderFactory", IdentityProviderFactory.class);
        this.unmarshaller = unmarshaller;
        final Timer timer = context.getBean("managedBackgroundTimer", Timer.class);
        timer.schedule(cacheCleanupTask, 5393L, cacheCleanupInterval);
    }

    public ServerLDAPUpdateAssertion(final LDAPUpdateAssertion assertion,
                                     final BeanFactory context) throws JAXBException {
        this(assertion, context, DefaultJAXBResourceUnmarshaller.getInstance());
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        providerName = ExpandVariables.process("${" + LDAPUpdateAssertion.PROVIDER_NAME + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String resourceXml = ExpandVariables.process("${" + LDAPUpdateAssertion.RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String operationVar = ExpandVariables.process("${" + LDAPUpdateAssertion.OPERATION + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String injectionProtectionVar = ExpandVariables.process("${" + LDAPUpdateAssertion.INJECTION_PROTECTION + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        if ("false".equalsIgnoreCase(injectionProtectionVar)) {
            injectionProtection = false;
        }

        boolean status = true;
        try {
            if ((null == providerName || EMPTY.equals(providerName)) && !CLEAR_CACHE.equalsIgnoreCase(operationVar)) {
                String errorMsg = LDAPUpdateAssertion.PROVIDER_NAME + " was empty";
                logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
                setContextVariables(context, 500, errorMsg, null);
                return AssertionStatus.FALSIFIED;
            }
            if (null == operationVar || EMPTY.equals(operationVar)) {
                String errorMsg = LDAPUpdateAssertion.OPERATION + " was empty";
                logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
                setContextVariables(context, 500, errorMsg, null);
                return AssertionStatus.FALSIFIED;
            }
            responseBody = new StringBuffer();
            if (MANAGE.equalsIgnoreCase(operationVar)) {
                if (null == resourceXml || "".equals(resourceXml)) {
                    String errorMsg = LDAPUpdateAssertion.RESOURCE + " was empty";
                    logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
                    setContextVariables(context, 500, errorMsg, null);
                    //the message body is empty, so the unmarshall will just fail, so return as falsified instead
                    return AssertionStatus.FALSIFIED;
                }

                Object obj = unmarshaller.unmarshal(resourceXml, Resource.class);
                if (obj instanceof LDAPOperation) {
                    final LDAPOperation operation = (LDAPOperation) obj;
                    status = performLDAPOperation(operation);
                } else if (obj instanceof LDAPOperations) {
                    final boolean enableMultipleOperation = SyspropUtil.getBoolean("com.l7tech.external.assertions.ldapupdate.enableMultipleOperation", false);
                    if (!enableMultipleOperation) {
                        String errorMsg = "Multiple operations not allowed.";
                        responseBody.append(errorMsg);
                        logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
                        status = false;
                    } else {
                        final LDAPOperations operations = (LDAPOperations) obj;
                        for (LDAPOperation operation : operations.getOperations()) {
                            boolean status_running = performLDAPOperation(operation);
                            if (status_running == false) {
                                status = false;
                                //do we want to stop now on first error? we don't really have an LDAP transaction
                                //but we should probably put a config that explicitly tell's us to stop processing the next operation
                            }
                        }
                    }
                }
            } else if (VALIDATE_PROVIDER.equalsIgnoreCase(operationVar)) {
                //build the provider xml response
                status = validateProvider();
            } else if (CLEAR_CACHE.equalsIgnoreCase(operationVar)) {
                //let us manually clear the cache
                try {
                    logger.log(Level.FINE, "cache size before clearing - " + cacheProviderName.size());
                    cacheProviderName.clear();
                    logger.log(Level.FINE, "clearing of cache complete.");
                } catch (Exception e) {
                    logger.log(Level.FINE, "Problem clearing cacheProviderName :" + ExceptionUtils.getMessage(e), e);
                }
            } else {
                String errorMsg = LDAPUpdateAssertion.OPERATION + " operation was invalid";
                logAndAudit(AssertionMessages.EXCEPTION_INFO, errorMsg);
                setContextVariables(context, 500, errorMsg, null);
                return AssertionStatus.FALSIFIED;
            }
        } catch (JAXBException e) {
            String errorMsg = "Error unmarshalling to resource:" + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(e));
            responseBody.append(errorMsg);
            status = false;
        } catch (Exception e) {
            String errorMsg = "Error processing request:" + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(e));
            responseBody.append(errorMsg);
            status = false;
        }
        if (status == true) {
            setContextVariables(context, 200, SUCCESS, responseBody.toString());
            return AssertionStatus.NONE;
        } else {
            setContextVariables(context, 500, responseBody.toString(), null);
            return AssertionStatus.FALSIFIED;
        }
    }

    /**
     * protected access to cache count
     */
    protected int getCacheCount() {
        return cacheProviderName.size();
    }

    private boolean performLDAPOperation(LDAPOperation operation) {
        final String mode = operation.getOperation();
        if (operation.getOperation() == null || "".equals(operation.getOperation())) {
            String errorMsg = "operation is null";
            responseBody.append(errorMsg);
            responseBody.append("\n");
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
            return false;
        }
        //let's stop now if not a valid mode, since getting the browser context is an expensive operation
        if (!CREATE.equalsIgnoreCase(mode) && !UPDATE.equalsIgnoreCase(mode) && !DELETE.equalsIgnoreCase(mode)) {
            String errorMsg = "Unsupported operation - " + mode;
            responseBody.append(errorMsg);
            responseBody.append("\n");
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
            return false;
        }
        if (DELETE.equalsIgnoreCase(mode)) {
            final boolean enableDelete = SyspropUtil.getBoolean("com.l7tech.external.assertions.ldapupdate.enableDelete", false);
            if (!enableDelete) {
                String errorMsg = "Unauthorized operation - " + mode;
                responseBody.append(errorMsg);
                responseBody.append("\n");
                logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg});
                return false;
            }
        }
        DirContext dirContext = null;
        try {
            final IdentityProvider provider = getIdProvider();
            if (provider instanceof LdapIdentityProvider) {
                final LdapIdentityProvider identityProvider = (LdapIdentityProvider) provider;
                dirContext = identityProvider.getBrowseContext();

                String dn = operation.getDn();
                final String filterExpression;
                if (injectionProtection) {
                    filterExpression = LdapUtils.filterEscape(dn);
                } else {
                    filterExpression = dn;
                }
                logAndAudit(AssertionMessages.LDAP_QUERY_SEARCH_FILTER, filterExpression);
                if (CREATE.equalsIgnoreCase(mode)) {
                    Attributes attributes = new BasicAttributes(true);
                    if (operation.getAttributes() != null) {
                        for (OperationAttribute attribute : operation.getAttributes().getAttributes()) {
                            if (attribute.getValues() != null && attribute.getValues().getValues() != null && attribute.getValues().getValues().size() > 0) {
                                Attribute attr = new BasicAttribute(attribute.getName());
                                for (String value : attribute.getValues().getValues()) {
                                    attr.add(handleFormat(attribute.getFormat(), value));
                                }
                                attributes.put(attr);
                            } else {
                                attributes.put(attribute.getName(), handleFormat(attribute));
                            }
                        }
                    }
                    dirContext.createSubcontext(filterExpression, attributes);
                } else if (UPDATE.equalsIgnoreCase(mode)) {
                    List<ModificationItem> modsList = new ArrayList<ModificationItem>();
                    if (operation.getAttributes() != null) {
                        for (OperationAttribute attribute : operation.getAttributes().getAttributes()) {
                            int modType = DirContext.REPLACE_ATTRIBUTE;
                            BasicAttribute ldapAttribute;
                            if (attribute.getValues() != null && attribute.getValues().getValues() != null && attribute.getValues().getValues().size() > 0) {
                                ldapAttribute = new BasicAttribute(attribute.getName());
                                for (String value : attribute.getValues().getValues()) {
                                    ldapAttribute.add(handleFormat(attribute.getFormat(), value));
                                }
                            } else {
                                ldapAttribute = new BasicAttribute(attribute.getName(), handleFormat(attribute));
                            }
                            if (attribute.getAction() != null && "delete".equalsIgnoreCase(attribute.getAction())) {
                                modType = DirContext.REMOVE_ATTRIBUTE;
                            } else if (attribute.getAction() != null && "add".equalsIgnoreCase(attribute.getAction())) {
                                modType = DirContext.ADD_ATTRIBUTE;
                            }
                            ModificationItem modItem = new ModificationItem(modType, ldapAttribute);
                            modsList.add(modItem);
                        }
                    }
                    if (modsList.size() > 0) {
                        ModificationItem[] mods = modsList.toArray(new ModificationItem[modsList.size()]);
                        dirContext.modifyAttributes(filterExpression, mods);
                    }
                } else if (DELETE.equalsIgnoreCase(mode)) {
                    dirContext.destroySubcontext(filterExpression);
                }
            } else {
                logger.log(Level.FINE, "Not a valid LdapIdentityProvider");
                return false;
            }
        } catch (FindException e) {
            String errorMsg = "Unable to Find LDAP Provider:" + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(e));
            responseBody.append(errorMsg);
            responseBody.append("\n");
            return false;
        } catch (Exception e) {
            String errorMsg = "Error performing LDAP Operation:" + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{}, ExceptionUtils.getDebugException(e));
            responseBody.append(errorMsg);
            responseBody.append("\n");
            return false;
        } finally {
            ResourceUtils.closeQuietly(dirContext);
        }
        return true;
    }

    private boolean validateProvider() {
        try {
            IdentityProvider identityProvider = getIdProvider();
            responseBody.append("<l7:LDAPValidate  xmlns:l7=\"" + JAXBResourceUnmarshaller.NAMESPACE + "\">\n");
            responseBody.append("<l7:Name>");
            responseBody.append(identityProvider.getConfig().getName());
            responseBody.append("</l7:Name>\n");
            try {
                //it will be helpful if we can get the templateName but its currently protected
                //--identityProvider.getConfig().getProperty("originalTemplateName");
                //so we'll just serialize the props
                String serializedProps = identityProvider.getConfig().getSerializedProps();
                if (serializedProps != null) {
                    ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
                    SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in).build();
                    Map mapProps = (Map) decoder.readObject();
                    String templateName = (String) mapProps.get("originalTemplateName");
                    if (templateName != null) {
                        responseBody.append("<l7:TemplateName>");
                        responseBody.append(templateName);
                        responseBody.append("</l7:TemplateName>\n");
                    }
                    String ldapsearchbase = (String) mapProps.get("ldapsearchbase");
                    if (ldapsearchbase != null) {
                        responseBody.append("<l7:LDAPSearchBase>");
                        responseBody.append(ldapsearchbase);
                        responseBody.append("</l7:LDAPSearchBase>\n");
                    }
                    Map<String, String> grpmappings = (Map) mapProps.get("grpmappings");
                    if (grpmappings != null) {
                        PoolByteArrayOutputStream output = null;
                        java.beans.XMLEncoder encoder = null;
                        try {
                            output = new PoolByteArrayOutputStream();
                            encoder = new XMLEncoder(new NonCloseableOutputStream(output));
                            encoder.writeObject(grpmappings);
                            encoder.close();
                            encoder = null;
                            String groupSettingsPropsXml = output.toString(Charsets.UTF8);
                            responseBody.append("<l7:GroupMappings><![CDATA[");
                            responseBody.append(groupSettingsPropsXml.toString());
                            responseBody.append("]]></l7:GroupMappings>\n");
                        } finally {
                            if (encoder != null) encoder.close();
                            ResourceUtils.closeQuietly(output);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Unable to get extended ldap properties." + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            responseBody.append("<l7:Type>");
            responseBody.append(identityProvider.getConfig().type().description());
            responseBody.append("</l7:Type>\n");
            responseBody.append("<l7:TypeVal>");
            responseBody.append(identityProvider.getConfig().getTypeVal());
            responseBody.append("</l7:TypeVal>\n");
            responseBody.append("</l7:LDAPValidate>");
            return true;
        } catch (FindException e) {
            String errorMsg = "Unable to Find LDAP Provider:" + ExceptionUtils.getMessage(e);
            logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(e));
            responseBody.append(errorMsg);
            responseBody.append("\n");
            return false;
        }
    }

    private void setContextVariables(final PolicyEnforcementContext context, final int responseStatus, final String message, final String xml) {
        context.setVariable(LDAPUpdateAssertion.RESPONSE_RESOURCE, xml);
        context.setVariable(LDAPUpdateAssertion.RESPONSE_STATUS, responseStatus);
        context.setVariable(LDAPUpdateAssertion.RESPONSE_DETAIL, message);
    }

    private IdentityProvider getIdProvider() throws FindException {
        // get identity provider
        Goid providerOid = cacheProviderName.get(providerName);

        if (providerOid == null) {
            //not yet in cache, so find it!
            for (IdentityProvider identityProvider : identityProviderFactory.findAllIdentityProviders()) {
                logger.log(Level.FINE, "Checking " + identityProvider.getConfig().getName() + "," + identityProvider.getConfig().getId() + "," + identityProvider.getConfig().getGoid());
                if (identityProvider.getConfig().getName().trim().equalsIgnoreCase(providerName.trim())) {
                    providerOid = identityProvider.getConfig().getGoid();
                    cacheProviderName.put(providerName, providerOid);
                    return identityProvider;
                }
            }
            throw new FindException("The ldap identity provider referenced from this assertion cannot be found - " + providerName + ".");
        } else {
            IdentityProvider output = identityProviderFactory.getProvider(providerOid);
            if (output == null) {
                cacheProviderName.remove(providerName);//the cache entry, is no longer valid, remove it then
                throw new FindException("The ldap identity provider referenced from this assertion cannot be found anymore - " + providerName + ", " + providerOid + ".");
            }
            return output;
        }
    }

    private String handleFormat(OperationAttribute attribute) {
        return handleFormat(attribute.getFormat(), attribute.getValue());
    }

    private String handleFormat(String format, String value) {
        if (null == format || "".equals(format)) {//it will be this case most of the time
            return value;
        } else if ("unicodePwd".equalsIgnoreCase(format)) {
            return encodePassword(value);
        } else if ("digest-md5".equalsIgnoreCase(format)) {
            final String hash = new String(HexUtils.encodeBase64(HexUtils.getMd5Digest(value.getBytes())));
            final String ret =  "{MD5}" + hash;
            return ret;
        } else if ("digest-sha1".equalsIgnoreCase(format)) {
            final String hash = new String(HexUtils.encodeBase64(HexUtils.getSha1Digest(value.getBytes())));
            final String ret =  "{SHA}" + hash;
            return ret;
        } else if ("sha1-base64".equalsIgnoreCase(format)) {
            final String ret =  "{SHA}" + DatatypeConverter.printBase64Binary(DatatypeConverter.parseHexBinary(value));
            return ret;
        } else if ("encodeBase64".equalsIgnoreCase(format)) {
            final String ret = new String(HexUtils.encodeBase64(value.getBytes()));
            return ret;
        } else if ("decodeBase64".equalsIgnoreCase(format)) {
            final String ret = new String(HexUtils.decodeBase64(value));
            return ret;
        }
        //add more here if needed
        //
        //if we reach here, just return the value as is
        return value;
    }

    //microsoft AD requires a specific encode for setting password
    private String encodePassword(String password) {
        String quotedPassword = "\"" + password + "\"";
        char unicodePwd[] = quotedPassword.toCharArray();
        byte pwdArray[] = new byte[unicodePwd.length * 2];

        for (int i = 0; i < unicodePwd.length; i++) {
            pwdArray[i * 2 + 1] = (byte) (unicodePwd[i] >>> 8);
            pwdArray[i * 2 + 0] = (byte) (unicodePwd[i] & 0xff);
        }
        return new String(pwdArray);
    }

    //Operations
    protected final static String MANAGE = "manage";
    protected final static String VALIDATE_PROVIDER = "validateProvider";
    protected final static String CLEAR_CACHE = "clearCache";

    //- PRIVATE
    private final IdentityProviderFactory identityProviderFactory;
    private JAXBResourceUnmarshaller unmarshaller;
    private String providerName;
    private boolean injectionProtection = true;
    private StringBuffer responseBody;
    private final static String CREATE = "CREATE";
    private final static String UPDATE = "UPDATE";
    private final static String DELETE = "DELETE";
    private final static String SUCCESS = "SUCCESS";
    private final static String EMPTY = "";


    private static final long cacheCleanupInterval = ConfigFactory.getLongProperty("com.l7tech.external.assertions.ldapupdate.cacheCleanupInterval", 321123L); // 3853476=1hr,
    // key: resolved search filter value value: cached entry
    private final ConcurrentMap<String, Goid> cacheProviderName = new ConcurrentHashMap<String, Goid>();
    private final TimerTask cacheCleanupTask = new ManagedTimerTask() {
        @Override
        protected void doRun() {
            cacheProviderName.clear();
        }
    };
}