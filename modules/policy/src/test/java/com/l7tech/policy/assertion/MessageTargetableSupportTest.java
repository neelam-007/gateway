package com.l7tech.policy.assertion;

import com.l7tech.test.BugNumber;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageTargetableSupportTest {
    @Test
    @BugNumber(13388)
    public void copyConstructorSetsTargetModifiedByGateway() {
        final MessageTargetableSupport original = new MessageTargetableSupport();
        original.setTargetModifiedByGateway(true);
        final MessageTargetableSupport copy = new MessageTargetableSupport(original);
        assertTrue(copy.isTargetModifiedByGateway());
    }

    @Test
    @BugNumber(13388)
    public void setTaretMessageSetsTargetModifiedByGateway() {
        final MessageTargetableSupport original = new MessageTargetableSupport();
        original.setTargetModifiedByGateway(true);
        final MessageTargetableSupport newMessage = new MessageTargetableSupport();
        newMessage.setTargetMessage(original);
        assertTrue(newMessage.isTargetModifiedByGateway());
    }

}
