/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.Country;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;

/**
 * @author alex
 */
public class CountryImp extends NamedEntityImp implements Country {

    public String getCode() { return _code; }

    public void setCode(String code) { _code = code; }

    public Set getStates() {
        return _states;
    }

    public void setStates( Set states ) {
        _states = states;
    }

    private String _code;
    private Set _states;
}
