package com.l7tech.common.io;

import org.apache.xerces.util.SymbolTable;

/**
 * This is a version of SymbolTable that avoids eventually filling all memory with old symbols, but that (hopefully!)
 * doesn't NPE in rare instances during times of memory pressure, like SoftRefereneSymbolTable does as of Xerces 2.11.0.
 * <p/>
 * This version simply relies on String.intern() at all times, and makes no effort to avoid creating redundant
 * String objects to perform lookups by character array.
 * <p/>
 * This relies on the JDK 7 change to store interned strings on the regular heap rather than in permgen -- if used
 * with JDK 6, the Gateway's permgen may be prone to running out.
 * <p/>
 * Since this table never retains references to symbols, it should not be susceptible to resource exhaustion
 * if the Gateway is fed a large number of unique symbols.
 * <p/>
 * It may, however, substantially slow down DOM parsing speed.
 */
public class InternSymbolTable extends SymbolTable {
    @Override
    public String addSymbol(String symbol) {
        return symbol.intern();
    }

    @Override
    public String addSymbol(char[] buffer, int offset, int length) {
        return new String(buffer, offset, length).intern();
    }

    @Override
    protected void rehash() {
    }

    @Override
    public boolean containsSymbol(String symbol) {
        return true;
    }

    @Override
    public boolean containsSymbol(char[] buffer, int offset, int length) {
        return true;
    }
}
