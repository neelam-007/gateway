/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.Country;
import com.l7tech.identity.internal.State;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class StateImp extends NamedEntityImp implements State {
    public Country getCountry() {
        return _country;
    }

    public String getCode() {
        return _code;
    }

    public void setCountry(Country country) {
        _country = country;
    }

    public void setCode(String code) {
        _code = code;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StateImp)) return false;

        final StateImp stateImp = (StateImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == stateImp._oid) : stateImp._oid != DEFAULT_OID ) return false;
        if (_code != null ? !_code.equals(stateImp._code) : stateImp._code != null) return false;
        if (_country != null ? !_country.equals(stateImp._country) : stateImp._country != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_country != null ? _country.hashCode() : 0);
        result = 29 * result + (_code != null ? _code.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

    private Country _country;
    private String _code;
}
