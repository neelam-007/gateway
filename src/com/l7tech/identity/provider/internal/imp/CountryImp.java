/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.provider.internal.imp;

import com.l7tech.identity.provider.internal.Country;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class CountryImp extends NamedEntityImp implements Country {

    public String getCode() { return _code; }

    public void setCode(String code) { _code = code; }

    private String _code;
}
