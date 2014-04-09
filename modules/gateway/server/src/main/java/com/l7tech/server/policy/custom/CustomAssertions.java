package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
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

    static CustomAssertionDescriptor unregister(final CustomAssertionDescriptor eh) {
        logger.fine("unregistering " + eh);
        return assertions.remove(eh.getName());
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
    static Set<CustomAssertionDescriptor> getAllDescriptors() {
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        for (CustomAssertionDescriptor cd : assertions.values()) {
            descriptors.add(cd);
        }
        return descriptors;
    }

    /**
     * @return the set of all descriptors
     */
    static Set<CustomAssertionDescriptor> getDescriptors() {
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        descriptors.addAll(assertions.values());
        return descriptors;
    }


    /**
     * @return the set of all assertions registered
     */
    static Set<CustomAssertionDescriptor> getDescriptors( Category cat) {
        Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        for (CustomAssertionDescriptor cd : assertions.values()) {
            if (cd.hasCategory(cat)) {
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
    static Set<Class> getAssertions() {
        Set<Class> allAssertions = new HashSet<>();
        for (CustomAssertionDescriptor eh : assertions.values()) {
            allAssertions.add(eh.getAssertion());
        }
        return allAssertions;
    }

    /**
     * @return the set of all assertions registered
     */
    static Set<Class> getAssertions(Category cat) {
        Set<Class> allAssertions = new HashSet<>();
        for (CustomAssertionDescriptor eh : assertions.values()) {
            if (eh.hasCategory(cat)) {
                allAssertions.add(eh.getAssertion());
            }
        }
        return allAssertions;
    }

    /**
     * @return the Serializer object, implementing {@link CustomEntitySerializer} for the specified class name,
     * or {@code null} if the specified class name is not registered.
     */
    static CustomEntitySerializer getExternalEntitySerializer(final String extEntitySerializerClassName) {
        try {
            for (CustomAssertionDescriptor eh : assertions.values()) {
                for (final Class<? extends CustomEntitySerializer> extEntitySerializerClass : eh.getExternalEntitySerializers()) {
                    if (extEntitySerializerClass.getName().equals(extEntitySerializerClassName)) {
                        return extEntitySerializerClass.newInstance();
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            logger.log(Level.WARNING, "Failed to instantiate external entity serializer with class: \"" + extEntitySerializerClassName + "\"");
        }

        return null;
    }

    private static Map<String, CustomAssertionDescriptor> assertions = new ConcurrentHashMap<>();
}