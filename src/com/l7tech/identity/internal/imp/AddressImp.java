/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.Address;
import com.l7tech.identity.internal.Country;
import com.l7tech.identity.internal.State;
import com.l7tech.objectmodel.imp.EntityImp;

/**
 * @author alex
 */
public class AddressImp extends EntityImp implements Address {
	public String getAddress() { return _address; }
    public String getAddress2() { return _address2; }
    public String getCity() { return _city; }
    public State getState() { return _state; }
    public Country getCountry() { return _country; }
    public String getPostalCode() { return _postalCode; }
    
    public void setAddress(String address) { _address = address; }
    public void setAddress2(String address2) { _address2 = address2; }
    public void setCity(String city) { _city = city; }
    public void setState(State state) { _state = state; }
    public void setCountry(Country country) { _country = country; }
    public void setPostalCode(String postalCode) { _postalCode = postalCode; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddressImp)) return false;

        final AddressImp addressImp = (AddressImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == addressImp._oid) : addressImp._oid != DEFAULT_OID ) return false;
        if (_address != null ? !_address.equals(addressImp._address) : addressImp._address != null) return false;
        if (_address2 != null ? !_address2.equals(addressImp._address2) : addressImp._address2 != null) return false;
        if (_city != null ? !_city.equals(addressImp._city) : addressImp._city != null) return false;
        if (_country != null ? !_country.equals(addressImp._country) : addressImp._country != null) return false;
        if (_postalCode != null ? !_postalCode.equals(addressImp._postalCode) : addressImp._postalCode != null) return false;
        if (_state != null ? !_state.equals(addressImp._state) : addressImp._state != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_postalCode != null ? _postalCode.hashCode() : 0);
        result = 29 * result + (_country != null ? _country.hashCode() : 0);
        result = 29 * result + (_state != null ? _state.hashCode() : 0);
        result = 29 * result + (_city != null ? _city.hashCode() : 0);
        result = 29 * result + (_address2 != null ? _address2.hashCode() : 0);
        result = 29 * result + (_address != null ? _address.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

	private String _postalCode;
	private Country _country;
	private State _state;
	private String _city;
	private String _address2;
	private String _address;
}
