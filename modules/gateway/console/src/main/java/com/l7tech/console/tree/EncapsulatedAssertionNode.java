package com.l7tech.console.tree;

import com.l7tech.console.action.ConfigureSecurityZoneAction;
import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EncapsulatedAssertionNode extends DefaultAssertionPaletteNode<EncapsulatedAssertion> {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionNode.class.getName());

    public EncapsulatedAssertionNode(final EncapsulatedAssertion prototype) {
        super(prototype);
    }

    @Override
    protected ConfigureSecurityZoneAction createSecurityZoneAction(final Assertion assertion) {
        ConfigureSecurityZoneAction action = null;
        if (assertion instanceof EncapsulatedAssertion) {
            final EncapsulatedAssertion encass = (EncapsulatedAssertion) assertion;
            final EncapsulatedAssertionConfig config = encass.config();
            if (config != null) {
                action = new ConfigureSecurityZoneAction<EncapsulatedAssertionConfig>(config, new EntitySaver<EncapsulatedAssertionConfig>() {
                    @Override
                    public EncapsulatedAssertionConfig saveEntity(final EncapsulatedAssertionConfig entity) throws SaveException {
                        try {
                            final long oid = Registry.getDefault().getEncapsulatedAssertionAdmin().saveEncapsulatedAssertionConfig(entity);
                            TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
                            entity.setOid(oid);
                        } catch (final UpdateException | VersionException e) {
                            throw new SaveException("Unable to save EncapsulatedAssertionConfig", e);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to reload EncapsulatedAssertionConfigs: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return entity;
                    }
                });
            }
        } else {
            logger.log(Level.WARNING, "Assertion is not an EncapsulatedAssertion: " + assertion);
        }
        return action;
    }
}
