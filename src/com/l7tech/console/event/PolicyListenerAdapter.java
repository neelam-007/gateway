package com.l7tech.console.event;

/**
 * Class <code>PolicyListenerAdapter</code>. An abstract adapter class for
 * receiving policy assertion events. The methods in this class are empty.
 * This class exists as convenience for creating listener objects.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
abstract public class PolicyListenerAdapter implements PolicyListener {
    public void assertionsChanged(PolicyEvent e) {
    }
   public void assertionsInserted(PolicyEvent e) {
    }
    public void assertionsRemoved(PolicyEvent e) {
    }
    public void policyStructureChanged(PolicyEvent e) {
    }
}
