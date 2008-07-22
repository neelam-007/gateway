package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.util.EnumTranslator;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: steve
 * Date: Sep 20, 2005
 * Time: 5:25:27 PM
 * $Id$
 */
public class AuthenticationProperties implements Cloneable, Serializable {

    //- PUBLIC

    public static final Method METHOD_BASIC = new Method("basic");
    public static final Method METHOD_FORM = new Method("form");

    public static Method forKeyName(String id) {
        Method method = null;
        if(METHOD_BASIC.getId().equals(id)) {
            method = METHOD_BASIC;
        }
        else if(METHOD_FORM.getId().equals(id)) {
            method = METHOD_FORM;
        }
        return method;
    }

    public AuthenticationProperties() {
        additionalFields = Collections.EMPTY_MAP;
        setMethod(METHOD_BASIC);
    }

    public AuthenticationProperties(AuthenticationProperties source) {
        setAuthenticationProperties(source);
    }

    public void setAuthenticationProperties(AuthenticationProperties source) {
        setMethod(source.getMethod());
        setFormTarget(source.getFormTarget());
        setUsernameFieldname(source.getUsernameFieldname());
        setPasswordFieldname(source.getPasswordFieldname());
        setRequestForm(source.isRequestForm());
        setRedirectAfterSubmit(source.isRedirectAfterSubmit());
        setEnableCookies(source.isEnableCookies());
        setCopyFormFields(source.isCopyFormFields());
        setAdditionalFields(source.getAdditionalFields());
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getFormTarget() {
        return formTarget;
    }

    public void setFormTarget(String formTarget) {
        this.formTarget = formTarget;
    }

    public String getUsernameFieldname() {
        return usernameFieldname;
    }

    public void setUsernameFieldname(String usernameFieldname) {
        this.usernameFieldname = usernameFieldname;
        if(this.usernameFieldname!=null && this.usernameFieldname.trim().length()==0) {
            this.usernameFieldname = null;
        }
    }

    public String getPasswordFieldname() {
        return passwordFieldname;
    }

    public void setPasswordFieldname(String passwordFieldname) {
        this.passwordFieldname = passwordFieldname;
        if(this.passwordFieldname!=null && this.passwordFieldname.trim().length()==0) {
            this.passwordFieldname = null;
        }

    }

    public boolean isRequestForm() {
        return requestForm;
    }

    public void setRequestForm(boolean requestForm) {
        this.requestForm = requestForm;
    }

    public boolean isRedirectAfterSubmit() {
        return redirectAfterSubmit;
    }

    public void setRedirectAfterSubmit(boolean redirectAfterSubmit) {
        this.redirectAfterSubmit = redirectAfterSubmit;
    }

    public boolean isEnableCookies() {
        return enableCookies;
    }

    public void setEnableCookies(boolean enableCookies) {
        this.enableCookies = enableCookies;
    }

    public boolean isCopyFormFields() {
        return copyFormFields;
    }

    public void setCopyFormFields(boolean copyFormFields) {
        this.copyFormFields = copyFormFields;
    }

    public Map getAdditionalFields() {
        return Collections.unmodifiableMap(additionalFields);
    }

    public void setAdditionalFields(Map additionalFields) {
        this.additionalFields = new HashMap(additionalFields);
    }

    public static final class Method implements Cloneable, Serializable {

        //- PUBLIC

        public boolean equals(Object compare) {
            boolean equal = false;
            if(compare==this) {
                equal = true;
            }
            else if(compare instanceof Method) {
                Method other = (Method) compare;
                equal = other.id.equals(this.id);
            }
            return equal;
        }

        public int hashCode() {
            return (id != null ? id.hashCode() : 0);
        }

        public String getId() {
            return id;
        }

        public String toString() {
            return "Method(id='"+id+"')";
        }

        // This method is invoked reflectively by WspEnumTypeMapping
        public static EnumTranslator getEnumTranslator() {
            return new EnumTranslator() {
                public String objectToString(Object in) {
                    return ((AuthenticationProperties.Method)in).getId();
                }

                public Object stringToObject(String in) {
                    return AuthenticationProperties.forKeyName(in);
                }
            };
        }

        //- PRIVATE

        private String id;

        private Method(String id) {
            this.id = id;
        }
    }

    //- PRIVATE

    private Method method;
    private String formTarget;
    private String usernameFieldname;
    private String passwordFieldname;
    private boolean requestForm;
    private boolean redirectAfterSubmit;
    private boolean enableCookies;
    private boolean copyFormFields;
    private Map additionalFields;

}
