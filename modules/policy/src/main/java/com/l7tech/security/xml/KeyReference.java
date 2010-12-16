/**
 * @author emil
 * @version 17-Apr-2005
 */
package com.l7tech.security.xml;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * The enumeration type for Key References.
 */
public final class KeyReference implements Serializable {
    private static int index = 0;
    public static final KeyReference BST = new KeyReference(index++, "BinarySecurityToken");
    public static final KeyReference SKI = new KeyReference(index++, "SubjectKeyIdentifier");
    public static final KeyReference ISSUER_SERIAL = new KeyReference(index++, "IssuerSerial");

    // for readResolve
    private static final KeyReference[] REFERENCES = {BST, SKI, ISSUER_SERIAL};

    private int val;
    private final String name;

    /**
     * Private constructor, to support instantiating only the constant
     */
    private KeyReference(int index, String name) {
        val = index;
        this.name = name;
    }

    /**
     * Get the KeyReference with the specified name.
     *
     * @param name the name to look up.  Case sensitive.  Required.
     * @return the KeyReference with a matching name.  Never null.
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if there is no KeyReference with the specified name.
     */
    public static KeyReference valueOf(String name) {
        if (name == null) throw new NullPointerException("name");
        if (name.equals(BST.getName())) {
            return BST;
        } else if (name.equals(SKI.getName())) {
            return SKI;
        } else if (name.equals(ISSUER_SERIAL.getName())) {
            return ISSUER_SERIAL;
        }
        throw new IllegalArgumentException("No such KeyReference: " + name);
    }

    /**
     * @return the short key reference type name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the array of all types
     */
    public static List<KeyReference> getAllTypes() {
        return Arrays.asList(REFERENCES);
    }

    /**
     * Serialization support for correct enum resolving.
     */
    private Object readResolve() throws ObjectStreamException {
        return REFERENCES[val];
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final KeyReference xencAlgorithm = (KeyReference)o;

        if (val != xencAlgorithm.val) return false;
        if (name != null ? !name.equals(xencAlgorithm.name) : xencAlgorithm.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = val;
        result = 29 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return name;
    }
}
