/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.variable;

import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.xml.sax.SAXException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class replaces the variables placeholders in the string that is passed to the
 * {@link ExpandVariables#process(String)} method.
 * The variables placeholders are by default of format <code>${var.name}</code> where
 * <code>var.name</code> is the variable name.
 * The variables are passed in the <code>Map</code> of string key-value pairs. The default
 * variables are passed in constructor and optional overriding variables can be passed in
 * {@link ExpandVariables#process(String, java.util.Map)} method.
 *
 * @author emil
 * @version Apr 8, 2005
 */
public class ExpandVariables {
    public static final String COMMON_VAR_REMOTEIP = "request.tcp.remoteip";
    public static final String COMMON_VAR_OPERATIONNAME = "request.soap.operationname";
    public static final String COMMON_VAR_OPERATIONURN = "request.soap.urn";
    // todo, add common variables as needed here

    private static final Logger logger = Logger.getLogger(ExpandVariables.class.getName());

    public static final String DEF_PREFIX = "(?:\\$\\{)";
    public static final String DEF_SUFFIX = "(?:\\})";
    private static final Pattern regexPattern = Pattern.compile(DEF_PREFIX+"(.+?)"+DEF_SUFFIX);

    private final Map defaultVariables;

    /**
     *  Default Constructor.  Creates the empty default varialbes map.
     */
    public ExpandVariables() {
        this(Collections.EMPTY_MAP);
    }

    /**
     * Constructor accepting the default variables map
     *
     * @param variables the default variables.
     */
    public ExpandVariables(Map variables) {
        if (variables == null) {
            throw new IllegalArgumentException();
        }
        this.defaultVariables = variables;
    }

    /**
     * Process the input string and expand the variables using the
     * default variables map in this class.
     *
     * @param s the input message as a message
     * @return the message with expanded/resolved varialbes
     * @throws NoSuchVariableException if the varialbe
     */
    public String process(String s) throws NoSuchVariableException {
        return process(s, Collections.EMPTY_MAP);
    }

    public static String[] getReferencedNames(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        ArrayList vars = new ArrayList();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            vars.add(var);
        }
        return (String[])vars.toArray(new String[0]);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param userVariables the caller supplied varialbes map that is consulted first
     * @return the message with expanded/resolved varialbes
     * @throws NoSuchVariableException if the varialbe
     */
    public String process(String s, Map userVariables) throws NoSuchVariableException {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }
            String var = matcher.group(1);
            String replacement = null;
            Object varval = userVariables.get(var);
            if (varval != null) {
                replacement = varval.toString();
            }
            if (replacement == null) {
                varval = defaultVariables.get(var);
                if (varval != null) {
                    replacement = varval.toString();
                }
            }
            if (replacement == null) {
                throw new NoSuchVariableException(var);
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void populateLazyRequestVariables(final PolicyEnforcementContext cntx) {
        // remote address variable
        final TcpKnob tcp = (TcpKnob)cntx.getRequest().getKnob(TcpKnob.class);
        if (tcp == null) {
            logger.info("This context's request has no TcpKnob. No remoteIp variable can be populated.");
        } else {//
            cntx.setVariable(COMMON_VAR_REMOTEIP, new Object() {
                public String toString() {
                    return tcp.getRemoteAddress();
                }
            });
        }

        // operation name variable
        cntx.setVariable(COMMON_VAR_OPERATIONNAME, new Object() {
                public String toString() {
                    try {
                        Operation op = cntx.getOperation();
                        if (op != null) {
                            return op.getName();
                        }
                    } catch (IOException e) {
                        logger.log(Level.INFO, "cannot get operation name", e);
                    } catch (SAXException e) {
                        logger.log(Level.INFO, "cannot get operation name", e);
                    } catch (WSDLException e) {
                        logger.log(Level.INFO, "cannot get operation name", e);
                    } catch (InvalidDocumentFormatException e) {
                        logger.log(Level.INFO, "cannot get operation name", e);
                    }
                    return "[unknown]";
                }
            });

        // operation urn variable
        cntx.setVariable(COMMON_VAR_OPERATIONURN, new Object() {
                public String toString() {
                    try {
                        return cntx.getRequest().getSoapKnob().getPayloadNamespaceUri();
                    } catch (IOException e) {
                        logger.log(Level.INFO, "cannot get operation urn", e);
                    } catch (SAXException e) {
                        logger.log(Level.INFO, "cannot get operation urn", e);
                    } catch (MessageNotSoapException e) {
                        logger.log(Level.INFO, "cannot get operation urn", e);
                    }
                    return "[unknown]";
                }
            });

        // todo, other common variables as needed.
    }

}