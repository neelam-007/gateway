package com.l7tech.util.locator;

import com.l7tech.util.Locator;
import com.l7tech.util.WeakSet;

import java.util.*;

/**
 * This class provides the default way of how to store (Class, Object)
 * pairs in the locators. It offers protected methods for subclasses
 * to register the pairs.
 *
 * *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class AbstractLocator extends Locator {

    /** The general lookup method.
     * @param template a template describing the services to look for
     * @return an object containing the matching results
     */
    public Locator.Matches lookup(Locator.Template template) {
        return new NameClassMatches(template);
    }

    /**
     * Add the name, class Pair. This method is offered to
     * subclasses.
     * @param name the name. may be null
     * @param clazz the clazz register
     */
    protected void addPair(String name, Class clazz) {
        nameClassSet.add(new NameClassPair(name, clazz));
    }

    /**
     * The default concrete implementation of the <code>Matches</code>
     * backed by name, class pair.
     */
    final class NameClassMatches extends Matches {
        NameClassMatches(Locator.Template template) {
            this.template = template;
        }

        /**
         * Get all instances in the result.
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

        /** Get all classes represented in the result.
         * That is, the set of concrete classes
         * used by instances present in the result.
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

        /** Get all registered items.
         * This should include all pairs of instances together
         * with their classes, IDs, and so on.
         * @return collection of {@link Locator.Item}
         */
        public Collection allItems() {
            if (allItems != null) {
                return allItems;
            }
            allItems = new ArrayList();
            Iterator it = AbstractLocator.this.nameClassSet.iterator();
            while (it.hasNext()) {
                NameClassPair nc = (NameClassPair)it.next();
                if (matches(nc, this.template)) {
                    allItems.add(nc);
                }
            }
            allItems = Collections.unmodifiableCollection(allItems);
            return allItems;
        }

        /**
         * determine whether the name class pair matches the template.
         * @param pair the name class pair
         * @param template the template to check against
         * @return true if match, false otherwise
         */
        private boolean matches(NameClassPair pair, Locator.Template template) {
            Object lookupInstance = template.getInstance();
            Class type = template.getType();
            String id = template.getId();
            boolean matches = true;

            if (lookupInstance != null) {
                matches = lookupInstance.equals(pair.getInstance());
                if (!matches) return false;
            }
            if (template.getType() != null) {
                matches = matches && type.isAssignableFrom(pair.getType());
            }
            if (id != null) {
                matches = matches && id.equals(pair.getId());
            }
            return matches;
        }

        private Set allClasses = null;
        private Collection allItems = null;
        private Collection allInstances = null;
        private final Locator.Template template;

    }

    /**
     * Instance of one item representing an object.
     */
    final class NameClassPair extends Item {
        private Class cls;
        private String name;
        private Object instance;

        /**
         * Create an item.
         * @param name the name. may be null
         * @param clazz object to register
         */
        public NameClassPair(String name, Class clazz) {
            if (clazz == null) throw new NullPointerException();
            this.cls = clazz;
            this.name = name;
        }

        /**
         * Tests whether this item can produce object
         * of class c.
         */
        public boolean instanceOf(Class c) {
            return c.isAssignableFrom(cls);
        }

        /**
         * Get instance of registered object.
         * @return the instance of the object.
         */
        public Object getInstance() {
            if (instance != null) {
                return instance;
            }

            try {
                instance = cls.newInstance();
                WeakSet ws = (WeakSet)instancesCache.get(getType());
                if (ws == null) {
                    ws = new WeakSet();
                    instancesCache.put(getType(), ws);
                }
                ws.add(instance);
                return instance;
            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new RuntimeException("error instantiating " + cls, e);
            }

        }

        public boolean equals(Object o) {
            if (!(o instanceof NameClassPair)) {
                return false;
            }

            return o.hashCode() == o.hashCode();
        }

        /**
         * Computes hashcod. The hashcode is cached.
         * @return hashcode
         */
        public int hashCode() {
            if (hashCode != 0) {
                return hashCode;
            }

            int result = 17;
            result = 37 * result + (null == name ? 0 : name.intern().hashCode());
            result += 37 * result + (cls.hashCode());
            hashCode = result;

            return hashCode;
        }

        /**
         * An identity of the item.
         * @return string representing the item
         */
        public String getId() {
            if (name != null) return name;

            return "ID[" + cls.toString() + "]";
        }

        /** The class of this item.
         * @return the correct class
         */
        public Class getType() {
            return cls;
        }

        private int hashCode = 0;
    }

    /**
     * Returns a string representation of the object, that
     * is the list of <code>Locator.Item</code> isntances
     * that are registered.
     *
     * @return  a string representation of the object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        Locator.Matches matches = lookup(new Locator.Template());
        Iterator it = matches.allInstances().iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append("\n");
        }
        return sb.toString();
    }


    private Map instancesCache = new HashMap();
    private Set nameClassSet = new HashSet();

}
