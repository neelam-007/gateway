package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;

import java.util.*;
import java.util.logging.Logger;

/**
 * The utility class, custom assertions holder that keeps track of the registered custom assertions.
 */
class CustomAssertions {
    static Logger logger = Logger.getLogger(CustomAssertions.class.getName());

    /**
     * cannot instantiate
     */
    private CustomAssertions() {}

    static void register( CustomAssertionDescriptor eh) {
        logger.fine("registering " + eh);
        assertions.put(eh.getName(), eh);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    static CustomAssertionDescriptor getDescriptor(Class a) {
        for (CustomAssertionDescriptor cd : assertions.values()) {
            if (a.equals(cd.getAssertion())) {
                return cd;
            }
        }
        return null;
    }

    /**
   * Return the <code>CustomAssertionUI</code> for a given assertion or <b>null<b>
   *
   * @param assertionClassName the assertion class
   * @return the custom assertion UI class or <b>null</b>
   */
  static CustomAssertionUI getUI(String assertionClassName) {
      for (CustomAssertionDescriptor cd : assertions.values()) {
          if (assertionClassName.equals(cd.getAssertion().getName())) {
              try {
                  Class uiClass = cd.getUiClass();
                  if (uiClass == null) {
                      return null;
                  }
                  return (CustomAssertionUI) uiClass.newInstance();
              } catch (Exception e) {
                  throw new RuntimeException(e);
              }
          }
      }
      return null;
  }

    /**
     * Return the <code>CustomAssertionUI</code> for a given assertion or <b>null<b>
     *
     * @param assertionClassName the assertion class
     * @return the task action UI class or <b>null</b>
     */
    static CustomTaskActionUI getTaskActionUI(String assertionClassName) {
        for (CustomAssertionDescriptor cd : assertions.values()) {
            if (assertionClassName.equals(cd.getAssertion().getName())) {
                try {
                    Class takActionUiClass = cd.getTakActionUiClass();
                    if (takActionUiClass == null) {
                        return null;
                    }
                    return (CustomTaskActionUI) takActionUiClass.newInstance();
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
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        for (CustomAssertionDescriptor cd : assertions.values()) {
            descriptors.add(cd);
        }
        return descriptors;
    }

    /**
     * @return the set of all descriptors
     */
    static Set getDescriptors() {
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        descriptors.addAll(assertions.values());
        return descriptors;
    }


    /**
     * @return the set of all assertions registered
     */
    static Set getDescriptors( Category cat) {
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        for (CustomAssertionDescriptor cd : assertions.values()) {
            if (cat.equals(cd.getCategory())) {
                descriptors.add(cd);
            }
        }
        return descriptors;
    }

    /**
     *  Return the <code>CustomAssertionDescriptor</code> for a given assertion or <b>null<b>
     *
     * @param assertionClassName the assertion class
     * @return the first registered assertion matching the assertion class name
     */
    static CustomAssertionDescriptor getDescriptor(final String assertionClassName) {
        for (CustomAssertionDescriptor cd : assertions.values()) {
            if (cd.getAssertion().getName().equals(assertionClassName)) {
                return cd;
            }
        }
        return null;
    }

    /**
     * Return server assertion for a given assertion or <b>null<b>
     *
     * @param a the assertion class
     * @return the server assertion class or <b>null</b>
     */
    static Class getServerAssertion(Class a) {
        for (CustomAssertionDescriptor eh : assertions.values()) {
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
        Set<Class> allAssertions = new HashSet<>();
        for (CustomAssertionDescriptor eh : assertions.values()) {
            allAssertions.add(eh.getAssertion());
        }
        return allAssertions;
    }

    /**
     * @return the set of all assertions registered
     */
    static Set getAssertions(Category cat) {
        Set<Class> allAssertions = new HashSet<>();
        for (CustomAssertionDescriptor eh : assertions.values()) {
            if (cat.equals(eh.getCategory())) {
                allAssertions.add(eh.getAssertion());
            }
        }
        return allAssertions;
    }

    private static Map<String, CustomAssertionDescriptor> assertions = new HashMap<>();
}