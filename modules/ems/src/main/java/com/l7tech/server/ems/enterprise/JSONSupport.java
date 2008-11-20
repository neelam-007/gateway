package com.l7tech.server.ems.enterprise;

import org.mortbay.util.ajax.JSON;

import java.util.Map;

/**
 * Non thread-safe JSON support class.
 */
public abstract class JSONSupport implements JSON.Convertible {

    //- PUBLIC

    public JSONSupport() {
        this( null );
    }

    public JSONSupport( final JSON.Convertible base ) {
        this.base = base;
    }

    @Override
    public final void toJSON( final JSON.Output out ) {
        this.out = out;
        try {
            writeJson();            
        } finally {
            this.out = null;
        }
    }

    @Override
    public final void fromJSON( final Map object ) {
        throw new UnsupportedOperationException();
    }

    //- PROTECTED

    protected void writeJson() {
        if ( base != null ) {
            base.toJSON( out );
        }
    }

    protected final void addClass(Class c) {
        out.addClass(c);
    }

    protected final void add(Object obj) {
        out.add(obj);
    }

    protected final void add(String name, Object value) {
        out.add(name, value);
    }

    protected final void add(String name, double value) {
        out.add(name, value);
    }

    protected final void add(String name, long value) {
        out.add(name, value);
    }

    protected final void add(String name, boolean value) {
        out.add(name, value);
    }

    //- PRIVATE

    private final JSON.Convertible base;
    private JSON.Output out;
}
