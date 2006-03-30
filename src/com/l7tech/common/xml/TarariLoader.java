/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.objectmodel.FindException;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Probe presence of Tarari without introducing any static classloader dependencies on Tarari.
 */
public class TarariLoader {
    /**
     * Set this to <code>true</code>
     */
    public static final String ENABLE_PROPERTY = "com.l7tech.common.xml.tarari.enable";

    private static final String XPATH_COMPILER_CLASSNAME = "com.tarari.xml.rax.fastxpath.XPathCompiler";
    private static final String FACTORIES_CLASSNAME = "com.l7tech.common.xml.tarari.TarariFactories";
    private static final String SERVERTARARICONTEXT_CLASSNAME = "com.l7tech.common.xml.tarari.GlobalTarariContextImpl";

    private static Boolean tarariPresent = null;
    private static GlobalTarariContext tarariContext = null;
    private static TarariMessageContextFactory messageContextFactory = null;

    private static final Logger logger = Logger.getLogger(TarariLoader.class.getName());

    /**
     * @return the XML acceleration context if present, or null if not
     */
    public static GlobalTarariContext getGlobalContext() {
        if (tarariPresent == null) initialize();
        return tarariContext;
    }

    /**
     * @return the Tarari message context factory if XML acceleration is present, or null if not.
     */
    public static TarariMessageContextFactory getMessageContextFactory() {
        if (tarariPresent == null) initialize();
        return messageContextFactory;
    }

    private static void initialize() {
        if (tarariPresent == null) {
            synchronized (TarariLoader.class) {
                if (tarariPresent == null) {
                    if (Boolean.getBoolean(ENABLE_PROPERTY)) {
                        try {
                            logger.fine("Tarari hardware XML acceleration probe is starting...");

                            Class xpathCompilerClass = Class.forName(XPATH_COMPILER_CLASSNAME, false, TarariLoader.class.getClassLoader());
                            Method resetMethod = xpathCompilerClass.getMethod("reset", new Class[0]);
                            resetMethod.invoke(null, new Object[0]);

                            Class tarariFactoryClass = Class.forName(FACTORIES_CLASSNAME);
                            Constructor c = tarariFactoryClass.getConstructor(new Class[0]);
                            messageContextFactory = (TarariMessageContextFactory)c.newInstance(new Object[0]);

                            Class tarariContextClass = Class.forName(SERVERTARARICONTEXT_CLASSNAME);
                            c = tarariContextClass.getConstructor(new Class[0]);
                            tarariContext = (GlobalTarariContext)c.newInstance(new Object[0]);

                            logger.info("Tarari hardware XML acceleration probe succeeded: XPath compiler is ready");
                            tarariPresent = Boolean.TRUE;
                        } catch (UnsatisfiedLinkError e) {
                            logger.log(Level.WARNING, "Tarari hardware XML acceleration probe failed: " + e.getMessage(), e);
                            tarariPresent = Boolean.FALSE;
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Tarari hardware XML acceleration probe failed: " + t.getMessage(), t);
                            tarariPresent = Boolean.FALSE;
                        }
                    } else {
                        // Disabled -- skip the probe
                        logger.info("Tarari hardware XML acceleration probe not performed: probing is not enabled");
                        tarariPresent = Boolean.FALSE;
                    }
                }
            }
        }
    }

    /**
     * If there's a GlobalTarariContext, calls compile() on it.  This will only do a full compile
     * if the set of registered global xpaths has changed since the last time a compile was done.
     */
    public static void compile() {
        GlobalTarariContextImpl context = (GlobalTarariContextImpl)getGlobalContext();
        if (context != null) {
            try {
                context.compileAllXpaths();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unable to compile fastxpaths: all expressions will fallback to direct xpath: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    public static void updateSchemasToCard(BeanFactory managerResolver) throws FindException, IOException, XmlException {
        GlobalTarariContext context = getGlobalContext();
        if (context != null)
            context.updateSchemasToCard(managerResolver);
    }
}
