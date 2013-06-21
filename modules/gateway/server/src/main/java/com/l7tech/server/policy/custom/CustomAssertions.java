package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;

import java.util.*;
import java.util.logging.Logger;

/**
 * The utility class, custom assertions holder that keeps track of the registered
 * custom assertions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class CustomAssertions {
    static Logger logger = Logger.getLogger(CustomAssertions.class.getName());

    /**
     * cannot instanitate
     */
    private CustomAssertions() {}

    static void register( CustomAssertionDescriptor eh) {
        logger.fine("registering " + eh);
        assertions.put(eh.getName(), eh);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    static CustomAssertionDescriptor getDescriptor(Class a) {
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor cd = (CustomAssertionDescriptor)iterator.next();
            if (a.equals(cd.getAssertion())) {
                return cd;
            }
        }
        return null;
    }

    /**
   * Return the <code>CustomAssertionUI</code> for a given assertion or
   * <b>null<b>
   *
   * @param assertionClassName the assertion class
   * @return the custom assertion UI class or <b>null</b>
   */
  static CustomAssertionUI getUI(String assertionClassName) {
      for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
          CustomAssertionDescriptor cd = (CustomAssertionDescriptor)iterator.next();
          if (assertionClassName.equals(cd.getAssertion().getName())) {
              try {
                  Class uiClass = cd.getUiClass();
                  if (uiClass == null) {
                      return null;
                  }
                  return (CustomAssertionUI)uiClass.newInstance();
              } catch (Exception e) {
                  throw new RuntimeException(e);
              }
          }
      }
      return null;
  }

    /**
     * Return the <code>CustomAssertionUI</code> for a given assertion or
     * <b>null<b>
     *
     * @param assertionClassName the assertion class
     * @return the task action UI class or <b>null</b>
     */
    static CustomTaskActionUI getTaskActionUI(String assertionClassName) {
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor cd = (CustomAssertionDescriptor)iterator.next();
            if (assertionClassName.equals(cd.getAssertion().getName())) {
                try {
                    Class takActionUiClass = cd.getTakActionUiClass();
                    if (takActionUiClass == null) {
                        return null;
                    }
                    return (CustomTaskActionUI)takActionUiClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    /**
     * Return all the registered <code>CustomAssertionDescriptor</code>
     *
     * @return the registered descriptors
     */
    static Set getAllDescriptors() {
        Set descriptors = new HashSet();
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor cd = (CustomAssertionDescriptor)iterator.next();
            descriptors.add(cd);
        }
        return descriptors;
    }

    /**
     * @return the set of all descriptors
     */
    static Set getDescriptors() {
        Set descriptors = new HashSet();
        descriptors.addAll(assertions.values());
        return descriptors;
    }


    /**
     * @return the set of all assertions registered
     */
    static Set getDescriptors( Category cat) {
        Set descriptors = new HashSet();
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor cd = (CustomAssertionDescriptor)iterator.next();
            if (cat.equals(cd.getCategory())) {
                descriptors.add(cd);
            }
        }
        return descriptors;
    }

    /**
     * Return server assertion for a given assertion or <b>null<b>
     *
     * @param a the assertion class
     * @return the server assertion class or <b>null</b>
     */
    static Class getServerAssertion(Class a) {
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            if (a.equals(eh.getAssertion())) {
                return eh.getServerAssertion();
            }
        }
        return null;
    }

    /**
     * @return the set of all assertions registered
     */
    static Set getAssertions() {
        Set allAssertions = new HashSet();
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            allAssertions.add(eh.getAssertion());
        }
        return allAssertions;
    }

    /**
     * @return the set of all assertions registered
     */
    static Set getAssertions(Category cat) {
        Set allAssertions = new HashSet();
        for (Iterator iterator = assertions.values().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            if (cat.equals(eh.getCategory())) {
                allAssertions.add(eh.getAssertion());
            }
        }
        return allAssertions;
    }

    private static Map assertions = new HashMap();
}