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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CountryImp)) return false;

        final CountryImp countryImp = (CountryImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == countryImp._oid) : countryImp._oid != DEFAULT_OID ) return false;
        if (_code != null ? !_code.equals(countryImp._code) : countryImp._code != null) return false;
        if (_states != null ? !_states.equals(countryImp._states) : countryImp._states != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_code != null ? _code.hashCode() : 0);
        result = 29 * result + (_states != null ? _states.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

}
