package com.l7tech.console.tree;

import com.l7tech.common.LicenseException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.AssertionFinder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Mar 9, 2006
 * Time: 3:05:19 PM
 */
public abstract class AbstractPaletteFolderNode extends AbstractAssertionPaletteNode {
    protected static final Logger logger = Logger.getLogger(AbstractPaletteFolderNode.class.getName());

    private final String id;
    private final String name;

    protected AbstractPaletteFolderNode(String name, String id) {
        this(name, id, null, null);
    }

    protected AbstractPaletteFolderNode(String name, String id, Object object, Comparator c) {
        super(object, c);
        this.name = name;
        this.id = id;
        if (id == null || id.length() < 1)
            throw new IllegalArgumentException("Palette folder node must provide a palette folder ID");
        logger.fine("PaletteFolderNode ID: " + id);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Insert any modular assertions that belong in this folder.
     *
     * @param nextIndex the childIndex to use for the first modular assertion inserted by this method.
     * @return the childIndex to use for the next child inserted.  This will be the incoming nextIndex plus one
     *         for each modular assertion that was inserted.
     */
    protected int insertMatchingModularAssertions(int nextIndex) {
        AssertionFinder assFinder = TopComponents.getInstance().getAssertionRegistry();
        Set<Assertion> bothHands = assFinder.getAssertions();
        for (Assertion ass : bothHands) {
            // Find variants
            Assertion[] variants = (Assertion[])ass.meta().get(AssertionMetadata.VARIANT_PROTOTYPES);
            if (variants == null || variants.length < 1) variants = new Assertion[] { ass };

            for (Assertion variant : variants) {
                String[] folders = (String[])variant.meta().get(AssertionMetadata.PALETTE_FOLDERS);
                if (folders == null || folders.length < 1) folders = new String[] {};
                for (String folder : folders) {
                    if (this.id.equals(folder)) {
                        // This assertion wants to be in this folder
                        nextIndex = insertModularAssertion(variant, nextIndex);
                    }
                }
            }
        }
        return nextIndex;
    }

    /**
     * Insert a palette node for the specified modular assertion into this folder, if possible.
     *
     * @param ass  the prototype assertion that wants to be inserted.  Must not be null.
     * @param nextIndex the childIndex to use for this modular assertion.
     * @return the childIndex to use for the next child inserted.  This will have been incremented from nextIndex
     *         if an palette node was added successfully.
     */
    protected int insertModularAssertion(Assertion ass, int nextIndex) {
        //noinspection unchecked
        Functions.Unary< AbstractAssertionPaletteNode, Assertion > factory =
                (Functions.Unary<AbstractAssertionPaletteNode, Assertion>)
                        ass.meta().get(AssertionMetadata.PALETTE_NODE_FACTORY);
        if (factory == null) {
            logger.warning("Assertion " + ass.getClass().getName() + " requests to be in palette folder " + id + " but provides no paletteNodeFactory");
            return nextIndex;
        }

        AbstractAssertionPaletteNode paletteNode = factory.call(ass);
        if (paletteNode == null) {
            logger.warning("Assertion " + ass.getClass().getName() + " requests to be in palette folder " + id + " but its paletteNodeFactory returned null");
            return nextIndex;
        }

        insert(paletteNode, nextIndex++);
        return nextIndex;
    }

    protected int insertMatchingCustomAssertions(int index, Category category) {
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(category).iterator();
            while (it.hasNext()) {
                CustomAssertionHolder a = (CustomAssertionHolder)it.next();
                insert(new CustomAccessControlNode(a), index++);
            }
        } catch (RuntimeException e1) {
            if (ExceptionUtils.causedBy(e1, LicenseException.class)) {
                logger.log(Level.INFO, "Custom assertions unavailable or unlicensed");
            } else
                logger.log(Level.WARNING, "Unable to retrieve custom assertions", e1);
        } 
        return index;
    }    

    protected String iconResource(boolean open) {
        if (open) return getOpenIconResource();
        return getClosedIconResource();
    }

    protected String getOpenIconResource() {
        return "com/l7tech/console/resources/folderOpen.gif";
    }
    protected String getClosedIconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }
}
