package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Logger;

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
    protected static final Logger logger = Logger.getLogger(AbstractLocator.class.getName());


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
     * @param intf  the interface clas
     * @param impl the implementation class
     */
    protected void addPair(Class intf, Class impl) {
        nameClassSet.add(new ClassPair(intf, impl));
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
                ClassPair nc = (ClassPair)it.next();
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
        private boolean matches(ClassPair pair, Locator.Template template) {
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
    final class ClassPair extends Item {
        private Class itemInterface;
        private Class itemImplementation;
        private String id;

        /**
         * Create an item.
         * @param intf the interface.
         * @param impl object implementaiton to register
         */
        public ClassPair(Class intf, Class impl) {
            this(null, intf, impl);
        }

        /**
         * Create an item; full constructor.
         * @param id the item id. may be null
         * @param intf the interface.
         * @param impl object implementaiton to register
         */
        public ClassPair(String id, Class intf, Class impl) {
            if (impl == null || intf == null)
                throw new IllegalArgumentException();
            this.id = id;
            this.itemInterface = intf;
            this.itemImplementation = impl;
        }


        /**
         * Tests whether this item can produce object
         * of class c.
         */
        public boolean instanceOf(Class c) {
            return c.isAssignableFrom(itemInterface);
        }

        /**
         * Get instance of registered object.
         * @return the instance of the object.
         */
        public Object getInstance() {
            try {
                synchronized (instancesCache) {
                    Object instance;
                    WeakReference ref = (WeakReference)instancesCache.get(this);
                    if (ref == null) {
                        Object o = createInstance();
                        logger.finest("Cache lookup failed '"+itemInterface.getName()+"'" +
                          "\nin " + instancesCache +
                          "\ncreating new instance  " + o);
                        instancesCache.put(this, new WeakReference(o));
                        instance = o;
                    } else {
                        Object o = ref.get();
                        if (o == null) {
                            logger.finest("Cache lookup failed '"+itemInterface.getName()+"', removing weak reference - type " + itemImplementation.getName());
                            instancesCache.remove(this);
                            return getInstance();
                        }
                        logger.finest("Cache lookup success, returns instance " +o);
                        instance = o;
                    }
                    return instance;
                }
            } catch (InstantiationException e) {
                throw new RuntimeException("error instantiating " + itemImplementation, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("error instantiating " + itemImplementation, e);
            }
        }

        private Object createInstance()
          throws InstantiationException, IllegalAccessException {
            Object o = itemImplementation.newInstance();
            if (o instanceof ObjectFactory) {
                return ((ObjectFactory)o).getInstance(getType(), null);
            }
            return o;
        }

        /**
         * An identity of the item.
         * @return string representing the item
         */
        public String getId() {
            return id;
        }

        /** The class of this item.
         * @return the correct class
         */
        public Class getType() {
            return itemInterface;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassPair)) return false;

            final ClassPair classPair = (ClassPair)o;

            if (id != null ? !id.equals(classPair.id) : classPair.id != null) return false;
            if (!itemImplementation.equals(classPair.itemImplementation)) return false;
            if (!itemInterface.equals(classPair.itemInterface)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = itemInterface.hashCode();
            result = 29 * result + itemImplementation.hashCode();
            result = 29 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
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
        sb.append("------------ Items -----------------\n");
        Locator.Matches matches = lookup(new Locator.Template());
        Iterator it = matches.allItems().iterator();
        while (it.hasNext()) {
            sb.append(it.next()).append("\n");
        }
        sb.append("------------ Instances -------------\n");
        it = instancesCache.keySet().iterator();
        while (it.hasNext()) {
            Item key = (Locator.Item)it.next();
            WeakReference wr = (WeakReference)instancesCache.get(key);
            Object o = wr.get();
            String s = " Reference cleared (null) ";
            if (o != null) {
                s = o.getClass().getName();
            }
            sb.append("Type : "+key.getType()+ " "+s).append("\n");
        }

        return sb.toString();
    }


    private Map instancesCache = new HashMap();
    private Set nameClassSet = new HashSet();

}
