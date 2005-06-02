/**
 * @author emil
 * @version 17-Apr-2005
 */
package com.l7tech.common.security.xml;

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
    // for readResolve
    private static final KeyReference[] REFERENCES = {BST, SKI};

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
     * @return the short key reference type name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the array of all types
     */
    public static List getAllTypes() {
        return Arrays.asList(REFERENCES);
    }

    /**
     * Serialization support for correct enum resolving.
     */
    private Object readResolve() throws ObjectStreamException {
        return REFERENCES[val];
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final KeyReference xencAlgorithm = (KeyReference)o;

        if (val != xencAlgorithm.val) return false;
        if (name != null ? !name.equals(xencAlgorithm.name) : xencAlgorithm.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = val;
        result = 29 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


    public String toString() {
        return "KeyReference{" +
                 "val=" + val +
                 ", name='" + name + "'" +
                 "}";
    }
}
