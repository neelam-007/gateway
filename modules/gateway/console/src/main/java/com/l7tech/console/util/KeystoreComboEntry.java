package com.l7tech.console.util;

import com.l7tech.objectmodel.Goid;

/**
 * This class defines key store details for each private key in a combo box list.
 *
 * Note: this class is extracted from ResolvePrivateKeyPanel.
 * Now it is used in ResolvePrivateKeyPanel and MigrationEntityDetailPanel.
 */
public class KeystoreComboEntry implements Comparable<KeystoreComboEntry> {
    private final Goid keystoreid;
    private final String keystorename;
    private final String alias;

    public KeystoreComboEntry(Goid keystoreid, String keystorename, String alias) {
        this.keystoreid = keystoreid;
        this.keystorename = keystorename;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public Goid getKeystoreid() {
        return keystoreid;
    }

    @Override
    public int compareTo( final KeystoreComboEntry o ) {
        return alias.compareTo( o.alias );
    }

    public String toString() {
        return "'" + alias + "'" + " in " + keystorename;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final KeystoreComboEntry that = (KeystoreComboEntry) o;

        if ( keystoreid != that.keystoreid ) return false;
        if ( alias != null ? !alias.equals( that.alias ) : that.alias != null ) return false;
        if ( keystorename != null ? !keystorename.equals( that.keystorename ) : that.keystorename != null )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = keystoreid != null ? keystoreid.hashCode() : 0;
        result = 31 * result + (keystorename != null ? keystorename.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        return result;
    }
}