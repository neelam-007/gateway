package com.l7tech.ntlm.protocol;

 /**
  * Copyright: Layer 7 Technologies, 2012
  * User: ymoiseyenko
  */
 public class PasswordCredential
   implements NtlmCredential
 {
   protected char[] password = null;
   protected NtlmSecurityPrincipal principalNtlm;

     public PasswordCredential(String acctname, char[] password)
             throws AuthenticationManagerException {
        this(null, acctname, password);
     }

   public PasswordCredential(String domain, String acctname, char[] password)
     throws AuthenticationManagerException
   {
     this.principalNtlm = new NtlmSecurityPrincipal(domain, acctname);
     if (password != null) {
       this.password = new char[password.length];
       System.arraycopy(password, 0, this.password, 0, password.length);
     }
   }
 
   public NtlmSecurityPrincipal getSecurityPrincipal()
   {
     return this.principalNtlm;
   }
 
   public char[] getPassword()
   {
     return this.password;
   }
 
   public void destroy()
   {
     if (this.password == null)
       return;
     for (int i = 0; i < this.password.length; i++)
       this.password[i] = ' ';
     this.password = null;
   }
 
   public String toString()
   {
     return this.principalNtlm.getName();
   }
 }
