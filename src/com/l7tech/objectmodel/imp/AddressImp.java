/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Address;
import com.l7tech.objectmodel.Country;
import com.l7tech.objectmodel.State;

/**
 * @author alex
 */
public class AddressImp extends StandardEntityImp implements Address {
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

	private String _postalCode;
	private Country _country;
	private State _state;
	private String _city;
	private String _address2;
	private String _address;
}
