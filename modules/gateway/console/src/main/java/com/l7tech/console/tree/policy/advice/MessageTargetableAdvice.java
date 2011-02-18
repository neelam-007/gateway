package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;

import java.util.Arrays;
import java.util.List;

/**
 * Policy advice for Message Targetable assertions (sets the target message)
 */
public class MessageTargetableAdvice extends AddContextSensitiveAssertionAdvice {

    //- PUBLIC

    @Override
    public void proceed( PolicyChange pc ) {
        super.proceed( pc );
        pc.proceed();
    }

    //- PROTECTED

    @Override
    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof MessageTargetable )) throw new IllegalStateException();
        if ( !ignore(assertion) ) {
            MessageTargetable messageTargetable = (MessageTargetable) assertion;
            messageTargetable.setTarget( TargetMessageType.REQUEST );
        }
    }

    @Override
    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof MessageTargetable )) throw new IllegalStateException();
        if ( !ignore(assertion) ) {
            MessageTargetable messageTargetable = (MessageTargetable) assertion;
            messageTargetable.setTarget( TargetMessageType.RESPONSE );
        }
    }

    //- PRIVATE

    // Ignore any assertions that handle message target themselves
    private static final List<Class<? extends Assertion>> IGNORE_CLASSES = Arrays.<Class<? extends Assertion>>asList(
            Regex.class,
            SchemaValidation.class,
            XslTransformation.class,
            RequestSizeLimit.class
    );

    private boolean ignore(final Assertion assertion) {
        boolean ignore = false;

        if ( assertion == null || IGNORE_CLASSES.contains( assertion.getClass() )) {
            ignore = true;
        }

        return ignore;
    }
}
