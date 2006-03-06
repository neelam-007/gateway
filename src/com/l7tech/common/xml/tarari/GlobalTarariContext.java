/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.xpath.CompilableXpath;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.InvalidXpathException;
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
     * @throws InvalidXpathException if the XPath could not be parsed, even with Tarari direct XPath 1.0.
     */
    CompiledXpath compileXpath(CompilableXpath compilableXpath) throws InvalidXpathException;

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

    /**
     * Validate the given document in hardare using the current schemas, if possible to do so.
     *
     * @param doc  the document to validate.  Must not be null.
     * @param desiredTargetNamespaceUri  the target namespace that must be validated.  Must not be null.  May be empty to refer to "no namespace"
     * @return Boolean.TRUE if the document was validated;
     *         Boolean.FALSE if the document was invalid; or
     *         null if hardware validation could not be attempted because the target namespace was loaded more than once.
     */
    Boolean validateDocument(TarariMessageContext doc, String desiredTargetNamespaceUri);
}
