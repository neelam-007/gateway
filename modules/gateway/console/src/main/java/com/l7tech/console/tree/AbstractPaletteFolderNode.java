package com.l7tech.console.tree;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.AssertionFinder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;

import javax.swing.tree.TreeNode;
import java.util.Collection;
import java.util.Comparator;
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

    protected AbstractPaletteFolderNode(String name, String id, Object object, Comparator<TreeNode> c) {
        super(object, c);
        this.name = name;
        this.id = id;
        if (id == null || id.length() < 1)
            throw new IllegalArgumentException("Palette folder node must provide a palette folder ID");
        logger.fine("PaletteFolderNode ID: " + id);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Insert any modular assertions that belong in this folder.
     * <p/>
     * Requirements for a match:<br>
     * A modular assertion will 'match' when it defines the PALETTE_FOLDERS meta data, and any of the contents of this
     * String [] match the <code>id</code> of this folder node.
     * e.g. for an assertion to match the 'Policy Logic' folder
     * meta() should define: meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"})
     *
     * Once PALETTE_FOLDERS is defined in an assertions meta data, insertModularAssertionByType should no longer be used
     */
    protected void insertMatchingModularAssertions() {

        AssertionFinder assFinder = TopComponents.getInstance().getAssertionRegistry();
        Set<Assertion> bothHands = assFinder.getAssertions();
        for (Assertion ass : bothHands) {
            insertAssertionVariants(ass);
        }
    }

    /**
     * Insert any encapsulated assertions that belong in this folder.
     */
    protected void insertMatchingEncapsulatedAssertions() {
        Collection<EncapsulatedAssertionConfig> configs = TopComponents.getInstance().getEncapsulatedAssertionRegistry().getRegisteredEncapsulatedAssertionConfigurations();
        for (EncapsulatedAssertionConfig config : configs) {
            Assertion ass = new EncapsulatedAssertion(config);
            insertAssertionVariants(ass);
        }
    }

    private void insertAssertionVariants(Assertion ass) {
        // Find variants
        Assertion[] variants = (Assertion[]) ass.meta().get(AssertionMetadata.VARIANT_PROTOTYPES);
        if (variants == null || variants.length < 1) variants = new Assertion[]{ass};

        for (Assertion variant : variants) {
            String[] folders = (String[]) variant.meta().get(AssertionMetadata.PALETTE_FOLDERS);
            if (folders == null || folders.length < 1) folders = new String[]{};
            for (String folder : folders) {
                if (this.id.equals(folder)) {
                    // This assertion wants to be in this folder
                    insertMetadataBasedAssertion(variant);
                }
            }
        }
    }


    /**
     * Insert a modular assertion at the specified index.
     * <p/>
     * Requirements for use:
     * The meta data for the modular assertion MUST NOT define PALETTE_FOLDERS.
     * If the assertion defines PALETTE_FOLDERS meta data and it matches the folder node from where this method is called,
     * then the assertion will appear in the folder twice.
     *
     * @param assertionClass assertion to represent in the palette folder
     * @return int insert index
     */
    protected void insertModularAssertionByType(Class<? extends Assertion> assertionClass) {
        AssertionFinder assFinder = TopComponents.getInstance().getAssertionRegistry();
        Set<Assertion> bothHands = assFinder.getAssertions();
        for (Assertion ass : bothHands) {
            // Find variants
            Assertion[] variants = (Assertion[]) ass.meta().get(AssertionMetadata.VARIANT_PROTOTYPES);
            if (variants == null || variants.length < 1) variants = new Assertion[]{ass};

            for (Assertion variant : variants) {
                if (assertionClass.isInstance(variant)) {
                    // Add assertion to folder
                    insertMetadataBasedAssertion(variant);
                }
            }
        }
    }


    /**
     * Insert a palette node for the specified assertion into this folder, if possible.
     *
     * @param ass  the prototype assertion that wants to be inserted.  Must not be null.
     * @return the childIndex to use for the next child inserted.  This will have been incremented from nextIndex
     *         if an palette node was added successfully.
     */
    protected void insertMetadataBasedAssertion(Assertion ass) {
        //noinspection unchecked
        Functions.Unary< AbstractAssertionPaletteNode, Assertion > factory =
                (Functions.Unary<AbstractAssertionPaletteNode, Assertion>)
                        ass.meta().get(AssertionMetadata.PALETTE_NODE_FACTORY);
        if (factory == null) {
            logger.warning("Assertion " + ass.getClass().getName() + " requests to be in palette folder " + id + " but provides no paletteNodeFactory");
            return;
        }

        AbstractAssertionPaletteNode paletteNode = factory.call(ass);
        if (paletteNode == null) {
            logger.warning("Assertion " + ass.getClass().getName() + " requests to be in palette folder " + id + " but its paletteNodeFactory returned null");
            return;
        }

        insert(paletteNode, getInsertPosition( paletteNode ));
    }

    protected void insertMatchingCustomAssertions(Category category) {
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            for (Object o : cr.getAssertions(category)) {
                CustomAssertionHolder a = (CustomAssertionHolder) o;
                final CustomAccessControlNode customNode =  new CustomAccessControlNode(a);
                insert(customNode, getInsertPosition(customNode));
            }
        } catch (RuntimeException e1) {
            if (ExceptionUtils.causedBy(e1, LicenseException.class)) {
                logger.log(Level.INFO, "Custom assertions unavailable or unlicensed");
            } else
                logger.log(Level.WARNING, "Unable to retrieve custom assertions", e1);
        } 
    }

    @Override
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

    /**
     * @return the palette folder ID for modular assertion metadata matching purposes, eg "serviceAvailability".
     */
    public String getFolderId() {
        return id;
    }
}
