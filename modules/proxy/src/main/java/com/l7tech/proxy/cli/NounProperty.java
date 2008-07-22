/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.TextUtils;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Represents a configurable property of a configuration noun.
 * Simple properties will just use this class, and set and get with reflection.
 * More complex properties will subclass NounProperty for more complex behavior.
 */
class NounProperty extends Word {
    private final String methodName;
    private final Object targetObject;
    private Method accessor;
    private Method mutator;
    private Class type;

    public NounProperty(Object targetObject, String name, String methodName, String desc) {
        super(name, desc);
        this.targetObject = targetObject;
        this.methodName = methodName;
        setMinAbbrev(3);
    }

    /**
     * Print the human-displayed value of this property.
     *
     * @param out          the PrintStream to print to.  Must not be null.
     * @param singleLine   true if the display needs to be very short, a single line at most
     */
    public void printValue(PrintStream out, boolean singleLine) {
        out.print(getValue());
        if (!singleLine) out.println();
    }

    /**
     * Attempt to get the accessor method for our target object.
     *
     * @return the accessor method.  Never null.
     * @throws RuntimeException if an accessor method couldn't be found.
     */
    protected Method getAccessor() {
        if (accessor == null) {
            try {
                try {
                    accessor = targetObject.getClass().getMethod("get" + methodName, new Class[0]);
                } catch (NoSuchMethodException e) {
                    accessor = targetObject.getClass().getMethod("is" + methodName, new Class[0]);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        return accessor;
    }

    /**
     * Attempt to get the data type of this property.
     *
     * @return the data type.  Never null.
     * @throws RuntimeException if an accessor couldn't be found or a data type couldn't be determined.
     */
    protected Class getType() {
        if (type == null) {
            Class c = getAccessor().getReturnType();
            if (c == null || c.equals(void.class)) throw new RuntimeException("Accessor returns void");
            type = c;
        }
        return type;
    }

    /**
     * Attempt to get the mutator method for our target object.
     *
     * @return the mutator method.  Never null.
     * @throws RuntimeException if a mutator method couldn't be found.
     */
    protected Method getMutator() {
        if (mutator == null) {
            try {
                mutator = targetObject.getClass().getMethod("set" + methodName, new Class[]{getType()});
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return mutator;
    }

    /**
     * Attempt to get the value by invoking our accessor method on our target object.
     *
     * @return the Object returned from calling our accessor on our target object, which may be null.
     * @throws RuntimeException if the accessor throws an exception.
     */
    protected Object getValue() {
        try {
            return getAccessor().invoke(targetObject, new Object[0]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to read property " + getName() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Attempt to set the value by invoking our mutator method on our target object.
     *
     * @param value  the new value to set.  May be null.  Must be an instance of type.
     * @throws ClassCastException if the given object wouldn't fit into our mutator.
     */
    protected void setValue(Object value) throws CommandException {
        try {
            getMutator().invoke(targetObject, new Object[] {value});
        } catch (IllegalArgumentException e) {
            throw new ClassCastException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // shouldn't happen
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to set property " + getName() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }



    /**
     * Attempt to set the value by making sense of the specified string array.
     * This method will work only if args contains exactly one String.  It attempts to convert the
     * string into the appropriate type and then call the mutator.  Subclasses may override this
     * method to do something more complex, as needed.
     *
     * @param args  remainder of a command line specifying what to set.
     */
    public void set(String[] args) throws CommandException {
        try {
            Method valueOfString = null;
            Constructor ctorFromString = null;

            final Class type = getType();
            if (!type.isPrimitive()) {
                try {
                    valueOfString = type.getMethod("valueOf", new Class[] { String.class });
                    if (!Modifier.isStatic(valueOfString.getModifiers()))
                        throw new IllegalStateException(type.getName() + " contains a non-static valueOf() method"); // programming error
                } catch (NoSuchMethodException e) {
                    ctorFromString = type.getConstructor(new Class[] { String.class });
                }
            }

            if (args == null || args.length < 1)
                throw new CommandException(getName() + " requires a single value of type " + ClassUtils.getClassName(type));
            final String stringVal;
            if (args.length > 1)
                stringVal = TextUtils.join("", args).toString();
            else
                stringVal = args[0];

            final Object objectVal;
            if (type.isPrimitive()) {
                if (type.equals(boolean.class))
                    //noinspection UnnecessaryBoxing
                    objectVal = Boolean.valueOf(isTrue(stringVal));
                else if (type.equals(char.class)) {
                    if (stringVal.length() < 1)
                        throw new CommandException(getName() + " requires a single character value"); // can't happen
                    objectVal = new Character(stringVal.charAt(0));
                } else if (type.equals(byte.class))
                    objectVal = new Byte(stringVal);
                else if (type.equals(short.class))
                    objectVal = new Short(stringVal);
                else if (type.equals(int.class))
                    objectVal = new Integer(stringVal);
                else if (type.equals(long.class))
                    objectVal = new Long(stringVal);
                else if (type.equals(float.class))
                    objectVal = new Float(stringVal);
                else if (type.equals(double.class))
                    objectVal = new Double(stringVal);
                else
                    throw new IllegalStateException("Unknown primitive type " + type.getName()); // void was ruled out earlier, in getType()

            } else if (valueOfString != null) {
                objectVal = valueOfString.invoke(null, new Object[] { stringVal });
            } else if (ctorFromString != null) {
                objectVal = ctorFromString.newInstance(new Object[] { stringVal });
            } else
                throw new IllegalStateException("No valueOf or String ctor");  // can't happen

            setValue(objectVal);

        } catch (NoSuchMethodException e) {
            // We tried our best, but you'll need to make a subclass for your property.
            throw new CommandException("Property " + getName() + " cannot be set.");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // can't happen
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to set property " + getName() + ": " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to set property " + getName() + ": " + ExceptionUtils.getMessage(e), e);
        } catch (NumberFormatException e) {
            throw new CommandException("Property " + getName() + " must be a valid " + ClassUtils.getClassName(getType()) + " number");
        }
    }

    /**
     * Evaluate a boolean value.
     * @return true if this value looks like "1", "t", "true", "y" or "yes"; false if it looks like "0", "f", "false", "n", or "no"
     * @throws CommandException if it didn't look like anything we recognize
     */
    private boolean isTrue(String stringVal) throws CommandException {
        String s = stringVal.toLowerCase();
        if ("1".equals(s) || "t".equals(s) || "true".equals(s) || "y".equals(s) || "yes".equals(s)) return true;
        if ("0".equals(s) || "f".equals(s) || "false".equals(s) || "n".equals(s) || "no".equals(s)) return false;
        throw new CommandException("Property " + getName() + " can be set to 'true' or 'false'.");
    }

    /**
     * Remove this property value.  This method just tries to set it to null.  Subclasses can override this to
     * take more specific action.
     *
     * @throws CommandException if this property is not deletable.
     */
    public void delete() throws CommandException {
        try {
            setValue(null);
        } catch (IllegalArgumentException e) {
            throw new CommandException("Property " + getName() + " can't be deleted.", e);
        }
    }
}
