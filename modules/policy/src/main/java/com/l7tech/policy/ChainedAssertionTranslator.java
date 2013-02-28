package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An AssertionTranslator which supports multiple translations through chaining.
 */
public class ChainedAssertionTranslator implements AssertionTranslator {
    private final Logger logger = Logger.getLogger(ChainedAssertionTranslator.class.getName());
    private final List<AssertionTranslator> translators;

    /**
     * @param translators the AssertionTranslators which will be chained in the order they were added to the List.
     */
    public ChainedAssertionTranslator(@NotNull List<AssertionTranslator> translators) {
        this.translators = translators;
    }

    /**
     * Translates an Assertion by running it through a chain of AssertionTranslators.
     */
    @Override
    public Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException {
        Assertion toTranslate = sourceAssertion;
        for (final AssertionTranslator translator : translators) {
            final Assertion translated = translator.translate(toTranslate);
            translator.translationFinished(toTranslate);
            toTranslate = translated;
        }
        return toTranslate;
    }

    @Override
    public void translationFinished(@Nullable Assertion sourceAssertion) {
        logger.log(Level.FINER, "Finished translating assertion: " + sourceAssertion.toString());
    }
}
