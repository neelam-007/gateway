/*
 * Created on 7-May-2003
 */
package com.l7tech.ssg.objectmodel.imp;

import com.l7tech.ssg.objectmodel.Country;

/**
 * @author alex
 */
public class CountryImp extends NamedEntityImp implements Country {

    public String getCode() { return _code; }

    public void setCode(String code) { _code = code; }

    private String _code;
}
