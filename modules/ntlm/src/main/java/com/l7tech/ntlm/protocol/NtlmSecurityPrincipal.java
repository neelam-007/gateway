package com.l7tech.ntlm.protocol;

import java.security.Principal;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class NtlmSecurityPrincipal
  implements Principal
{
  public static final CanonicalForm DEFAULT_FORM = CanonicalForm.SIMPLE_NAME;

  public enum CanonicalForm {
      SIMPLE_NAME,
      NETBIOS,
      INTERNET
  }

  protected String domain;
  protected String name;
  protected CanonicalForm form = DEFAULT_FORM;

  public NtlmSecurityPrincipal(String acctname)
    throws AuthenticationManagerException
  {
    this(null, acctname);
  }

  public NtlmSecurityPrincipal(String dname, String uname)
    throws AuthenticationManagerException
  {
      int ci = uname.indexOf('@');
      if (ci >= 0) {
        dname = uname.substring(ci + 1);
        uname = uname.substring(0, ci);
        form = CanonicalForm.INTERNET;
      } else {
        ci = uname.indexOf('\\');
        if (ci >= 0) {
          dname = uname.substring(0, ci);
          uname = uname.substring(ci + 1);
          form = CanonicalForm.NETBIOS;
        }
      }

    if (dname != null) {
      dname = dname.trim();
      if (dname.length() == 0)
        dname = null;
      if (dname == null) {
          form = DEFAULT_FORM;
      }
    }
    if (uname != null) {
      uname = uname.trim();
      if (uname.length() == 0) {
        uname = null;
      }
    }
    if (uname == null) {
      throw new AuthenticationManagerException("Invalid account name");
    }
    this.domain = dname;
    this.name = uname;
  }

  public String getDomain()
  {
    return this.domain;
  }

  public String getUsername()
  {
    return this.name;
  }

  public String getName()
  {
    return getCanonicalForm(DEFAULT_FORM);
  }

  public String getCanonicalForm(CanonicalForm form) {
   String ret = null;
   switch (form) {
    case SIMPLE_NAME:
      ret = name;
      break;
    case NETBIOS:
      ret =  (this.domain == null ? "" : this.domain) + '\\' + this.name;
      break;
    case INTERNET:
      ret = this.name + '@' + (this.domain == null ? "" : this.domain);
    }
    return ret;
  }


  public int hashCode()
  {
    int hc = this.name.hashCode();
    if (this.domain != null)
      hc *= this.domain.hashCode();
    return hc;
  }

  public boolean equals(Object obj)
  {
    if ((obj instanceof NtlmSecurityPrincipal)) {
      NtlmSecurityPrincipal p = (NtlmSecurityPrincipal)obj;

      if (this.name.equalsIgnoreCase(p.name)) {
        if ((this.domain == null) && (p.domain == null))
          return true;
        return (this.domain != null) && (p.domain != null) && (this.domain.equalsIgnoreCase(p.domain));
      }
    }
    return false;
  }

  public String toString()
  {
    return getName();
  }
}
