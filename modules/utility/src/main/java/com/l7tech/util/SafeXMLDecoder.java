/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by CA Layer 7 Technologies to support class, constructor, and field whitelisting.
 */

package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.beans.ExceptionListener;
import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Based in part on sources from the Apache Harmony project.  The Apache source
 * file header is included below:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An XMLDecoder that is very strict about the constructors and methods it will invoke.
 * <p/>
 * <code>XMLDecoder</code> reads objects from xml created by
 * <code>XMLEncoder</code>.
 * <p>
 * The API is similar to <code>ObjectInputStream</code>.
 * </p>
 */
public class SafeXMLDecoder implements Closeable {
    private static final Logger logger = Logger.getLogger(SafeXMLDecoder.class.getName());

    public static final String PROP_DISABLE_FILTER = "com.l7tech.util.SafeXMLDecoder.disableAllFiltering";

    private static final String SYSTEM_ID_XMLOBJ = "http://layer7tech.com/ns/xmlobj";

    private static boolean isDisableAllFiltering() {
        return SyspropUtil.getBoolean(PROP_DISABLE_FILTER, false);
    }

    public static class ClassFilterException extends SecurityException {
        public ClassFilterException(String s) {
            super(s);
        }
    }

    public static class ClassNotPermittedException extends ClassFilterException {
        private final String className;

        public ClassNotPermittedException(String className) {
            super("Class not permitted for XML decoding: " + className);
            this.className = className;
        }

        /**
         * @return the name of the class that was not whitelisted
         */
        public String getClassName() {
            return className;
        }
    }

    public static class MethodNotPermittedException extends ClassFilterException {
        private final Method method;


        public MethodNotPermittedException(Method m) {
            super("Method not permitted for XML decoding: " + m);
            this.method = m;
        }

        /**
         * @return the method that was not permitted.
         */
        public Method getMethod() {
            return method;
        }
    }

    public static class ConstructorNotPermittedException extends ClassFilterException {
        private final Constructor constructor;

        public ConstructorNotPermittedException(Constructor constructor) {
            super("Constructor not permitted for XML decoding: " + constructor);
            this.constructor = constructor;
        }

        /**
         * @return the constructor that was not permitted.
         */
        public Constructor getConstructor() {
            return constructor;
        }
    }

    private final ClassFilter classFilter;
    private ClassLoader defaultClassLoader = null;

    private static class DefaultExceptionListener implements ExceptionListener {

        public void exceptionThrown(Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Continue...");
        }
    }

    private class SAXHandler extends DefaultHandler {

        boolean inJavaElem = false;

        HashMap<String, Object> idObjMap = new HashMap<>();

        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException {
            if (!inJavaElem) {
                return;
            }
            if (readObjs.size() > 0) {
                Elem elem = readObjs.peek();
                if (elem.isBasicType) {
                    String str = new String(ch, start, length);
                    elem.methodName = elem.methodName == null ? str
                        : elem.methodName + str;
                }
            }
        }

        @SuppressWarnings("nls")
        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (!inJavaElem) {
                if ("java".equals(qName)) {
                    inJavaElem = true;
                } else {
                    listener.exceptionThrown(new Exception(
                        MessageFormat.format("Unknown root element: {0}", qName)));
                }
                return;
            }

            if ("object".equals(qName)) {
                startObjectElem(attributes);
            } else if ("array".equals(qName)) {
                startArrayElem(attributes);
            } else if ("void".equals(qName)) {
                startVoidElem(attributes);
            } else if ("boolean".equals(qName) || "byte".equals(qName)
                || "char".equals(qName) || "class".equals(qName)
                || "double".equals(qName) || "float".equals(qName)
                || "int".equals(qName) || "long".equals(qName)
                || "short".equals(qName) || "string".equals(qName)
                || "null".equals(qName)) {
                startBasicElem(qName, attributes);
            }
        }

        @SuppressWarnings("nls")
        private void startObjectElem(Attributes attributes) {
            Elem elem = new Elem();
            elem.isExpression = true;
            elem.id = attributes.getValue("id");
            elem.idref = attributes.getValue("idref");
            if (elem.idref == null) {
                obtainTarget(elem, attributes);
                obtainMethod(elem, attributes);
            }

            readObjs.push(elem);
        }

        private void obtainTarget(Elem elem, Attributes attributes) {
            String className = attributes.getValue("class");
            if (className != null) {
                if (!classFilter.permitClass(className))
                    throw new ClassNotPermittedException(className);
                try {
                    elem.target = classForName(className);
                } catch (ClassNotFoundException e) {
                    listener.exceptionThrown(e);
                }
            } else {
                Elem parent = latestUnclosedElem();
                if (parent == null) {
                    elem.target = owner;
                    return;
                }
                elem.target = execute(parent);
            }
        }

        @SuppressWarnings("nls")
        private void obtainMethod(Elem elem, Attributes attributes) {
            elem.methodName = attributes.getValue("method");
            if (elem.methodName != null) {
                return;
            }

            elem.methodName = attributes.getValue("property");
            if (elem.methodName != null) {
                elem.fromProperty = true;
                return;
            }

            elem.methodName = attributes.getValue("index");
            if (elem.methodName != null) {
                elem.fromIndex = true;
                return;
            }

            elem.methodName = attributes.getValue("field");
            if (elem.methodName != null) {
                elem.fromField = true;
                return;
            }

            elem.methodName = attributes.getValue("owner");
            if (elem.methodName != null) {
                elem.fromOwner = true;
                return;
            }

            elem.methodName = "new"; // default method name
        }

        @SuppressWarnings("nls")
        private Class<?> classForName(String className)
            throws ClassNotFoundException {
            switch (className) {
                case "boolean":
                    return Boolean.TYPE;
                case "byte":
                    return Byte.TYPE;
                case "char":
                    return Character.TYPE;
                case "double":
                    return Double.TYPE;
                case "float":
                    return Float.TYPE;
                case "int":
                    return Integer.TYPE;
                case "long":
                    return Long.TYPE;
                case "short":
                    return Short.TYPE;
                default:
                    if (!classFilter.permitClass(className))
                        throw new ClassNotPermittedException(className);
                    return Class.forName(className, true,
                        defaultClassLoader == null ? Thread.currentThread()
                            .getContextClassLoader() : defaultClassLoader);
            }
        }

        private void startArrayElem(Attributes attributes) {
            Elem elem = new Elem();
            elem.isExpression = true;
            elem.id = attributes.getValue("id"); 
            try {
                // find component class
                Class<?> compClass = classForName(attributes.getValue("class")); 
                String lengthValue = attributes.getValue("length");
                if (lengthValue != null) {
                    // find length
                    int length = Integer
                        .parseInt(attributes.getValue("length")); 
                    // execute, new array instance
                    elem.result = Array.newInstance(compClass, length);
                    elem.isExecuted = true;
                } else {
                    // create array without length attribute,
                    // delay the execution to the end,
                    // get array length from sub element
                    elem.target = compClass;
                    elem.methodName = "newArray";
                    elem.isExecuted = false;
                }
            } catch (Exception e) {
                listener.exceptionThrown(e);
            }
            readObjs.push(elem);
        }

        @SuppressWarnings("nls")
        private void startVoidElem(Attributes attributes) {
            Elem elem = new Elem();
            elem.id = attributes.getValue("id");
            obtainTarget(elem, attributes);
            obtainMethod(elem, attributes);
            readObjs.push(elem);
        }

        @SuppressWarnings("nls")
        private void startBasicElem(String tagName, Attributes attributes) {
            Elem elem = new Elem();
            elem.isBasicType = true;
            elem.isExpression = true;
            elem.id = attributes.getValue("id");
            elem.idref = attributes.getValue("idref");
            elem.target = tagName;
            readObjs.push(elem);
        }

        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            if (!inJavaElem) {
                return;
            }
            if ("java".equals(qName)) { 
                inJavaElem = false;
                return;
            }
            // find the elem to close
            Elem toClose = latestUnclosedElem();
            // make sure it is executed
            execute(toClose);
            // set to closed
            toClose.isClosed = true;
            // pop it and its children
            while (readObjs.pop() != toClose) {
                //
            }

            if (toClose.isExpression) {
                // push back expression
                readObjs.push(toClose);
            }
        }

        private Elem latestUnclosedElem() {
            for (int i = readObjs.size() - 1; i >= 0; i--) {
                Elem elem = readObjs.get(i);
                if (!elem.isClosed) {
                    return elem;
                }
            }
            return null;
        }

        private Object execute(Elem elem) {
            if (elem.isExecuted) {
                return elem.result;
            }

            // execute to obtain result
            try {
                if (elem.idref != null) {
                    elem.result = idObjMap.get(elem.idref);
                } else if (elem.isBasicType) {
                    elem.result = executeBasic(elem);
                } else {
                    elem.result = executeCommon(elem);
                }
            } catch (Exception e) {
                listener.exceptionThrown(e);
            }

            // track id
            if (elem.id != null) {
                idObjMap.put(elem.id, elem.result);
            }

            elem.isExecuted = true;
            return elem.result;
        }

        @SuppressWarnings("nls")
        private Object executeCommon(Elem elem) throws Exception {
            // pop args
            ArrayList<Object> args = new ArrayList<>(5);
            while (readObjs.peek() != elem) {
                Elem argElem = readObjs.pop();
                args.add(0, argElem.result);
            }
            // decide method name
            String method = elem.methodName;
            if (elem.fromProperty) {
                method = (args.size() == 0 ? "get" : "set")
                    + capitalize(method);
            }
            if (elem.fromIndex) {
                Integer index = Integer.valueOf(method);
                args.add(0, index);
                method = args.size() == 1 ? "get" : "set";
            }
            if (elem.fromField) {
                Field f = ((Class<?>) elem.target).getField(method);
                return (new SafeExpression(classFilter, f, "get", new Object[] { null })).getValue();
            }
            if (elem.fromOwner) {
                return owner;
            }

            if (elem.target == owner) {
                if ("getOwner".equals(method)) {
                    return owner;
                }
                Class<?>[] c = new Class[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    Object arg = args.get(i);
                    c[i] = (arg == null ? null: arg.getClass());
                }

                // Try actual match method
                try {

                    final Class<?> ownerClazz = owner.getClass();
                    Method m = ownerClazz.getMethod(method, c);
                    checkMethod(m);
                    return m.invoke(owner, args.toArray());
                } catch (NoSuchMethodException e) {
                    // Do nothing
                }

                // Find the specific method matching the parameter
                Method mostSpecificMethod = findMethod(
                    owner instanceof Class<?> ? (Class<?>) owner : owner
                        .getClass(), method, c);

                checkMethod(mostSpecificMethod);
                return mostSpecificMethod.invoke(owner, args.toArray());
            }

            // execute
            SafeExpression exp = new SafeExpression(classFilter, elem.target, method, args.toArray());
            return exp.getValue();
        }

        private Method findMethod(Class<?> clazz, String methodName,
                                  Class<?>[] clazzes) throws Exception {
            Method[] methods = clazz.getMethods();
            ArrayList<Method> matchMethods = new ArrayList<>();

            // Add all matching methods into a ArrayList
            for (Method method : methods) {
                if (!methodName.equals(method.getName())) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != clazzes.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    boolean isNull = (clazzes[i] == null);
                    boolean isPrimitive = isPrimitiveWrapper(clazzes[i], parameterTypes[i]);
                    boolean isAssignable = !isNull && parameterTypes[i].isAssignableFrom(clazzes[i]);
                    if ( isNull || isPrimitive || isAssignable ) {
                        continue;
                    }
                    match = false;
                }
                if (match) {
                    matchMethods.add(method);
                }
            }

            int size = matchMethods.size();
            if (size == 1) {
                // Only one method matches, just invoke it
                return matchMethods.get(0);
            } else if (size == 0) {
                // Does not find any matching one, throw exception
                throw new NoSuchMethodException(MessageFormat.format(
                    "No method with name {0} is found", methodName));
            }

            // There are more than one method matching the signature
            // Find the most specific one to invoke
            MethodComparator comparator = new MethodComparator(methodName,
                clazzes);
            Method chosenOne = matchMethods.get(0);
            matchMethods.remove(0);
            int methodCounter = 1;
            for (Method method : matchMethods) {
                int difference = comparator.compare(chosenOne, method);
                if (difference > 0) {
                    chosenOne = method;
                    methodCounter = 1;
                } else if (difference == 0) {
                    methodCounter++;
                }
            }
            if (methodCounter > 1) {
                // if 2 methods have same relevance, throw exception
                throw new NoSuchMethodException(MessageFormat.format(
                    "Cannot decide which method to call to match {0}", methodName));
            }
            return chosenOne;
        }

        private boolean isPrimitiveWrapper(Class<?> wrapper, Class<?> base) {
            return (base == boolean.class) && (wrapper == Boolean.class)
                || (base == byte.class) && (wrapper == Byte.class)
                || (base == char.class) && (wrapper == Character.class)
                || (base == short.class) && (wrapper == Short.class)
                || (base == int.class) && (wrapper == Integer.class)
                || (base == long.class) && (wrapper == Long.class)
                || (base == float.class) && (wrapper == Float.class)
                || (base == double.class) && (wrapper == Double.class);
        }

        private String capitalize(String str) {
            StringBuilder buf = new StringBuilder(str);
            buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
            return buf.toString();
        }

        @SuppressWarnings("nls")
        private Object executeBasic(Elem elem) throws Exception {
            String tag = (String) elem.target;
            String value = elem.methodName;

            switch (tag) {
                case "null":
                    return null;
                case "string":
                    return value == null ? "" : value;
                case "class":
                    return classForName(value);
                case "boolean":
                    return Boolean.valueOf(value);
                case "byte":
                    return Byte.valueOf(value);
                case "char":
                    return value.charAt(0);
                case "double":
                    return Double.valueOf(value);
                case "float":
                    return Float.valueOf(value);
                case "int":
                    return Integer.valueOf(value);
                case "long":
                    return Long.valueOf(value);
                case "short":
                    return Short.valueOf(value);
                default:
                    throw new Exception(MessageFormat.format("Unknown tag of basic type: {0}", tag));
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            listener.exceptionThrown(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            listener.exceptionThrown(e);
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            listener.exceptionThrown(e);
        }
    }

    private void checkMethod(Method method) {
        if (!classFilter.permitMethod(method))
            throw new MethodNotPermittedException(method);
    }

    private static class Elem {
        String id;

        String idref;

        boolean isExecuted;

        boolean isExpression;

        boolean isBasicType;

        boolean isClosed;

        Object target;

        String methodName;

        boolean fromProperty;

        boolean fromIndex;

        boolean fromField;

        boolean fromOwner;

        Object result;

    }

    private InputStream inputStream;

    private ExceptionListener listener;

    private Object owner;

    private Stack<Elem> readObjs = new Stack<>();

    private int readObjIndex = 0;

    private SAXHandler saxHandler = null;

    /**
     * Create a decoder to read from specified input stream.
     *
     * @param inputStream
     *            an input stream of xml
     */
    public SafeXMLDecoder(@NotNull ClassFilter classFilter, InputStream inputStream) {
        this(classFilter, inputStream, null, null, null);
    }

    /**
     * Create a decoder to read from specified input stream.
     *
     * @param inputStream
     *            an input stream of xml
     * @param owner
     *            the owner of this decoder
     */
    public SafeXMLDecoder(@NotNull ClassFilter classFilter, InputStream inputStream, Object owner) {
        this(classFilter, inputStream, owner, null, null);
    }

    /**
     * Create a decoder to read from specified input stream.
     *
     * @param inputStream
     *            an input stream of xml
     * @param owner
     *            the owner of this decoder
     * @param listener
     *            listen to the exceptions thrown by the decoder
     */
    public SafeXMLDecoder(@NotNull ClassFilter classFilter, InputStream inputStream, Object owner,
                          ExceptionListener listener) {
        this(classFilter, inputStream, owner, listener, null);
    }

    public SafeXMLDecoder(@NotNull ClassFilter classFilter, InputStream inputStream, @Nullable Object owner,
                          @Nullable ExceptionListener listener, @Nullable ClassLoader cl) {
        this.inputStream = inputStream;
        this.owner = owner;
        this.listener = listener != null ? listener : new DefaultExceptionListener();
        defaultClassLoader = cl;

        if (isDisableAllFiltering()) {
            logger.warning("SafeXMLDecoder security filtering disabled");
            classFilter = new ClassFilter() {
                @Override
                public boolean permitClass(@NotNull String classname) {
                    return true;
                }

                @Override
                public boolean permitConstructor(@NotNull Constructor<?> constructor) {
                    return true;
                }

                @Override
                public boolean permitMethod(@NotNull Method method) {
                    return true;
                }
            };
        }

        this.classFilter = classFilter;
    }

    /**
     * Close the input stream of xml data.
     */
    public void close() {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (Exception e) {
            listener.exceptionThrown(e);
        }
    }

    /**
     * Returns the exception listener.
     *
     * @return the exception listener
     */
    public ExceptionListener getExceptionListener() {
        return listener;
    }

    /**
     * Returns the owner of this decoder.
     *
     * @return the owner of this decoder
     */
    public Object getOwner() {
        return owner;
    }

    /**
     * Reads the next object.
     *
     * @return the next object
     * @exception ArrayIndexOutOfBoundsException
     *                if no more objects to read
     */
    @SuppressWarnings("nls")
    public Object readObject() {
        if (inputStream == null) {
            return null;
        }
        if (saxHandler == null) {
            saxHandler = new SAXHandler();
            try {
                InputSource input = new InputSource(inputStream);
                input.setSystemId(SYSTEM_ID_XMLOBJ);
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                xmlReader.setEntityResolver(new EntityResolver() {
                    @Override
                    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
                        String msg = "Document referred to an external entity with system id '" + systemId + "'";
                        throw new SAXException(msg);
                    }
                });
                xmlReader.setErrorHandler(new ErrorHandler() {
                    @Override
                    public void warning( final SAXParseException exception ) throws SAXException {}
                    @Override
                    public void error( final SAXParseException exception ) throws SAXException {
                        throw exception;
                    }
                    @Override
                    public void fatalError( final SAXParseException exception ) throws SAXException {
                        throw exception;
                    }
                });
                xmlReader.setContentHandler(saxHandler);
                xmlReader.parse(input);
            } catch (Exception e) {
                this.listener.exceptionThrown(e);
            }
        }

        if (readObjIndex >= readObjs.size()) {
            throw new ArrayIndexOutOfBoundsException("no more objects to read");
        }
        Elem elem = readObjs.get(readObjIndex);
        if (!elem.isClosed) {
            // bad element, error occurred while parsing
            throw new ArrayIndexOutOfBoundsException("no more objects to read");
        }
        readObjIndex++;
        return elem.result;
    }

    /**
     * Sets the exception listener.
     *
     * @param listener
     *            an exception listener
     */
    public void setExceptionListener(@Nullable ExceptionListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    /**
     * Sets the owner of this decoder.
     *
     * @param owner
     *            the owner of this decoder
     */
    public void setOwner(@Nullable Object owner) {
        this.owner = owner;
    }

    private static Class<?> getPrimitiveWrapper(Class<?> base) {
        Class<?> res = null;
        if (base == boolean.class) {
            res = Boolean.class;
        } else if (base == byte.class) {
            res = Byte.class;
        } else if (base == char.class) {
            res = Character.class;
        } else if (base == short.class) {
            res = Short.class;
        } else if (base == int.class) {
            res = Integer.class;
        } else if (base == long.class) {
            res = Long.class;
        } else if (base == float.class) {
            res = Float.class;
        } else if (base == double.class) {
            res = Double.class;
        }
        return res;
    }

    /**
     * Comparator to determine which of two methods is "closer" to the reference
     * method.
     */
    static class MethodComparator implements Comparator<Method> {
        static int INFINITY = Integer.MAX_VALUE;

        private String referenceMethodName;

        private Class<?>[] referenceMethodArgumentTypes;

        private final Map<Method, Integer> cache;

        public MethodComparator(String refMethodName, Class<?>[] refArgumentTypes) {
            this.referenceMethodName = refMethodName;
            this.referenceMethodArgumentTypes = refArgumentTypes;
            cache = new HashMap<>();
        }

        public int compare(Method m1, Method m2) {
            Integer norm1 = cache.get(m1);
            Integer norm2 = cache.get(m2);
            if (norm1 == null) {
                norm1 = getNorm(m1);
                cache.put(m1, norm1);
            }
            if (norm2 == null) {
                norm2 = getNorm(m2);
                cache.put(m2, norm2);
            }
            return (norm1 - norm2);
        }

        /**
         * Returns the norm for given method. The norm is the "distance" from
         * the reference method to the given method.
         *
         * @param m
         *            the method to calculate the norm for
         * @return norm of given method
         */
        private int getNorm(Method m) {
            String methodName = m.getName();
            Class<?>[] argumentTypes = m.getParameterTypes();
            int totalNorm = 0;
            if (!referenceMethodName.equals(methodName)
                || referenceMethodArgumentTypes.length != argumentTypes.length) {
                return INFINITY;
            }
            for (int i = 0; i < referenceMethodArgumentTypes.length; i++) {
                if (referenceMethodArgumentTypes[i] == null) {
                    // doesn't affect the norm calculation if null
                    continue;
                }
                if (referenceMethodArgumentTypes[i].isPrimitive()) {
                    referenceMethodArgumentTypes[i] = getPrimitiveWrapper(referenceMethodArgumentTypes[i]);
                }
                if (argumentTypes[i].isPrimitive()) {
                    argumentTypes[i] = getPrimitiveWrapper(argumentTypes[i]);
                }
                totalNorm += getDistance(referenceMethodArgumentTypes[i], argumentTypes[i]);
            }
            return totalNorm;
        }

        /**
         * Returns a "hierarchy distance" between two classes.
         *
         * @param clz1 first class
         * @param clz2 second class
         *            should be superclass or superinterface of clz1
         * @return hierarchy distance from clz1 to clz2, Integer.MAX_VALUE if
         *         clz2 is not assignable from clz1.
         */
        private static int getDistance(Class<?> clz1, Class<?> clz2) {
            Class<?> superClz;
            int superDist = INFINITY;
            if (!clz2.isAssignableFrom(clz1)) {
                return INFINITY;
            }
            if (clz1.getName().equals(clz2.getName())) {
                return 0;
            }
            superClz = clz1.getSuperclass();
            if (superClz != null) {
                superDist = getDistance(superClz, clz2);
            }
            if (clz2.isInterface()) {
                Class<?>[] interfaces = clz1.getInterfaces();
                int bestDist = INFINITY;
                for (Class<?> element : interfaces) {
                    int curDist = getDistance(element, clz2);
                    if (curDist < bestDist) {
                        bestDist = curDist;
                    }
                }
                if (superDist < bestDist) {
                    bestDist = superDist;
                }
                return (bestDist != INFINITY ? bestDist + 1 : INFINITY);
            }
            return (superDist != INFINITY ? superDist + 1 : INFINITY);
        }
    }
}

class SafeStatement {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final Logger logger = Logger.getLogger(SafeStatement.class.getName());

    private final ClassFilter classFilter;

    private Object target;

    private String methodName;

    private Object[] arguments;

    // cache used methods of specified target class to accelerate method search
    private static WeakHashMap<Class<?>, Method[]> cache = new WeakHashMap<>();

    public SafeStatement(ClassFilter classFilter, Object target, String methodName, Object[] arguments) {
        this.classFilter = classFilter;
        this.target = target;
        this.methodName = methodName;
        if (arguments != null) {
            this.arguments = arguments;
        } else {
            this.arguments = EMPTY_ARRAY;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Object theTarget = getTarget();
        String theMethodName = getMethodName();
        Object[] theArguments = getArguments();
        String targetVar = theTarget != null ? convertClassName(theTarget.getClass()) : "null";
        sb.append(targetVar);
        sb.append('.');
        sb.append(theMethodName);
        sb.append('(');
        if (theArguments != null) {
            for (int i = 0; i < theArguments.length; ++i) {
                if (i > 0) {
                    sb.append(", "); 
                }
                if (theArguments[i] == null) {
                    sb.append("null");
                } else if (theArguments[i] instanceof String) {
                    sb.append('"');
                    sb.append(theArguments[i].toString());
                    sb.append('"');
                } else {
                    sb.append(convertClassName(theArguments[i].getClass()));
                }
            }
        }
        sb.append(')');
        sb.append(';');
        return sb.toString();
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getTarget() {
        return target;
    }

    public void execute() throws Exception {
        invokeMethod();
    }

    Object invokeMethod() throws Exception {
        Object result = null;
        try {
            Object theTarget = getTarget();
            String theMethodName = getMethodName();
            Object[] theArguments = getArguments();
            if (theTarget.getClass().isArray()) {
                Method method = findArrayMethod(theMethodName, theArguments);
                Object[] args = new Object[theArguments.length + 1];
                args[0] = theTarget;
                System.arraycopy(theArguments, 0, args, 1, theArguments.length);
                if (!classFilter.permitMethod(method))
                    throw new SafeXMLDecoder.MethodNotPermittedException(method);
                result = method.invoke(null, args);
            } else if (theMethodName.equals("newInstance") 
                && theTarget == Array.class) {
                Class<?> componentType = (Class<?>) theArguments[0];
                int length = (Integer) theArguments[1];
                result = Array.newInstance(componentType, length);
            } else if (theMethodName.equals("new") 
                || theMethodName.equals("newInstance")) {
                if (theTarget instanceof Class<?>) {
                    Constructor<?> constructor = findConstructor((Class<?>)theTarget, theArguments);
                    if (!classFilter.permitConstructor(constructor))
                        throw new SafeXMLDecoder.ConstructorNotPermittedException(constructor);
                    result = constructor.newInstance(theArguments);
                } else {
                    if ("new".equals(theMethodName)) { 
                        throw new NoSuchMethodException(this.toString());
                    }
                    // target class declares a public named "newInstance" method
                    Method method = findMethod(theTarget.getClass(),
                        theMethodName, theArguments, false);
                    if (!classFilter.permitMethod(method))
                        throw new SafeXMLDecoder.MethodNotPermittedException(method);
                    result = method.invoke(theTarget, theArguments);
                }
            } else if (theMethodName.equals("newArray")) {
                // create a new array instance without length attribute
                int length = theArguments.length;
                Class<?> clazz = (Class<?>) theTarget;

                // check the element types of array
                for (Object theArgument : theArguments) {
                    boolean isNull = theArgument == null;
                    boolean isPrimitiveWrapper = !isNull && isPrimitiveWrapper(theArgument.getClass(), clazz);
                    boolean isAssignable = !isNull && clazz.isAssignableFrom(theArgument.getClass());
                    if (!isNull && !isPrimitiveWrapper && !isAssignable) {
                        throw new IllegalArgumentException("The type of element is mismatch with the type of array");
                    }
                }
                result = Array.newInstance(clazz, length);
                if (clazz.isPrimitive()) {
                    // Copy element according to primitive types
                    arrayCopy(clazz, theArguments, result, length);
                } else {
                    // Copy element of Objects
                    //noinspection SuspiciousSystemArraycopy
                    System.arraycopy(theArguments, 0, result, 0, length);
                }
                return result;
            } else if (theTarget instanceof Class<?>) {
                Method method;
                boolean found = false;
                try {
                    /*
                     * Try to look for a static method of class described by the
                     * given Class object at first process only if the class
                     * differs from Class itself
                     */
                    if (theTarget != Class.class) {
                        method = findMethod((Class<?>) theTarget, theMethodName, theArguments, true);
                        if (!classFilter.permitMethod(method))
                            throw new SafeXMLDecoder.MethodNotPermittedException(method);
                        result = method.invoke(null, theArguments);
                        found = true;
                    }
                } catch (NoSuchMethodException e) {
                    // expected
                }
                if (!found) {
                    // static method was not found
                    // try to invoke method of Class object
                    if (theMethodName.equals("forName") 
                        && theArguments.length == 1 && theArguments[0] instanceof String) {
                        // special handling of Class.forName(String)
                        final String className = (String) theArguments[0];
                        if (!classFilter.permitClass(className))
                            throw new SafeXMLDecoder.ClassNotPermittedException(className);
                        try {
                            result = Class.forName(className);
                        } catch (ClassNotFoundException e2) {
                            result = Class.forName(className, true, Thread
                                .currentThread().getContextClassLoader());
                        }
                    } else {
                        method = findMethod(theTarget.getClass(), theMethodName, theArguments, false);
                        if (!classFilter.permitMethod(method))
                            throw new SafeXMLDecoder.MethodNotPermittedException(method);
                        result = method.invoke(theTarget, theArguments);
                    }
                }
            } else if (theTarget instanceof Iterator<?>){
                final Iterator<?> iterator = (Iterator<?>) theTarget;
                final Method method = findMethod(theTarget.getClass(), theMethodName,
                    theArguments, false);
                if (!classFilter.permitMethod(method))
                    throw new SafeXMLDecoder.MethodNotPermittedException(method);
                if (iterator.hasNext()) {
                    setMethodAccessible(method);
                    result = method.invoke(iterator);
                }
            } else {
                Method method = findMethod(theTarget.getClass(), theMethodName,
                    theArguments, false);
                if (!classFilter.permitMethod(method))
                    throw new SafeXMLDecoder.MethodNotPermittedException(method);
                setMethodAccessible(method);
                result = method.invoke(theTarget, theArguments);
            }
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getCause();
            throw (t != null) && (t instanceof Exception) ? (Exception) t : ite;
        }
        return result;
    }

    /**
     * Utility function for setting the {@code accessible flag} to {@code true} with asserted privileges.
     */
    private static void setMethodAccessible(@NotNull final Method method) {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    method.setAccessible(true);
                    return null;
                }
            });
        } catch (final Exception e) {
            // if any unchecked exception is propagated through AccessController#doPrivileged log it.
            logger.log(Level.WARNING, "Failed to set method \"" + method.getName() + "\" accessible flag to true!", ExceptionUtils.getDebugException(e));
        }
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private void arrayCopy(Class<?> type, Object[] src, Object dest, int length) {
        if (type == boolean.class) {
            boolean[] destination = (boolean[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Boolean) src[i];
            }
        } else if (type == short.class) {
            short[] destination = (short[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Short) src[i];
            }
        } else if (type == byte.class) {
            byte[] destination = (byte[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Byte) src[i];
            }
        } else if (type == char.class) {
            char[] destination = (char[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Character) src[i];
            }
        } else if (type == int.class) {
            int[] destination = (int[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Integer) src[i];
            }
        } else if (type == long.class) {
            long[] destination = (long[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Long) src[i];
            }
        } else if (type == float.class) {
            float[] destination = (float[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Float) src[i];
            }
        } else if (type == double.class) {
            double[] destination = (double[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = (Double) src[i];
            }
        }
    }

    private Method findArrayMethod(String theMethodName, Object[] theArguments) throws NoSuchMethodException {
        // the code below reproduces exact RI exception throwing behavior
        if (!theMethodName.equals("set") && !theMethodName.equals("get")) {
            throw new NoSuchMethodException("Unknown method name for array");
        } else if (theArguments.length > 0 && theArguments[0].getClass() != Integer.class) {
            throw new ClassCastException("First parameter in array getter(setter) is not of Integer type");
        } else if (theMethodName.equals("get") && (theArguments.length != 1)) {
            throw new ArrayIndexOutOfBoundsException("Illegal number of arguments in array getter");
        } else if (theMethodName.equals("set") && (theArguments.length != 2)) {
            throw new ArrayIndexOutOfBoundsException("Illegal number of arguments in array setter");
        }
        if (theMethodName.equals("get")) {
            return Array.class.getMethod("get", new Class[] { Object.class,
                int.class });
        }
        return Array.class.getMethod("set", new Class[] { Object.class,
            int.class, Object.class });
    }

    private Constructor<?> findConstructor(Class<?> targetClass, Object[] theArguments) throws NoSuchMethodException {
        Class<?>[] argClasses = getClasses(theArguments);
        Constructor<?> result = null;
        Constructor<?>[] constructors = targetClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == argClasses.length) {
                boolean found = true;
                for (int j = 0; j < parameterTypes.length; ++j) {
                    boolean argIsNull = argClasses[j] == null;
                    boolean argIsPrimitiveWrapper = isPrimitiveWrapper(argClasses[j],
                        parameterTypes[j]);
                    boolean paramIsPrimitive = parameterTypes[j].isPrimitive();
                    boolean paramIsAssignable = !argIsNull && parameterTypes[j].isAssignableFrom(argClasses[j]);
                    if (!argIsNull && !paramIsAssignable && !argIsPrimitiveWrapper || argIsNull
                        && paramIsPrimitive) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    if (result == null) {
                        // first time, set constructor
                        result = constructor;
                        continue;
                    }
                    // find out more suitable constructor
                    Class<?>[] resultParameterTypes = result
                        .getParameterTypes();
                    boolean isAssignable = true;
                    for (int j = 0; j < parameterTypes.length; ++j) {
                        //noinspection ConstantConditions
                        if (theArguments[j] != null
                            && !(isAssignable &= resultParameterTypes[j].isAssignableFrom(parameterTypes[j]))) {
                            break;
                        }
                        //noinspection ConstantConditions
                        if (theArguments[j] == null
                            && !(isAssignable &= parameterTypes[j].isAssignableFrom(resultParameterTypes[j]))) {
                            break;
                        }
                    }
                    if (isAssignable) {
                        result = constructor;
                    }
                }
            }
        }
        if (result == null) {
            throw new NoSuchMethodException(MessageFormat.format("No constructor for class {0} found", targetClass.getName()));
        }
        return result;
    }

    /**
     * Searches for best matching method for given name and argument types.
     */
    static Method findMethod(Class<?> targetClass, String methodName, Object[] arguments,
                             boolean methodIsStatic) throws NoSuchMethodException {
        Class<?>[] argClasses = getClasses(arguments);
        Method[] methods;

        if(cache.containsKey(targetClass)){
            methods = cache.get(targetClass);
        }else{
            methods = targetClass.getMethods();
            cache.put(targetClass, methods);
        }

        ArrayList<Method> foundMethods = new ArrayList<>();
        Method[] foundMethodsArr;
        for (Method method : methods) {
            int mods = method.getModifiers();
            if (method.getName().equals(methodName)
                && (!methodIsStatic || Modifier.isStatic(mods))) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == argClasses.length) {
                    boolean found = true;
                    for (int j = 0; j < parameterTypes.length; ++j) {
                        boolean argIsNull = (argClasses[j] == null);
                        boolean argIsPrimitiveWrapper = isPrimitiveWrapper(argClasses[j],
                            parameterTypes[j]);
                        boolean paramIsAssignable = !argIsNull && parameterTypes[j].isAssignableFrom(argClasses[j]);
                        if (!argIsNull && !paramIsAssignable && !argIsPrimitiveWrapper){
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        foundMethods.add(method);
                    }
                }
            }
        }
        if (foundMethods.size() == 0) {
            throw new NoSuchMethodException(MessageFormat.format("No method with name {0} is found", methodName));
        }
        if(foundMethods.size() == 1){
            return foundMethods.get(0);
        }
        foundMethodsArr = foundMethods.toArray(new Method[foundMethods.size()]);
        //find the most relevant one
        MethodComparator comparator = new MethodComparator(methodName, argClasses);
        Method chosenOne = foundMethodsArr[0];
        for (int i = 1; i < foundMethodsArr.length; i++) {
            int difference = comparator.compare(chosenOne, foundMethodsArr[i]);
            //if 2 methods have same relevance, throw exception
            if (difference == 0) {
                // if 2 methods have the same signature, check their return type
                Class<?> oneReturnType = chosenOne.getReturnType();
                Class<?> foundMethodReturnType = foundMethodsArr[i]
                    .getReturnType();
                if (oneReturnType.equals(foundMethodReturnType)) {
                    // if 2 methods have the same signature and return type,
                    // throw NoSuchMethodException
                    throw new NoSuchMethodException(MessageFormat.format("Cannot decide which method to call to match {0}", methodName));
                }

                if (oneReturnType.isAssignableFrom(foundMethodReturnType)) {
                    // if chosenOne is super class or interface of
                    // foundMethodReturnType, set chosenOne to foundMethodArr[i]
                    chosenOne = foundMethodsArr[i];
                }
            }
            if(difference > 0){
                chosenOne = foundMethodsArr[i];
            }
        }
        return chosenOne;
    }

    private static boolean isPrimitiveWrapper(Class<?> wrapper, Class<?> base) {
        return (base == boolean.class) && (wrapper == Boolean.class) || (base == byte.class)
            && (wrapper == Byte.class) || (base == char.class)
            && (wrapper == Character.class) || (base == short.class)
            && (wrapper == Short.class) || (base == int.class)
            && (wrapper == Integer.class) || (base == long.class)
            && (wrapper == Long.class) || (base == float.class) && (wrapper == Float.class)
            || (base == double.class) && (wrapper == Double.class);
    }

    private static Class<?> getPrimitiveWrapper(Class<?> base) {
        Class<?> res = null;
        if (base == boolean.class) {
            res = Boolean.class;
        } else if (base == byte.class) {
            res = Byte.class;
        } else if (base == char.class) {
            res = Character.class;
        } else if (base == short.class) {
            res = Short.class;
        } else if (base == int.class) {
            res = Integer.class;
        } else if (base == long.class) {
            res = Long.class;
        } else if (base == float.class) {
            res = Float.class;
        } else if (base == double.class) {
            res = Double.class;
        }
        return res;
    }

    static String convertClassName(Class<?> type) {
        StringBuilder clazzNameSuffix = new StringBuilder();
        Class<?> componentType;
        Class<?> clazzType = type;
        while ((componentType = clazzType.getComponentType()) != null) {
            clazzNameSuffix.append("Array"); 
            clazzType = componentType;
        }
        String clazzName = clazzType.getName();
        int k = clazzName.lastIndexOf('.');
        if (k != -1 && k < clazzName.length()) {
            clazzName = clazzName.substring(k + 1);
        }
        if (clazzNameSuffix.length() == 0 && "String".equals(clazzName)) { 
            return "\"\""; 
        }
        return clazzName + clazzNameSuffix.toString();
    }

    private static Class<?>[] getClasses(Object[] arguments) {
        Class<?>[] result = new Class[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            result[i] = (arguments[i] == null) ? null : arguments[i].getClass();
        }
        return result;
    }

    /**
     * Comparator to determine which of two methods is "closer" to the reference
     * method.
     */
    static class MethodComparator implements Comparator<Method> {
        static int INFINITY = Integer.MAX_VALUE;

        private String referenceMethodName;

        private Class<?>[] referenceMethodArgumentTypes;

        private final Map<Method, Integer> cache;

        public MethodComparator(String refMethodName, Class<?>[] refArgumentTypes) {
            this.referenceMethodName = refMethodName;
            this.referenceMethodArgumentTypes = refArgumentTypes;
            cache = new HashMap<>();
        }

        public int compare(Method m1, Method m2) {
            Integer norm1 = cache.get(m1);
            Integer norm2 = cache.get(m2);
            if (norm1 == null) {
                norm1 = getNorm(m1);
                cache.put(m1, norm1);
            }
            if (norm2 == null) {
                norm2 = getNorm(m2);
                cache.put(m2, norm2);
            }
            return (norm1 - norm2);
        }

        /**
         * Returns the norm for given method. The norm is the "distance" from
         * the reference method to the given method.
         *
         * @param m
         *            the method to calculate the norm for
         * @return norm of given method
         */
        private int getNorm(Method m) {
            String methodName = m.getName();
            Class<?>[] argumentTypes = m.getParameterTypes();
            int totalNorm = 0;
            if (!referenceMethodName.equals(methodName)
                || referenceMethodArgumentTypes.length != argumentTypes.length) {
                return INFINITY;
            }
            for (int i = 0; i < referenceMethodArgumentTypes.length; i++) {
                if (referenceMethodArgumentTypes[i] == null) {
                    // doesn't affect the norm calculation if null
                    continue;
                }
                if (referenceMethodArgumentTypes[i].isPrimitive()) {
                    referenceMethodArgumentTypes[i] = getPrimitiveWrapper(referenceMethodArgumentTypes[i]);
                }
                if (argumentTypes[i].isPrimitive()) {
                    argumentTypes[i] = getPrimitiveWrapper(argumentTypes[i]);
                }
                totalNorm += getDistance(referenceMethodArgumentTypes[i], argumentTypes[i]);
            }
            return totalNorm;
        }

        /**
         * Returns a "hierarchy distance" between two classes.
         *
         * @param clz1 the first class
         * @param clz2 the second class
         *            should be superclass or superinterface of clz1
         * @return hierarchy distance from clz1 to clz2, Integer.MAX_VALUE if
         *         clz2 is not assignable from clz1.
         */
        private static int getDistance(Class<?> clz1, Class<?> clz2) {
            Class<?> superClz;
            int superDist = INFINITY;
            if (!clz2.isAssignableFrom(clz1)) {
                return INFINITY;
            }
            if (clz1.getName().equals(clz2.getName())) {
                return 0;
            }
            superClz = clz1.getSuperclass();
            if (superClz != null) {
                superDist = getDistance(superClz, clz2);
            }
            if (clz2.isInterface()) {
                Class<?>[] interfaces = clz1.getInterfaces();
                int bestDist = INFINITY;
                for (Class<?> element : interfaces) {
                    int curDist = getDistance(element, clz2);
                    if (curDist < bestDist) {
                        bestDist = curDist;
                    }
                }
                if (superDist < bestDist) {
                    bestDist = superDist;
                }
                return (bestDist != INFINITY ? bestDist + 1 : INFINITY);
            }
            return (superDist != INFINITY ? superDist + 1 : INFINITY);
        }
    }
}

class SafeExpression extends SafeStatement {

    final ClassFilter classFilter;
    boolean valueIsDefined = false;

    Object value;

    public SafeExpression(ClassFilter classFilter, Object target, String methodName, Object[] arguments) {
        super(classFilter, target, methodName, arguments);
        this.classFilter = classFilter;
        this.value = null;
        this.valueIsDefined = false;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();

            if (!valueIsDefined) {
                sb.append("<unbound>"); 
            } else {
                if (value == null) {
                    sb.append("null"); 
                } else {
                    sb.append(convertClassName(value.getClass()));
                }
            }
            sb.append('=');
            sb.append(super.toString());

            return sb.toString();
        } catch (Exception e) {
            return MessageFormat.format("Error in expression: {0}", e.getClass());
        }
    }

    public void setValue(Object value) {
        this.value = value;
        this.valueIsDefined = true;
    }

    public Object getValue() throws Exception {
        if (!valueIsDefined) {
            value = invokeMethod();
            valueIsDefined = true;
        }
        return value;
    }
}
