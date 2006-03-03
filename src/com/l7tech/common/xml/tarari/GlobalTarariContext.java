/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.xpath.CompilableXpath;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.objectmodel.FindException;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Implementations manage the server-wide state of the RAX API
 */
public interface GlobalTarariContext {
    int NO_SUCH_EXPRESSION = -1;

    /**
     * this makes a list of all community schema in the table as well as all the schemas defined in
     * policies and makes sure the schemas loaded on the tarari card are the same. this should typically
     * be called whenever a published service is updated or saved
     */
    public void updateSchemasToCard(BeanFactory managerResolver) throws FindException, IOException, XmlException;

    /**
     * returns the number of schemas loaded on the card that refers to a targetnamespace
     */
    public int targetNamespaceLoadedMoreThanOnce(String targetNamespace);

    /**
     * Prepare a {@link com.l7tech.common.xml.xpath.CompiledXpath} instance that will use Tarari hardware acceleration features.
     *
     * @param compilableXpath the CompilableXpath to compile.  Must not be null.
     * @return A TarariCompiledXpath instance.  Never null.
     */
    CompiledXpath compileXpath(CompilableXpath compilableXpath);

    /**
     * Attempts to convert the specified XPath expression into Tarari Normal Form.  Expressions in normal form
     * do not contain any qnames and hence no longer require a separate namespace map.
     *
     * @param xpathToSimplify the expression to simplify into Tarari Normal Form.  Must not be null.
     * @param namespaceMap a map of prefixes to namespace URIs, declaring any prefixes used by xpathToSimplify.
     *                     May be null if xpathToSimplify does not contain any qualified names.
     * @return the XPath simplified into Tarari Normal Form, suitable for use with Tarari fastxpath.  Never null.
     */
    String toTarariNormalForm(String xpathToSimplify, Map namespaceMap);
}
