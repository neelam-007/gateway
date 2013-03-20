package com.l7tech.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UsesEntitiesAtDesignTimeAssertionTranslator implements AssertionTranslator {
    private static final Logger logger = Logger.getLogger(UsesEntitiesAtDesignTimeAssertionTranslator.class.getName());
    private final HeaderBasedEntityFinder entityFinder;

    public UsesEntitiesAtDesignTimeAssertionTranslator(@NotNull final HeaderBasedEntityFinder entityFinder) {
        this.entityFinder = entityFinder;
    }

    @Override
    public Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException {
        if (sourceAssertion instanceof UsesEntitiesAtDesignTime) {
            final UsesEntitiesAtDesignTime usesEntities = (UsesEntitiesAtDesignTime) sourceAssertion;
            try {
                PolicyUtil.provideNeededEntities(usesEntities, entityFinder, null);
            } catch (final FindException e) {
                // don't fail if entities cannot be loaded (see SSM-4278)
                logger.log(Level.WARNING, "Unable to load UsesEntitiesAtDesignTime: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return sourceAssertion;
    }

    @Override
    public void translationFinished(@Nullable Assertion sourceAssertion) {
        if (sourceAssertion instanceof UsesEntitiesAtDesignTime) {
            logger.log(Level.FINEST, "Finished translating UsesEntitiesAtDesignTime assertion: " + sourceAssertion.toString());
        }
    }
}
