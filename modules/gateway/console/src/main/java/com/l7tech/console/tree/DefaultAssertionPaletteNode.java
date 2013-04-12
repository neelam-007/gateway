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
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Palette node used for modular assertions that do not specify a custom palette node factory in their
 * metadata.
 */
public class DefaultAssertionPaletteNode<AT extends Assertion> extends AbstractLeafPaletteNode {
    private static final Logger logger = Logger.getLogger(DefaultAssertionPaletteNode.class.getName());

    final AT prototype;

    public DefaultAssertionPaletteNode(final AT prototype) {
        super(prototype);
        this.prototype = prototype;
    }

    @Override
    protected ClassLoader iconClassLoader() {
        return prototype.getClass().getClassLoader();
    }

    @Override
    public Assertion asAssertion() {
        //noinspection unchecked
        Functions.Unary<AT, AT> factory =
                (Functions.Unary<AT, AT>)
                        prototype.meta().get(AssertionMetadata.ASSERTION_FACTORY);
        if (factory != null)
            return factory.call(prototype);

        // no factory -- try to just newInstance off the prototype
        try {
            return prototype.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Action[] getActions() {
        Action[] ret = new Action[0];

        // If no security zones are visible to current admin, don't bother offering the security zone action
        try {
            Collection<SecurityZone> zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
            if (zones == null || zones.isEmpty())
                return ret;
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check security zones: " + ExceptionUtils.getMessage(e), e);
        }

        SecureAction zoneAction = new ConfigureSecurityZoneAction<AssertionAccess>(TopComponents.getInstance().getAssertionRegistry().getAssertionAccess(prototype), new EntitySaver<AssertionAccess>() {
            @Override
            public AssertionAccess saveEntity(AssertionAccess assertionAccess) throws SaveException {
                try {
                    Registry.getDefault().getRbacAdmin().saveAssertionAccess(assertionAccess);
                    TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
                    return TopComponents.getInstance().getAssertionRegistry().getAssertionAccess(prototype);
                } catch (UpdateException e) {
                    throw new SaveException(e);
                }
            }
        });

        if (!zoneAction.isAuthorized())
            return ret;

        return zoneAction.isAuthorized() ? new Action[] { zoneAction } : new Action[]{};
    }
}
