/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.tarari;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariUtil;
import com.l7tech.server.communityschemas.CommunitySchemaManager;
import com.l7tech.server.communityschemas.CommunitySchemaEntry;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.objectmodel.FindException;
import com.tarari.xml.schema.SchemaLoader;
import com.tarari.xml.schema.SchemaLoadingException;
import com.tarari.xml.xpath.XPathCompiler;
import com.tarari.xml.xpath.XPathCompilerException;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.springframework.beans.factory.BeanFactory;
import org.w3.x2001.xmlSchema.SchemaDocument;
import org.apache.xmlbeans.XmlException;

/**
 * Holds the server-side Tarari state
 */
public class GlobalTarariContextImpl implements GlobalTarariContext {
    private final Logger logger = Logger.getLogger(GlobalTarariContextImpl.class.getName());
    private Xpaths currentXpaths = buildDefaultXpaths();
    private long compilerGeneration = 1;
    boolean xpathChangedSinceLastCompilation = true;
    private String[] tnss;

    public void compile() {
        // fla added 20-07-05 to avoid compiling xpath all the time for no reason
        if (!xpathChangedSinceLastCompilation) {
            logger.fine("skipping compilation since no changes to xpath expressions were detected");
            return;
        }
        logger.fine("compiling xpath expressions");
        while (true) {
            try {
                String[] expressions = currentXpaths.getExpressions();
                XPathCompiler.compile(expressions, 0);
                currentXpaths.installed(expressions, nextCompilerGeneration());
                xpathChangedSinceLastCompilation = false;
                return; // No exception, we're done
            } catch (XPathCompilerException e) {
                int badIndex = e.getCompilerErrorLine() - 1; // Silly Tarari, 1-based arrays are for kids!
                currentXpaths.remove(currentXpaths.getExpression(badIndex));
                if (currentXpaths.getExpression(0) == null)
                    throw new IllegalStateException("Last XPath was removed without successful compilation");
            }
        }
    }

    private synchronized long nextCompilerGeneration() {
        return ++compilerGeneration;
    }

    public long getCompilerGeneration() {
        return compilerGeneration;
    }

    public void addXpath(String expression) throws InvalidXpathException {
        currentXpaths.add(expression);
        xpathChangedSinceLastCompilation = true;
    }

    public void removeXpath(String expression) {
        currentXpaths.remove(expression);
        xpathChangedSinceLastCompilation = true;
    }

    /**
     * this makes a list of all community schema in the table as well as all the schemas defined in
     * policies and makes sure the schemas loaded on the tarari card are the same. this should typically
     * be called whenever a published service is updated or saved
     */
    public void updateSchemasToCard(BeanFactory managerResolver) throws FindException, IOException, SchemaLoadingException, XmlException {
        synchronized (this) {
            // List schemas on card
            ArrayList schemasOnCard = new ArrayList();
            String[] schemas = SchemaLoader.listSchemas();
            if (schemas != null) {
                for (int i = 0; i < schemas.length; i++) {
                    schemasOnCard.add(schemas[i]);

                }
            }

            // List schemas that need to be there
            ArrayList schemasInPolicyAndTable = new ArrayList();
            CommunitySchemaManager manager = (CommunitySchemaManager)managerResolver.getBean("communitySchemaManager");
            Collection allCommunitySchemas = manager.findAll();
            for (Iterator iterator = allCommunitySchemas.iterator(); iterator.hasNext();) {
                CommunitySchemaEntry communitySchemaEntry = (CommunitySchemaEntry) iterator.next();
                schemasInPolicyAndTable.add(communitySchemaEntry.getSchema());
            }
            ServiceCache servicesCache = (ServiceCache)managerResolver.getBean("serviceCache");
            schemasInPolicyAndTable.addAll(servicesCache.getAllPolicySchemas());

            // Record discrepencies
            ArrayList schemasOnCardThatShouldNotBeThere = new ArrayList();
            for (Iterator iterator = schemasOnCard.iterator(); iterator.hasNext();) {
                String soncard = (String) iterator.next();
                if (!schemasInPolicyAndTable.contains(soncard)) {
                    schemasOnCardThatShouldNotBeThere.add(soncard);
                }
            }
            ArrayList schemasMissingFromCard = new ArrayList();
            for (Iterator iterator = schemasInPolicyAndTable.iterator(); iterator.hasNext();) {
                String necessarys = (String) iterator.next();
                if (!schemasOnCard.contains(necessarys)) {
                    schemasMissingFromCard.add(necessarys);
                }
            }

            // Apply necessary modifications
            // currently, tarari's api does not allow to unload a particular schema so for now we need
            // to unload everything and readd everything again
            // todo, modify code when tarari's api is fixed
            // SchemaLoader.unloadSchema();

            // are there any discrepencies?
            if (!schemasOnCardThatShouldNotBeThere.isEmpty() || !schemasMissingFromCard.isEmpty()) {
                // asper note above, we need to remove everything for now
                if (!schemasOnCard.isEmpty()) {
                    logger.fine("removing schemas from card");
                    SchemaLoader.unloadAllSchemas();
                }

                for (Iterator iterator = schemasInPolicyAndTable.iterator(); iterator.hasNext();) {
                    String s = (String) iterator.next();
                    logger.fine("loading schema to card " + s);
                    SchemaLoader.loadSchema(new ByteArrayInputStream(s.getBytes("UTF-8")), "");
                }
            } else {
                logger.fine("schemas loaded on card are already correct");
            }


            // Keep track of all targetnamespace so that serverschemavalidation can check whether or not more than one
            // schema uses the same tns
            tnss = new String[schemasInPolicyAndTable.size()];
            int i = 0;
            for (Iterator iterator = schemasInPolicyAndTable.iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                SchemaDocument sdoc = SchemaDocument.Factory.parse(new StringReader(s));
                String tns = sdoc.getSchema().getTargetNamespace();
                tnss[i] = tns;
                i++;
            }

            // todo, for each schema, we also need to maintain xpath expressions to be loaded on the card. these
            // xpath expressions are there so verify that the children elements under the body match a target namespace
            // we need to be able to verify these for each of the tns
        }
    }

    public int targetNamespaceLoadedMoreThanOnce(String targetNamespace) {
        if (tnss == null) {
            logger.severe("tnss not loaded");
            return 0;
        }
        int output = 0;
        for (int i = 0; i < tnss.length; i++) {
            String tns = tnss[i];
            if (tns.equals(targetNamespace)) {
                ++output;
            }
        }
        return output;
    }

    /*
    public void addSchema(String schema) throws UnsupportedEncodingException, SchemaLoadingException {
        synchronized (GlobalTarariContextImpl.class) {
            logger.info("loading schema to card: " + schema);
            SchemaLoader.loadSchema(new ByteArrayInputStream(schema.getBytes("UTF-8")), "");
        }
    }

    public void removeAllSchemasFromCard() {
        synchronized (GlobalTarariContextImpl.class) {
            logger.info("removing all schemas from the card");
            // this seems to cause problems when card has nothing in it
            // SchemaLoader.unloadAllSchemas();
            logger.finest("querying SchemaLoader.listSchemas");
            String[] schemas = SchemaLoader.listSchemas();
            if (schemas == null || schemas.length < 1) {
                logger.finest("card already empty");
                return;
            } else {
                logger.finest("card not empty, trying to unload all schemas");
                SchemaLoader.unloadAllSchemas();
            }
        }
    }
    */

    public int getXpathIndex(String expression, long targetCompilerGeneration) {
        if (expression == null) return -1;
        return currentXpaths.getIndex(expression, targetCompilerGeneration) + 1;
    }

    /**
     * Builds the initial {@link Xpaths}
     */
    private Xpaths buildDefaultXpaths() {
        // Built-in stuff for isSoap etc.
        ArrayList xpaths0 = new ArrayList(TarariUtil.ISSOAP_XPATHS.length + 10);
        xpaths0.addAll(Arrays.asList(TarariUtil.ISSOAP_XPATHS));
        int ursStart = xpaths0.size() + 1; // 1-based arrays
        int[] uriIndices = new int[SoapUtil.ENVELOPE_URIS.size()+1];
        for (int i = 0; i < SoapUtil.ENVELOPE_URIS.size(); i++) {
            String uri = (String)SoapUtil.ENVELOPE_URIS.get(i);
            String nsXpath = TarariUtil.NS_XPATH_PREFIX + uri + TarariUtil.NS_XPATH_SUFFIX;
            xpaths0.add(nsXpath);
            uriIndices[i] = i + ursStart;
        }
        uriIndices[uriIndices.length-1] = 0;

        return new Xpaths(xpaths0, uriIndices);
    }

    public int[] getSoapNamespaceUriIndices() {
        return currentXpaths.getSoapUriIndices();
    }
}
