/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.xml.tarari;

import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xslt.CompiledStylesheet;
import com.l7tech.xml.xpath.CompilableXpath;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.FastXpath;

import java.text.ParseException;
import java.util.Map;

/**
 * Implementations manage the server-wide state of the RAX API
 */
public interface GlobalTarariContext {
    int NO_SUCH_EXPRESSION = -1;

    /**
     * Prepare a {@link com.l7tech.xml.xpath.CompiledXpath} instance that will use Tarari hardware acceleration features.
     *
     * @param compilableXpath the CompilableXpath to compile.  Must not be null.
     * @return A TarariCompiledXpath instance.  Never null.
     * @throws InvalidXpathException if the XPath could not be parsed, even with Tarari direct XPath 1.0.
     */
    CompiledXpath compileXpath(CompilableXpath compilableXpath) throws InvalidXpathException;

    /**
     * Prepare a {@link CompiledStylesheet} instance that can transform messages using Tarari.
     *
     * @return a TarariCompiledStylesheet instance.  Never null.
     * @throws ParseException if the stylesheet could not be compiled.
     */
    TarariCompiledStylesheet compileStylesheet(String stylesheet) throws ParseException;

    /**
     * Attempts to convert the specified XPath expression into Tarari Normal Form.  Expressions in normal form
     * do not contain any qnames and hence no longer require a separate namespace map.
     *
     * @param xpathToSimplify the expression to simplify into Tarari Normal Form.  Must not be null.
     * @param namespaceMap a map of prefixes to namespace URIs, declaring any prefixes used by xpathToSimplify.
     *                     May be null if xpathToSimplify does not contain any qualified names.
     * @return the XPath simplified into Tarari Normal Form, suitable for use with Tarari fastxpath.  Never null.
     */
    FastXpath toTarariNormalForm(String xpathToSimplify, Map namespaceMap);

    /**
     * Compiles the list of XPath expressions that have been gathered so far onto the Tarari card.
     */
    void compileAllXpaths();
}
