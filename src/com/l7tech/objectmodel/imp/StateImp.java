/*
 * Created on 7-May-2003
 */
package com.l7tech.ssg.objectmodel.imp;

import com.l7tech.ssg.objectmodel.Country;
import com.l7tech.ssg.objectmodel.State;

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

    private Country _country;
    private String _code;
}
