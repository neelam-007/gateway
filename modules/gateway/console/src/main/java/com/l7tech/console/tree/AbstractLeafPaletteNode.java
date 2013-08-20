package com.l7tech.console.tree;

import com.l7tech.console.action.ConfigureSecurityZoneAction;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.util.Collection;
import java.util.logging.Level;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Base class for leaf nodes (no children) with icons.
 *
 * @author $Author$
 * @version $Revision$
 */
public abstract class AbstractLeafPaletteNode extends AbstractAssertionPaletteNode implements Comparable<AbstractTreeNode> {

    //- PUBLIC

    /**
     *
     */
    public AbstractLeafPaletteNode(final String name, final String iconResource) {
        super(null);
        this.name = name != null ? name : "NAME NOT SET";
        this.iconResource = iconResource;
        this.base64EncodedIconImage = null;
    }

    public AbstractLeafPaletteNode(final Assertion assertion){
        super(null);
        final AssertionMetadata meta = assertion.meta();
        this.name = meta.get(PALETTE_NODE_NAME).toString();
        this.iconResource = meta.get(PALETTE_NODE_ICON).toString();
        this.base64EncodedIconImage = meta.get(BASE_64_NODE_IMAGE) == null ? null : meta.get(BASE_64_NODE_IMAGE).toString();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true as this is a leaf
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isSearchable(NodeFilter filter) {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     *
     * @return false since this is a leaf
     */
    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * The name of this node
     *
     * @return the node name that is displayed
     */
    @Override
    public String getName() {
        return name;
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open not applicable to palette nodes
     */
    @Override
    protected String iconResource(boolean open) {
        return iconResource;
    }

    @Override
    protected String base64EncodedIconImage(boolean open) {
        return base64EncodedIconImage;
    }

    @Override
    public int compareTo( final AbstractTreeNode treeNode ) {
        return String.CASE_INSENSITIVE_ORDER.compare(getName(),treeNode.getName());
    }

    @Override
    public Action[] getActions() {
        Action[] ret = new Action[0];
        final Assertion assertion = asAssertion();
        if (assertion != null) {
            // If no security zones are visible to current admin and the zone is not set on the assertion, don't bother offering the security zone action
            try {
                Collection<SecurityZone> zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
                final AssertionAccess assertionAccess = TopComponents.getInstance().getAssertionRegistry().getAssertionAccess(assertion);
                if ((zones == null || zones.isEmpty()) && (assertionAccess == null || assertionAccess.getSecurityZone() == null)) {
                    return ret;
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to check security zones: " + ExceptionUtils.getMessage(e), e);
            }

            SecureAction zoneAction = createSecurityZoneAction(assertion);

            ret = zoneAction.isAuthorized() ? new Action[] { zoneAction } : new Action[]{};
        }
        return ret;
    }

    protected ConfigureSecurityZoneAction createSecurityZoneAction(final Assertion assertion) {
        return new ConfigureSecurityZoneAction<AssertionAccess>(TopComponents.getInstance().getAssertionRegistry().getAssertionAccess(assertion), new EntitySaver<AssertionAccess>() {
            @Override
            public AssertionAccess saveEntity(AssertionAccess assertionAccess) throws SaveException {
                try {
                    Registry.getDefault().getRbacAdmin().saveAssertionAccess(assertionAccess);
                    TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
                    return TopComponents.getInstance().getAssertionRegistry().getAssertionAccess(assertion);
                } catch (final UpdateException e) {
                    throw new SaveException(e);
                }
            }
        });
    }

    //- PRIVATE

    private final String name;
    private final String iconResource;
    private final String base64EncodedIconImage;
}
