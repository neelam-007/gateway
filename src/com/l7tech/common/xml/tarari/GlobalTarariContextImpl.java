/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.common.xml.tarari;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.common.xml.xpath.CompilableXpath;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.FastXpath;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.rax.fastxpath.XPathCompiler;
import com.tarari.xml.rax.fastxpath.XPathCompilerException;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the server-side Tarari state.
 * This class should only be referenced by other classes that are already contaminated with direct or indirect
 * static references to Tarari classes.  Anyone else should instead reference the uncontaminated interface
 * {@link GlobalTarariContext} instead.
 * <p/>
 * Locks used in this class:
 * <ul>
 * <li>fastxpathLock - used to protect currentXpaths, XPathCompiler's static data structure, compilerGeneration,
 *                     xpathChangedSinceLastCompilation,
 *                     and schemas in the card when they are being resynched with the database
 * </ul>
 *
 */
public class GlobalTarariContextImpl implements GlobalTarariContext, TarariSchemaHandler {
    private final Logger logger = Logger.getLogger(GlobalTarariContextImpl.class.getName());
    static final ReadWriteLock tarariLock = new WriterPreferenceReadWriteLock();

    private Xpaths currentXpaths = buildDefaultXpaths();
    private long compilerGeneration = 1;
    boolean xpathChangedSinceLastCompilation = true;
    private SchemaResolver tarariResolver;

    /**
     * Compiles the list of XPath expressions that have been gathered so far onto the Tarari card.
     */
    public void compileAllXpaths() {
        logger.fine("compiling xpath expressions");
        while (true) {
            try {
                tarariLock.writeLock().acquire();
                if (!xpathChangedSinceLastCompilation) {
                    logger.fine("skipping compilation since no changes to xpath expressions were detected");
                    return;
                }
                try {
                    String[] expressions = currentXpaths.getExpressions();
                    XPathCompiler.compile(expressions);
                    currentXpaths.installed(expressions, ++compilerGeneration);
                    xpathChangedSinceLastCompilation = false;
                    return; // No exception, we're done
                } catch (XPathCompilerException e) {
                    int badIndex = e.getErrorLine() - 1; // Silly Tarari, 1-based arrays are for kids!
                    if (badIndex < 1) {
                        // work around Tarari bug in latest RAXJ where it doesn't report the error line
                        logger.warning("At least one fastxpath could not be compiled -- probing for and removing bad fastxpaths");
                        probeForBadXpaths();
                        /* FALLTHROUGH and try compiling again */
                    } else {
                        logger.fine("Disabling fastxpath for non-TNF expression with index #" + badIndex);
                        currentXpaths.markInvalid(currentXpaths.getExpression(badIndex));
                        if (currentXpaths.getExpression(0) == null)
                            throw new IllegalStateException("Last XPath was removed without successful compilation");
                        /* FALLTHROUGH and try compiling again */
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Tarari write lock");
            } finally {
                tarariLock.writeLock().release();
            }
        }
    }

    /**
     * Attempt to work around RAXJ bug where error index is always zero by probing for bad xpaths, one by one.
     * Must be called by someone who holds the fastxpath write lock.
     * <p/>
     * This method adds each current xpath to the set one at a time and tries to compile.  This way it can detect
     * the bad expressions.
     */
    private void probeForBadXpaths() {
        Set bad = new HashSet();
        String[] exprs = currentXpaths.getExpressions();
        String[] test = new String[exprs.length];
        for (int i = 0; i < test.length; i++)
            test[i] = "/UNUSED";
        for (int i = 0; i < exprs.length; i++) {
            final String expr = exprs[i];
            try {
                test[i] = expr;
                logger.finest("Test compile of fastxpath: " + expr);
                XPathCompiler.compile(test);
            } catch (Exception e) {
                test[i] = "/UNUSED";
                //noinspection unchecked
                bad.add(expr);
                logger.fine("Compile failed for fastxpath: " + expr);
            }
        }
        //noinspection ForLoopReplaceableByForEach
        for (Iterator i = bad.iterator(); i.hasNext();) {
            String expr = (String)i.next();
            logger.fine("Disabling fastxpath for non-TNF expression: " + expr);
            currentXpaths.markInvalid(expr);
            if (currentXpaths.getExpression(0) == null)
                throw new IllegalStateException("Last XPath was removed without successful compilation");
        }
    }

    /**
     * Get the compiler generation count of the most recently installed set of xpaths.  This is used to check whether
     * a particular xpath was present in the hardware when a particular TarariMessageContext was created.
     * Caller must hold the fastxpath read lock.
     *
     * @return the compiler generation count of the most recently installed set of xpaths.
     */
    long getCompilerGeneration() {
        return compilerGeneration;
    }

    public CompiledXpath compileXpath(CompilableXpath compilableXpath) throws InvalidXpathException {
        return new TarariCompiledXpath(compilableXpath, this);
    }

    public TarariCompiledStylesheet compileStylesheet(String stylesheet) throws ParseException {
        return new TarariCompiledStylesheetImpl(stylesheet);
    }

    public FastXpath toTarariNormalForm(String xpathToSimplify, Map namespaceMap) {
        try {
            return TarariXpathConverter.convertToFastXpath(namespaceMap, xpathToSimplify);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Adds an XPath expression to the context.
     * <p>
     * If this expression is valid, after {@link #compileAllXpaths} is called, and assuming the expression is a valid
     * Tarari Normal Form xpath that is accepted by the compiler, then subsequent calls to {@link #getXpathIndex}
     * will return a positive result.
     *
     * @param expression the XPath expression to add to the context.
     */
    void addXpath(String expression) {
        try {
            tarariLock.writeLock().acquire();
            currentXpaths.add(expression);
            xpathChangedSinceLastCompilation = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tarari write lock");
        } finally {
            tarariLock.writeLock().release();
        }
    }

    /**
     * Indicates that the caller is no longer interested in the specified expression.
     */
    void removeXpath(String expression) {
        try {
            tarariLock.writeLock().acquire();
            currentXpaths.remove(expression);
            xpathChangedSinceLastCompilation = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tarari write lock");
        } finally {
            tarariLock.writeLock().release();
        }
    }

    /**
     * @param expression the expression to look for.
     * @param targetCompilerGeneration the GlobalTarariContext compiler generation count that was in effect when your
     *                                 {@link com.l7tech.common.xml.tarari.TarariMessageContext} was produced.
     *                                 This is a mandatory parameter.
     *                                 See {@link #getCompilerGeneration}.
     * @return the 1-based Tarari index for the given expression, or a number less than 1
     *         if the given expression was not included in the specified compiler generation count.
     */
    public int getXpathIndex(String expression, long targetCompilerGeneration) {
        if (expression == null) return -1;
        return currentXpaths.getIndex(expression, targetCompilerGeneration) + 1;
    }

    public void setTarariSchemaResolver(final TarariSchemaResolver resolver) {
        if (resolver == null) throw new NullPointerException();
        this.tarariResolver = new SchemaResolver() {
            public byte[] resolveSchema(String namespaceUri, String locationHint, String baseUri) {
                return resolver.resolveSchema(namespaceUri, locationHint, baseUri);
            }
        };
    }

    public void loadHardware(String systemId, String schemadoc)
            throws SoftwareFallbackException, SAXException {
        try {
            tarariLock.writeLock().acquire();
            byte[] bytes = schemadoc.getBytes("UTF-8");
            SchemaLoader.setSchemaResolver(tarariResolver);
            SchemaLoader.loadSchema(new ByteArrayInputStream(bytes), systemId);
        } catch (XmlConfigException e) {
            TarariUtil.translateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen (it's a BAIS)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tarari write lock");
        } finally {
            tarariLock.writeLock().release();
        }
    }

    public void unloadHardware(String tns) {
        try {
            this.tarariLock.writeLock().acquire();
            SchemaLoader.unloadSchema(tns);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tarari write lock");
        } finally {
            tarariLock.writeLock().release();
        }
    }

    public boolean validate(TarariMessageContext tmc) throws SAXException {
        long before = 0;
        boolean result = false;
        try {
            this.tarariLock.readLock().acquire();
            if (logger.isLoggable(Level.FINE)) {
                before = System.currentTimeMillis();
                logger.fine("Validing message in hardware");
            }

            result = ((TarariMessageContextImpl)tmc).getRaxDocument().validate();
            return result;
        } catch (com.tarari.xml.XmlException e) {
            throw new SAXException("Unable to validate document: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tarari read lock");
        } finally {
            tarariLock.readLock().release();
            if (logger.isLoggable(Level.FINE)) {
                long after = System.currentTimeMillis();
                logger.log(Level.FINE, "Validation {0} in {1}ms", new Object[] { result ? "succeeded" : "failed", after-before});
            }
        }
    }


    /**
     * Builds the initial {@link Xpaths}
     */
    private Xpaths buildDefaultXpaths() {
        // Built-in stuff for isSoap etc.
        ArrayList xpaths0 = new ArrayList(TarariUtil.ISSOAP_XPATHS.length + 10);
        //noinspection unchecked
        xpaths0.addAll(Arrays.asList(TarariUtil.ISSOAP_XPATHS));
        int ursStart = xpaths0.size() + 1; // 1-based arrays
        int[] uriIndices = new int[SoapUtil.ENVELOPE_URIS.size()+1];
        for (int i = 0; i < SoapUtil.ENVELOPE_URIS.size(); i++) {
            String uri = (String)SoapUtil.ENVELOPE_URIS.get(i);
            String nsXpath = TarariUtil.NS_XPATH_PREFIX + uri + TarariUtil.NS_XPATH_SUFFIX;
            //noinspection unchecked
            xpaths0.add(nsXpath);
            uriIndices[i] = i + ursStart;
        }
        uriIndices[uriIndices.length-1] = 0;

        return new Xpaths(xpaths0, uriIndices);
    }

    /**
     * @return the indices corresponding to the xpath expressions that match namespace URIs for isSoap
     */
    public int[] getSoapNamespaceUriIndices() {
        return currentXpaths.getSoapUriIndices();
    }
}
