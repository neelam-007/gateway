package com.l7tech.common.util;

import com.l7tech.common.locator.Locators;
import com.l7tech.common.locator.PropertiesLocator;

import java.util.*;


/**
 * A general service lookup permitting clients to find instances of services,
 * service proxy objects, client side stubs etc.
 * <p/>
 * The services are  looked up by interface or super class. This allows to have
 * the client of the service to know what calls to make to gain access to the
 * service. But how (or even if) the proxy communicates with the service itself
 * is completely up to the proxy and the service from which it comes.
 * <p/>
 * <p/>
 * Here is a sample showing basic <code>Locator</code> usage.
 * <p/>
 * <blockquote><pre>
 * // in the applicaiton lookup a component that implements
 * // the UserManager interface
 * // lookup the component by name
 * UserManager um =
 *      (UserManager)Locator.lookup(UserManager.class);
 * </pre></blockquote>
 * <p/>
 * The class is inspired by JINI(tm) with the differences that methods are
 * not throwing exceptions, and is mostly concentrated on the lookup, not
 * on the registration.
 *
 * @author <a href="mailto:{acruise,emarceta}@layer7-tech.com">Alex Cruise, Emil Marceta</a>
 * @version 1.0
 */

public abstract class Locator {
    /**
     * A dummy locator that never returns any results.
     */
    public static final Locator EMPTY = new Empty();
    /**
     * default instance
     */
    private static Locator defaultLocator;

    /**
     * Static method to obtain the global locator.
     *
     * @return the global lookup in the system
     */
    public static synchronized Locator getDefault() {
        if (defaultLocator != null) {
            return defaultLocator;
        }

        String className =
          System.getProperty("com.l7tech.common.locator");

        try {
            if (className != null) {
                Class c = Class.forName(className);
                defaultLocator = (Locator)c.newInstance();
                return defaultLocator;
            }
        } catch (Exception ex) {
            // the lookup has not be found, do not use any error managers
            // as they may not be ready
            ex.printStackTrace();
        }
        // OK, none specified (successfully) in a system property.
        // Try PropertiesLookup as a default.
        String res =
          System.getProperty("com.l7tech.common.locator.properties");
        if (res == null) {
            res = PropertiesLocator.DEFAULT_PROPERTIES;
        }
        defaultLocator = Locators.propertiesLocator(res, null);

        return defaultLocator;
    }

    /**
     * Static method that sets the global locator
     *
     * @param l the new locator
     */
    public static synchronized void setDefault(Locator l) {
        defaultLocator = l;
    }

    /**
     * Empty constructor for use by subclasses.
     */
    protected Locator() {
    }

    /**
     * Look up an object matching a given interface.
     * This is the simplest method to use.
     * If more than one object matches, one will be returned arbitrarily.
     * The template class may be a class or interface; the instance is
     * guaranteed to be assignable to it.
     *
     * @param clazz class of the object we are searching for
     * @return an object implementing the given class or <code>null</code> if no such
     *         implementation is found
     */
    public Object lookup(Class clazz) {
        Matches res = lookup(new Template(clazz));
        Iterator it = res.allItems().iterator();
        return it.hasNext() ? ((Item)it.next()).getInstance() : null;
    }

    /**
     * The general lookup method.
     *
     * @param template a template describing the services to look for
     * @return an object containing the matching results
     */
    public abstract Matches lookup(Template template);

    /**
     * This class may grow in the future, but for now, it is*
     * enough to start with something simple.
     * Template defining a pattern to filter instances by.
     */
    public static final class Template {
        /**
         * cached hash code
         */
        private int hashCode;
        /**
         * type of the service
         */
        private Class type;
        /**
         * identity to search for
         */
        private String id;
        /**
         * instance to search for
         */
        private Object instance;

        /**
         * General template to find all possible instances.
         */
        public Template() {
            this(null);
        }

        /**
         * Create a simple template matching by class.
         *
         * @param type the class of service we are looking for (subclasses will match)
         */
        public Template(Class type) {
            this(type, null, null);
        }

        /**
         * Constructor to create new template.
         *
         * @param type     the class of service we are looking for or <code>null</code> to leave unspecified
         * @param id       the ID of the item/service we are looking for or <code>null</code> to leave unspecified
         * @param instance a specific known instance to look for or <code>null</code> to leave unspecified
         */
        public Template(Class type, String id, Object instance) {
            this.type = type == null ? Object.class : type;
            this.id = id;
            this.instance = instance;
        }

        /**
         * Get the class (or superclass or interface) to search for.
         * If it was not specified in the constructor, <code>Object</code> is used and
         * this will match any instance.
         *
         * @return the class to search for
         */
        public Class getType() {
            return type;
        }

        /**
         * Get the identifier being searched for, if any.
         *
         * @return the ID or <code>null</code>
         * @see Locator.Item#getId
         */
        public String getId() {
            return id;
        }

        /**
         * Get the specific instance being searched for, if any.
         * Most useful for finding an <code>Item</code> when the instance
         * is already known.
         *
         * @return the object to find or <code>null</code>
         */
        public Object getInstance() {
            return instance;
        }


        /* Computes hashcode for this template. The hashcode is cached.
         * @return hashcode
         */
        public int hashCode() {
            if (hashCode != 0) {
                return hashCode;
            }

            hashCode =
              (type == null ? 1 : type.hashCode()) +
              (id == null ? 2 : id.hashCode()) +
              (instance == null ? 3 : instance.hashCode());

            return hashCode;
        }

        /* Checks whether two templates represent the same query.
         * @param obj another template to check
         * @return true if so, false otherwise
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof Template)) {
                return false;
            }
            return hashCode() == obj.hashCode();
        }

        /* for debugging */
        public String toString() {
            return "Locator.Template[type=" + type + ",id=" + id + ",instance=" + instance + "]";
        }
    }

    /**
     * Matches of a lookup request.
     * Allows access to all matching instances at once.
     * Also permits listening to changes in the result.
     */
    public static abstract class Matches {

        /**
         * Get all instances in the result.
         *
         * @return collection of all instances
         */
        public Collection allInstances() {

            if (allInstances != null) {
                return allInstances;
            }

            allInstances = new ArrayList(allItems().size());
            Iterator it = allItems().iterator();
            while (it.hasNext()) {
                Item item = (Item)it.next();
                Object obj = item.getInstance();
                if (obj != null) {
                    allInstances.add(obj);
                }
            }
            return allInstances;
        }

        /**
         * Get all classes represented in the result.
         * That is, the set of concrete classes
         * used by instances present in the result.
         *
         * @return set of <code>Class</code> objects
         */
        public Set allClasses() {
            if (allClasses != null) {
                return allClasses;
            }

            allClasses = new HashSet();

            Iterator it = allItems().iterator();
            while (it.hasNext()) {
                Item item = (Item)it.next();
                Class clazz = item.getType();
                if (clazz != null) {
                    allClasses.add(clazz);
                }
            }
            return allClasses;
        }

        /**
         * Get all registered Items that match the criteria.
         * This should include all pairs of instances together
         * with their classes, IDs, and so on.
         *
         * @return collection of {@link Locator.Item}
         */
        public abstract Collection allItems();

        private Collection allInstances = null;
        private Set allClasses = null;
    }

    /**
     * A single item in a lookup matches result.
     * This wrapper provides unified access to not just the instance,
     * but its class, a possible identifier, and so on.
     */
    public static abstract class Item {
        /**
         * Get the instance itself.
         *
         * @return the instance or null if the instance cannot be created
         */
        public abstract Object getInstance();

        /**
         * Get the implementing class of the instance.
         *
         * @return the class of the item
         */
        public abstract Class getType();


        /**
         * Get the identifier for the item.
         * This identifier should uniquely represent the item
         * within its containing lookup
         *
         * @return a string ID of the item
         */
        public abstract String getId();

        /* for debugging */
        public String toString() {
            return "Locator.Item[type=" +
              getType() + ",id=" +
              (getId() == null ? "null" : getId()) /*+
              ",instance=" + getInstance() */ + "]";
        }

    }


    /**
     * Implementation of the default 'no-op' lookup
     */
    private static final class Empty extends Locator {
        Empty() {
        }

        private static final Matches NO_RESULT = new Matches() {
            /**
             * Get all registered Items that match the criteria.
             * This should include all pairs of instances together
             * with their classes, IDs, and so on.
             *
             * @return collection of {@link com.l7tech.common.util.Locator.Item}
             */
            public Collection allItems() {
                return Collections.EMPTY_LIST;
            }
        };

        /**
         * Locates an object of given interface. This is the simplest lookup
         * method.
         *
         * @param clazz class of the object we are searching for
         * @return the object implementing given class or null if no such
         *         has been found
         */
        public Object lookup(Class clazz) {
            return null;
        }

        /**
         * The general lookup method.
         *
         * @param template the template describing the services we are looking for
         * @return object containing the results
         */
        public Matches lookup(Template template) {
            return NO_RESULT;
        }
    }

    /**
     * recycle the current locator
     */
    public static synchronized void recycle() {
        defaultLocator = null;
    }

}
