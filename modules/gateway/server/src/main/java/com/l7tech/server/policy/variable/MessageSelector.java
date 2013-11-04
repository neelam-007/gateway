/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.UncheckedIOException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.identity.User;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.xml.MessageNotSoapException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Extracts values by name from {@link Message}s.
 * @author alex
*/
public class MessageSelector implements ExpandVariables.Selector<Message> {

    private static Map<String, MessageAttributeSelector> selectorMap = new HashMap<String, MessageAttributeSelector>();

    // NOTE: Variable names must be lower case
    private static final String HTTP_HEADER_PREFIX = "http.header.";
    private static final String HTTP_HEADERVALUES_PREFIX = "http.headervalues.";
    private static final String HTTP_HEADERNAMES = "http.headernames";
    private static final String HTTP_ALLHEADERVALUES = "http.allheadervalues";
    private static final String STATUS_NAME = "http.status";
    private static final String MAINPART_NAME = "mainpart";
    private static final String PARTS_NAME = "parts";
    private static final String ORIGINAL_MAINPART_NAME = "originalmainpart";
    private static final String SIZE_NAME = "size";
    private static final String CONTENT_TYPE = "contenttype";
    private static final String MAINPART_CONTENT_TYPE = "mainpart.contenttype";
    private static final String MAINPART_SIZE_NAME = "mainpart.size";
    private static final String BUFFER_STATUS = "buffer.status";
    private static final String BUFFER_ALLOWED = "buffer.allowed";
    private static final String COMMAND_TYPE_NAME = "command.type";
    private static final String COMMAND_PARAMETER_PREFIX = "command.parameter.";

    static enum BufferStatus {
        UNINITIALIZED("uninitialized"),
        UNREAD("unread"),
        BUFFERED("buffered"),
        GONE("gone"),
        ;
        private final String label;

        BufferStatus(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }
    }

    // NOTE: Variable names must be lower case
    private static final String WSS_PREFIX = "wss.";
    private static final String WSS_CERT_COUNT = WSS_PREFIX + "certificates.count";
    private static final String WSS_CERT_VALUES_PREFIX = WSS_PREFIX + "certificates.value.";
    private static final String WSS_SIGN_CERT_COUNT = WSS_PREFIX + "signingcertificates.count";
    private static final String WSS_SIGN_CERT_VALUES_PREFIX = WSS_PREFIX + "signingcertificates.value.";

    // NOTE: Variable names must be lower case
    private static final String TCP_REMOTE_ADDRESS = "tcp.remoteaddress";
    private static final String TCP_REMOTE_IP = "tcp.remoteip";
    private static final String TCP_REMOTE_HOST = "tcp.remotehost";
    private static final String TCP_REMOTE_PORT = "tcp.remoteport";
    private static final String TCP_LOCAL_ADDRESS = "tcp.localaddress";
    private static final String TCP_LOCAL_IP = "tcp.localip";
    private static final String TCP_LOCAL_HOST = "tcp.localhost";
    private static final String TCP_LOCAL_PORT = "tcp.localport";
    private static final String TCP_LISTEN_PORT = "tcp.listenport";

    // NOTE: Variable names must be lower case
    private static final String SSL_CIPHER_SUITE = "ssl.ciphersuite";
    private static final String SSL_KEY_SIZE = "ssl.keysize";
    private static final String SSL_SESSION_ID = "ssl.sessionid";

    // NOTE: Variable names must be lower case
    private static final String AUTH_USER_PASSWORD = "password";
    private static final String AUTH_USER_USERNAME = "username";
    private static final String AUTH_USER_USER = "authenticateduser";
    private static final String AUTH_USER_USERS = "authenticatedusers";
    private static final String AUTH_USER_DN = "authenticateddn";
    private static final String AUTH_USER_DNS = "authenticateddns";

    // NOTE: Variable names must be lower case
    private static final String SOAP_ENVELOPE_URI = "soap.envelopens";
    private static final String SOAP_VERSION = "soap.version";

    private static final String JMS_HEADER_PREFIX = "jms.header.";
    private static final String JMS_HEADERNAMES = "jms.headernames";
    private static final String JMS_ALLHEADERVALUES = "jms.allheadervalues";

    private static final Map<String, Functions.Unary<Object, TcpKnob>> TCP_FIELDS = Collections.unmodifiableMap(new HashMap<String, Functions.Unary<Object, TcpKnob>>() {{
        put(TCP_REMOTE_ADDRESS, Functions.propertyTransform(TcpKnob.class, "remoteAddress"));
        put(TCP_REMOTE_IP, Functions.propertyTransform(TcpKnob.class, "remoteAddress"));
        put(TCP_REMOTE_HOST, Functions.propertyTransform(TcpKnob.class, "remoteHost"));
        put(TCP_REMOTE_PORT, Functions.propertyTransform(TcpKnob.class, "remotePort"));
        put(TCP_LOCAL_ADDRESS, Functions.propertyTransform(TcpKnob.class, "localAddress"));
        put(TCP_LOCAL_IP, Functions.propertyTransform(TcpKnob.class, "localAddress"));
        put(TCP_LOCAL_HOST, Functions.propertyTransform(TcpKnob.class, "localHost"));
        put(TCP_LOCAL_PORT, Functions.propertyTransform(TcpKnob.class, "localPort"));
        put(TCP_LISTEN_PORT, Functions.propertyTransform(TcpKnob.class, "localListenerPort"));
    }});

    private static final Map<String, String> SERVLET_ATTRIBUTE_FIELDS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(SSL_CIPHER_SUITE, "javax.servlet.request.cipher_suite");
        put(SSL_KEY_SIZE, "javax.servlet.request.key_size");
        put(SSL_SESSION_ID, "javax.servlet.request.ssl_session");
    }});

    private static final Pattern PATTERN_PERIOD = Pattern.compile("\\.");

    private static final List<Class<? extends HasHeaders>> httpHeaderHaverKnobClasses =
            Arrays.<Class<? extends HasHeaders>>asList(HttpRequestKnob.class, HttpOutboundRequestKnob.class, HttpResponseKnob.class, HttpInboundResponseKnob.class, HeadersKnob.class);

    private static final List<Class<? extends HasHeaders>> jmsHeaderHaverKnobClasses = Arrays.<Class<? extends HasHeaders>>asList(JmsKnob.class);

    public static void registerSelector(String prefix, MessageAttributeSelector selector) {
        selectorMap.put(prefix, selector);
    }

    public static void unRegisterSelector(String prefix) {
        selectorMap.remove(prefix);
    }

    @Override
    public Class<Message> getContextObjectClass() {
        return Message.class;
    }

    @Override
    public Selection select(String contextName, Message message, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        final MessageAttributeSelector selector;

        final String lname = name.toLowerCase();
        String prefix = lname;
        int index = prefix.indexOf( '.' );
        if ( index > -1) prefix = prefix.substring( 0, index );
        if ( !ArrayUtils.contains(getPrefixes(), prefix) && (selectorMap.get(prefix) == null)) return null; // this check ensures prefixes are added to the list

        if (lname.startsWith(HTTP_HEADER_PREFIX))
            selector = singleHeaderSelector;
        else if (lname.startsWith(HTTP_HEADERVALUES_PREFIX))
            selector = multiHeaderSelector;
        else if (lname.equals(HTTP_HEADERNAMES))
            selector = headerNamesSelector;
        else if (lname.equals(HTTP_ALLHEADERVALUES))
            selector = allHeaderValuesSelector;
        else if (STATUS_NAME.equals(lname)) {
            selector = statusSelector;
        } else if (SIZE_NAME.equals(lname)) {
            selector = sizeSelector;
        } else if (MAINPART_NAME.equals(lname)) {
            selector = mainPartSelector;
        } else if (ORIGINAL_MAINPART_NAME.equals(lname)) {
            selector = originalMainPartSelector;
        } else if (MAINPART_SIZE_NAME.equals(lname)) {
            selector = mainPartSizeSelector;
        } else if (BUFFER_STATUS.equals(lname)) {
            selector = bufferStatusSelector;
        } else if (BUFFER_ALLOWED.equals(lname)) {
            selector = bufferAllowedSelector;
        } else if (lname.equals(CONTENT_TYPE)) {
            selector = contentTypeSelector;
        } else if (lname.equals(MAINPART_CONTENT_TYPE)) {
            selector = mainPartContentTypeSelector;
        } else if (SOAP_ENVELOPE_URI.equals(lname)) {
            selector = soapEnvelopeSelector;
        } else if (SOAP_VERSION.equals(lname)) {
            selector = soapVersionSelector;
        } else if (lname.startsWith(PARTS_NAME)) {
            selector = partsSelector;
        } else if (lname.startsWith(WSS_PREFIX)) {
            selector = wssSelector;
        } else if (lname.equals(AUTH_USER_PASSWORD) || lname.equals(AUTH_USER_USERNAME)) {
            selector = selectLastCredentials(message, lname);
        } else if (lname.startsWith(AUTH_USER_USERS)) {
            selector = select(new AuthenticatedUserGetter<String>(AUTH_USER_USERS, true, String.class, AuthenticatedUserGetter.USER_TO_NAME, message));
        } else if (lname.startsWith(AUTH_USER_USER)) {
            String[] names = PATTERN_PERIOD.split(name, 2);
            String remainingName = names.length > 1 ? names[1] : null;

            if (remainingName == null) {
                selector = select(new AuthenticatedUserGetter<String>(AUTH_USER_USER, false, String.class, AuthenticatedUserGetter.USER_TO_NAME, message));
            } else {
                // Check if the suffix (remainingName) is an index or an attribute of User object.
                try {
                    Integer.parseInt(remainingName);
                    // Case 1: No exception thrown means the suffix is an index (integer).
                    selector = select(new AuthenticatedUserGetter<String>(AUTH_USER_USER, false, String.class, AuthenticatedUserGetter.USER_TO_NAME, message));
                } catch (NumberFormatException e) {
                    // Case 2: An exception thrown means the suffix is an attribute.
                    final AuthenticatedUserGetter userGetter = new AuthenticatedUserGetter<User>(AUTH_USER_USER, false, User.class,AuthenticatedUserGetter.USER_TO_ITSELF, message);
                    final User user = (User) userGetter.get(names[0], PolicyEnforcementContextFactory.getCurrent());
                    if (user == null) {
                        return new Selection("");
                    } else {
                        return new Selection(user, remainingName);
                    }
                }
            }
        } else if (lname.startsWith(AUTH_USER_DNS)) {
            selector = select(new AuthenticatedUserGetter<String>(AUTH_USER_DNS, true, String.class, AuthenticatedUserGetter.USER_TO_DN, message));
        } else if (lname.startsWith(AUTH_USER_DN)) {
            selector = select(new AuthenticatedUserGetter<String>(AUTH_USER_DN, false, String.class, AuthenticatedUserGetter.USER_TO_DN, message));
        } else if (lname.startsWith(JMS_HEADER_PREFIX)) {
            selector = jmsHeaderSelector;
        } else if (lname.startsWith(JMS_HEADERNAMES)) {
            selector = jmsHeaderNamesSelector;
        } else if (lname.startsWith(JMS_ALLHEADERVALUES)) {
            selector = jmsAllHeaderValuesSelector;
       } else if (lname.startsWith(COMMAND_PARAMETER_PREFIX)) {
            selector = commandParameterSelector;
        } else if (lname.startsWith(COMMAND_TYPE_NAME)) {
            selector = commandTypeSelector;
        } else if (selectorMap.get(prefix) != null) {
            selector = selectorMap.get(prefix);
        } else {
            final Functions.Unary<Object,TcpKnob> tcpFieldGetter = TCP_FIELDS.get(lname);
            if (tcpFieldGetter != null) {
                selector = new TcpKnobMessageAttributeSelector(tcpFieldGetter);
            } else {
                String servletAttrName = SERVLET_ATTRIBUTE_FIELDS.get(lname);
                if (servletAttrName != null) {
                    selector = new HttpServletAttributeSelector(servletAttrName);
                } else {
                    String msg = handler.handleBadVariable(name + " in " + message.getClass().getName());
                    if (strict) throw new IllegalArgumentException(msg);
                    return null;
                }
            }
        }

        return selector.select(message, name, handler, strict);
    }

    // prefix must also be added in BuiltinVariables
    static String[] getPrefixes() {
        return new String[]{
                "http",
                "jms",
                "command",
                "mainpart",
                "parts",
                "originalmainpart",
                "wss",
                "tcp",
                "ssl",
                "soap",
                "buffer",
                SIZE_NAME,
                CONTENT_TYPE,
                AUTH_USER_PASSWORD,
                AUTH_USER_USERNAME,
                AUTH_USER_USER,
                AUTH_USER_USERS,
                AUTH_USER_DN,
                AUTH_USER_DNS
        };
    }

    private MessageAttributeSelector select( final AuthenticatedUserGetter authenticatedUserGetter ) {
        return new MessageAttributeSelector(){
            @Override
            public Selection select( final Message context, final String name, final Syntax.SyntaxErrorHandler handler, final boolean strict ) {
                final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.getCurrent();
                Object got = pec==null ? null : authenticatedUserGetter.get( name, pec );
                return got == null ?  null : new Selection(got);
            }
        };
    }

    private MessageAttributeSelector selectLastCredentials( final Message message, final String credentialPart ) {
        return new MessageAttributeSelector(){
            @Override
            public Selection select( final Message context, final String name, final Syntax.SyntaxErrorHandler handler, final boolean strict ) {
                Object got = null;
                final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.getCurrent();
                final LoginCredentials creds = pec==null ? null : pec.getAuthenticationContext(message).getLastCredentials();
                if ( creds != null ) {
                    if ( AUTH_USER_USERNAME.equals( credentialPart ) ) {
                        got = creds.getName();
                    } else if ( AUTH_USER_PASSWORD.equals( credentialPart ) ) {
                        final char[] pass = creds.getCredentials();
                        if (pass != null && pass.length > 0) {
                            got = new String(pass);
                        }
                    }
                }
                return got == null ?  null : new Selection(got);
            }
        };
    }

    public static interface MessageAttributeSelector {
        Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict);
    }

    private static final MessageAttributeSelector commandTypeSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            CommandKnob commandKnob = context.getKnob(CommandKnob.class);
            if (commandKnob == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            return new Selection(commandKnob.getCommandType());
        }
    };

    private static final MessageAttributeSelector commandParameterSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            final String paramName = name.substring(COMMAND_PARAMETER_PREFIX.length());
            CommandKnob commandKnob = context.getKnob(CommandKnob.class);
            if (commandKnob == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            return new Selection(commandKnob.getParameter(paramName));
        }
    };

    private static final MessageAttributeSelector sizeSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            MimeKnob mimeKnob = context.getKnob(MimeKnob.class);
            if (mimeKnob == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            try {
                return new Selection(mimeKnob.getContentLength());
            } catch (IOException e) {
                String msg = handler.handleBadVariable(name, e);
                if (strict) throw new IllegalArgumentException("Unable to determine complete message length: " + msg);
                return null;
            }
        }
    };

    private static final MessageAttributeSelector mainPartSizeSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            MimeKnob mimeKnob = context.getKnob(MimeKnob.class);
            if (mimeKnob == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            try {
                return new Selection(mimeKnob.getFirstPart().getActualContentLength());
            } catch (IOException e) {
                String msg = handler.handleBadVariable(name, e);
                if (strict) throw new IllegalArgumentException("Unable to determine first part length: " + msg);
                return null;
            } catch (NoSuchPartException e) {
                String msg = handler.handleBadVariable(name, e);
                if (strict) throw new IllegalArgumentException("Unable to determine first part length: " + msg);
                return null;
            }
        }
    };

    private static final MessageAttributeSelector bufferAllowedSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            return new Selection(getBufferAllowed(context));
        }

        private boolean getBufferAllowed(Message message) {
            if (!message.isInitialized())
                return true;

            MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
            return mimeKnob == null || !mimeKnob.isBufferingDisallowed();
        }
    };

    private static final MessageAttributeSelector bufferStatusSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            return new Selection(getBufferStatus(context).getLabel());
        }

        @NotNull
        private BufferStatus getBufferStatus(Message message) {
            if (!message.isInitialized())
                return BufferStatus.UNINITIALIZED;

            MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
            if (mimeKnob == null)
                return BufferStatus.UNINITIALIZED;

            try {
                PartInfo pa = mimeKnob.getFirstPart();
                if (pa.isBodyStashed())
                    return BufferStatus.BUFFERED;
                if (pa.isBodyAvailable())
                    return BufferStatus.UNREAD;
                return BufferStatus.GONE;

            } catch (IOException e) {
                return BufferStatus.GONE;
            }
        }
    };

    private static final MessageAttributeSelector statusSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            HttpResponseKnob hrk = context.getKnob(HttpResponseKnob.class);
            if (hrk == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            return new Selection(hrk.getStatus());
        }
    };

    private final MessageAttributeSelector mainPartSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            try {
                final MimeKnob mk = context.getKnob(MimeKnob.class);
                if (mk == null) {
                    // Message not yet initialized
                    return null;
                }
                ContentTypeHeader cth = mk.getFirstPart().getContentType();
                if (cth.isTextualContentType()) {
                    // TODO maximum size? This could be huge and OOM
                    byte[] bytes = IOUtils.slurpStream(mk.getFirstPart().getInputStream(false));
                    return new Selection(new String(bytes, cth.getEncoding()));
                } else {
                    String msg = handler.handleBadVariable("Message is not text");
                    if (strict) throw new IllegalArgumentException(msg);
                    return null;
                }
            } catch (ByteLimitInputStream.DataSizeLimitExceededException e){
                String msg = handler.handleBadVariable("Unable to get message text: " + e.getMessage());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } catch (IOException e) {
                String msg = handler.handleBadVariable("IOException while retrieving main part");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } catch (NoSuchPartException e) {
                String msg = handler.handleBadVariable("NoSuchPartException while retrieving main part");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    };

    private final MessageAttributeSelector originalMainPartSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            try {
                if (!context.isXml()) {
                    return null;
                }

                final XmlKnob xk = context.getXmlKnob();
                if (xk == null) {
                    // Message not XML or was never examined
                    return null;
                }

                if (!context.isEnableOriginalDocument()) {
                    // Saving the original document not enabled for this message
                    return null;
                }

                Document original = xk.getOriginalDocument();
                // TODO maximum size? This could be huge and OOM
                return new Selection(XmlUtil.nodeToString(original));

            } catch (SAXException e) {
                String msg = handler.handleBadVariable("Message is not well-formed XML");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } catch (IOException e) {
                String msg = handler.handleBadVariable("IOException while retrieving original document");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    };

    private static final MessageAttributeSelector contentTypeSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message message, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            final MimeKnob mk = message.getKnob(MimeKnob.class);
            if (mk == null) {
                // Message not yet initialized
                return null;
            }
            return new Selection(mk.getOuterContentType().getFullValue());
        }
    };

    private static final MessageAttributeSelector mainPartContentTypeSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message message, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            final MimeKnob mk = message.getKnob(MimeKnob.class);
            if (mk == null) {
                // Message not yet initialized
                return null;
            }

            try {
                return new Selection(mk.getFirstPart().getContentType().getFullValue());
            } catch (IOException e) {
                String msg = handler.handleBadVariable("IOException while retrieving main part content type");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    };

    private static final MessageAttributeSelector partsSelector = new MessageAttributeSelector() {
        @Override
        public Selection select( final Message message,
                                 final String name,
                                 final Syntax.SyntaxErrorHandler handler,
                                 final boolean strict) {
            final MimeKnob mk = message.getKnob(MimeKnob.class);
            if (mk == null) {
                // Message not yet initialized
                return null;
            }

            try {
                final List<PartInfo> partList = new ArrayList<PartInfo>();
                for ( final PartInfo partInfo : mk ) {
                    partList.add( partInfo );
                }
                String remainingName = null;
                if ( name.length() > PARTS_NAME.length() ) {
                    remainingName = name.substring( PARTS_NAME.length() );
                    if ( remainingName.startsWith( "." )) {
                        remainingName = remainingName.substring( 1 );
                    }
                }
                return new Selection(partList.toArray( new PartInfo[partList.size()] ), remainingName);
            } catch ( UncheckedIOException e ) {
                String msg = handler.handleBadVariable("Unable to access message parts '"+ ExceptionUtils.getMessage( e ) +"'");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    };

    private static final MessageAttributeSelector wssSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            final String lname = name.toLowerCase();

            if (WSS_CERT_COUNT.equals(lname)) {
                return new Selection(getCertificateCount(context, false), null);
            }
            else if (WSS_SIGN_CERT_COUNT.equals(lname)) {
                return new Selection(getCertificateCount(context, true), null);
            } else { // cert value request
                String prefix = getCertValPrefix(lname);
                String rname = prefix == null ? null : lname.substring(prefix.length());
                String[] valueParts = rname == null ? null : rname.split("\\.", 2);
                if (valueParts != null && valueParts.length > 0 && isInt(valueParts[0]) ) {
                    X509Certificate cert = getCertificate(context, Integer.parseInt(valueParts[0]), WSS_SIGN_CERT_VALUES_PREFIX.equals(prefix));
                    return cert == null ?
                            null :
                            new Selection(cert , valueParts.length > 1 ? valueParts[1] : null);
                } else if (strict) {
                    String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
                    throw new IllegalArgumentException(msg);
                } else {
                    return null;
                }
            }
        }
    };

    private static String getCertValPrefix(String lname) {
        return lname == null ? null :
               lname.startsWith(WSS_CERT_VALUES_PREFIX) ? WSS_CERT_VALUES_PREFIX :
               lname.startsWith(WSS_SIGN_CERT_VALUES_PREFIX) ? WSS_SIGN_CERT_VALUES_PREFIX :
               null;
    }

    private static boolean isInt(String maybeInt) {
        try {
            Integer.parseInt(maybeInt);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static int getCertificateCount(Message message, boolean signingOnly) {
        int count = 0;
        ProcessorResult result = message.getSecurityKnob().getProcessorResult();
        if ( result != null ) {
            for(SecurityToken token : result.getXmlSecurityTokens()) {
                if ( token instanceof X509SigningSecurityToken && (!signingOnly || ((X509SigningSecurityToken)token).isPossessionProved()))
                    count++;
            }
        }
        return count;
    }

    private static X509Certificate getCertificate(Message message, int index, boolean signingOnly) {
        ArrayList<X509SigningSecurityToken> candidates = new ArrayList<X509SigningSecurityToken>();
        ProcessorResult result = message.getSecurityKnob().getProcessorResult();
        if ( result != null ) {
            for(SecurityToken token : result.getXmlSecurityTokens()) {
                if(token instanceof X509SigningSecurityToken && (!signingOnly || ((X509SigningSecurityToken)token).isPossessionProved()))
                    candidates.add((X509SigningSecurityToken) token);
            }
        }
        return index < 1 || index > candidates.size() ? null : candidates.get(index - 1).getCertificate();
    }

    private static final MessageAttributeSelector headerNamesSelector = new HeaderNamesSelector(httpHeaderHaverKnobClasses);
    private static final MessageAttributeSelector jmsHeaderNamesSelector = new HeaderNamesSelector(jmsHeaderHaverKnobClasses);

    private static class HeaderNamesSelector implements MessageAttributeSelector {
        final List<Class<? extends HasHeaders>> supportedClasses;

        private HeaderNamesSelector(final List<Class<? extends HasHeaders>> supportedClasses) {
            this.supportedClasses = supportedClasses;
        }

        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            boolean sawHeaderHaver = false;
            for (Class<? extends HasHeaders> headerKnob : supportedClasses) {
                HasHeaders hrk = context.getKnob(headerKnob);
                if (hrk != null) {
                    sawHeaderHaver = true;
                    String[] headers = hrk.getHeaderNames();
                    return new Selection(headers);
                }
            }
             if (sawHeaderHaver) {
                String msg = handler.handleBadVariable("Headers are empty");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } else {
                String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    };


    private static final MessageAttributeSelector allHeaderValuesSelector = new AllHeaderValuesSelector(httpHeaderHaverKnobClasses);
    private static final MessageAttributeSelector jmsAllHeaderValuesSelector = new AllHeaderValuesSelector(jmsHeaderHaverKnobClasses);

    private static final class AllHeaderValuesSelector implements MessageAttributeSelector {
        final List<Class<? extends HasHeaders>> supportedClasses;

        private AllHeaderValuesSelector(final List<Class<? extends HasHeaders>> supportedClasses) {
            this.supportedClasses = supportedClasses;
        }

        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            boolean sawHeaderHaver = false;
            for (Class<? extends HasHeaders> headerKnob : supportedClasses) {
                HasHeaders hrk = context.getKnob(headerKnob);
                if (hrk != null) {
                    sawHeaderHaver = true;
                    String[] headers = hrk.getHeaderNames();
                    ArrayList <String> values = new ArrayList<String>();
                    final Syntax syntax = Syntax.parse(name, Syntax.DEFAULT_MV_DELIMITER);
                    final char delimiter = ':';
                    for(String header : headers)
                    {
                        String [] vals = hrk.getHeaderValues(header);
                        String valStr = vals.length > 0 ? syntax.format(vals, Syntax.getFormatter( vals[0] ), handler, strict) : "";
                        values.add(header+delimiter+valStr) ;
                    }
                    return new Selection(values.toArray());
                }
            }
             if (sawHeaderHaver) {
                String msg = handler.handleBadVariable( "Headers are empty");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } else {
                String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    }

    private static final HeaderSelector singleHeaderSelector = new HeaderSelector(HTTP_HEADER_PREFIX, false, httpHeaderHaverKnobClasses);
    private static final HeaderSelector multiHeaderSelector = new HeaderSelector(HTTP_HEADERVALUES_PREFIX, true, httpHeaderHaverKnobClasses);
    private static final HeaderSelector jmsHeaderSelector = new HeaderSelector(JMS_HEADER_PREFIX, false, jmsHeaderHaverKnobClasses);

    public static class HeaderSelector implements MessageAttributeSelector {
        final String prefix;
        final boolean multi;
        final List<Class<? extends HasHeaders>> supportedClasses;

        public HeaderSelector(final String prefix, final boolean multi, final List<Class<? extends HasHeaders>> supportedClasses) {
            this.prefix = prefix;
            this.multi = multi;
            this.supportedClasses = supportedClasses;
        }

        @Override
        public Selection select(Message message, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            boolean sawHeaderHaver = false;
            final String hname = name.substring(prefix.length());
            for (Class<? extends HasHeaders> headerKnob : supportedClasses) {
                HasHeaders hrk = message.getKnob(headerKnob);
                if (hrk != null) {
                    sawHeaderHaver = true;
                    String[] vals = hrk.getHeaderValues(hname);
                    if (vals != null && vals.length > 0) {
                        return new Selection(multi ? vals : vals[0]);
                    }
                }
            }

            if (sawHeaderHaver) {
                String msg = handler.handleBadVariable(hname + " header was empty");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } else {
                String msg = handler.handleBadVariable(name + " in " + message.getClass().getName());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    }

    private static class TcpKnobMessageAttributeSelector implements MessageAttributeSelector {
        private final Functions.Unary<Object, TcpKnob> tcpFieldGetter;

        private TcpKnobMessageAttributeSelector(Functions.Unary<Object, TcpKnob> tcpFieldGetter) {
            this.tcpFieldGetter = tcpFieldGetter;
        }

        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            TcpKnob tcpknob = context.getKnob(TcpKnob.class);
            if (tcpknob == null)
                return null;
            return new Selection(tcpFieldGetter.call(tcpknob));
        }
    }

    private static class HttpServletAttributeSelector implements MessageAttributeSelector {
        private final String attributeName;

        private HttpServletAttributeSelector(String attributeName) {
            this.attributeName = attributeName;
        }

        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            HttpServletRequestKnob hsRequestKnob = context.getKnob(HttpServletRequestKnob.class);
            if (hsRequestKnob == null) {
                // TODO support protocols other than HTTPS
                // although getting this information out of Apache FtpServer doesn't look straightforward at the moment
                return null;
            }
            final HttpServletRequest httpServletRequest = hsRequestKnob.getHttpServletRequest();
            Object value = httpServletRequest.getAttribute(attributeName);
            return new Selection(value == null ? null : value.toString());
        }
    }

    private static final MessageAttributeSelector soapVersionSelector = new SoapKnobSelector(true);
    private static final MessageAttributeSelector soapEnvelopeSelector = new SoapKnobSelector(false);

    private static class SoapKnobSelector implements MessageAttributeSelector {
        private final boolean wantsVersion;

        private SoapKnobSelector(boolean wantsVersion) {
            this.wantsVersion = wantsVersion;
        }

        @Override
        public Selection select(Message mess, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            try {
                if (!mess.isSoap()) {
                    handler.handleBadVariable("Message is not SOAP");
                    return null;
                }
                SoapKnob soapKnob = mess.getSoapKnob();
                return new Selection(wantsVersion ? soapKnob.getSoapVersion().getVersionNumber() : soapKnob.getSoapEnvelopeUri());

            } catch (IOException e) {
                handler.handleBadVariable("Unable to obtain SOAP info for message", e);
                return null;
            } catch (SAXException e) {
                handler.handleBadVariable("Unable to obtain SOAP info for message", e);
                return null;
            } catch (MessageNotSoapException e) {
                handler.handleBadVariable("Unable to obtain SOAP info for message", e);
                return null;
            }
        }
    }
}