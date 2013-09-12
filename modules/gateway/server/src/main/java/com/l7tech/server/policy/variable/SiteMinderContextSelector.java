package com.l7tech.server.policy.variable;

import com.ca.siteminder.SiteMinderContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/11/13
 */
public class SiteMinderContextSelector implements ExpandVariables.Selector<SiteMinderContext> {

    protected static final Pattern ATTRUBUTES_PATTERN = Pattern.compile("attributes(\\.(\\d+)((\\.\\w{1,})*))?");
    protected static final Pattern AUTHSCHEME_PATTERN = Pattern.compile("authschemes(\\[(\\d+)\\])?");

    @Override
    public Selection select(String contextName, SiteMinderContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        final String lname = name.toLowerCase();

        if(null == context) return null;// empty data

        if(lname.startsWith("attributes")) {
            Matcher m = ATTRUBUTES_PATTERN.matcher(lname);
            if(m.find()){
                if(StringUtils.isNotEmpty(m.group(1))){
                    if(StringUtils.isNotEmpty(m.group(3))){
                        Pair<String, Object> attribute = getElement(context.getAttrList(), m, handler);
                        String remaining = m.group(3).substring(1);
                        if(remaining.equals("name")) {
                            return new Selection(attribute.left);
                        }
                        else if(remaining.equals("value")) {
                            return new Selection(attribute.right);
                        }
                        else{
                            return null;
                        }
                    }
                }
                else if (lname.equals("attributes.length")){
                    if(context.getAttrList() != null) {
                        return new Selection(Integer.toString(context.getAttrList().size()));
                    }
                }
                else if(lname.equals("attributes")) {
                    return new Selection(context.getAttrList(), name.substring("attributes".length()));
                }
                else {
                    if(lname.length() > "attributes".length() + 1){
                        String remaining = lname.substring("attributes".length() + 1);
                        for(Pair<String, Object> attribute : context.getAttrList()) {
                            if(remaining.equalsIgnoreCase(attribute.left)){
                                return new Selection(attribute.right);
                            }
                        }
                    }
                }
            }
        }
        else if(lname.equals("ssotoken")){
            return new Selection(context.getSsoToken());
        }
        else if(lname.equals("transactionid")) {
            return new Selection(context.getTransactionId());
        }
        else if(lname.startsWith("authschemes")){
            List<SiteMinderContext.AuthenticationScheme> authSchemeList = context.getAuthSchemes();
            if(lname.equals("authschemes.length")){
                return new Selection(Integer.toString(authSchemeList.size()));
            }
            else {
                Matcher m = AUTHSCHEME_PATTERN.matcher(lname);
                if(m.find()) {
                    if(StringUtils.isNotEmpty(m.group(2))){
                        SiteMinderContext.AuthenticationScheme scheme = getElement(authSchemeList, m, handler);
                        return new Selection(scheme.toString());
                    }
                    else if(StringUtils.isEmpty(m.group(1))){
                        return getAuthenticationSchemesAsStrings(lname, authSchemeList);
                    }
                }
            }
        }


        return null;
    }

    private Selection getAuthenticationSchemesAsStrings(String lname, List<SiteMinderContext.AuthenticationScheme> authSchemeList) {
        List<String> sAuthSchemeList = new ArrayList<>(authSchemeList.size());
        for(SiteMinderContext.AuthenticationScheme scheme : authSchemeList){
            sAuthSchemeList.add(scheme.toString());
        }
        return new Selection(sAuthSchemeList, lname.substring("authschemes".length()));
    }

    private static <T> T getElement(List<T> list, Matcher matcher, Syntax.SyntaxErrorHandler handler){
        T elem = null;
        int index = 0;
        try{
            index = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ne) {
            throw new RuntimeException(handler.handleBadVariable(matcher.group(), ne));
        }
        if(null != list){
            if(index >= list.size()) {
                throw new RuntimeException(handler.handleSubscriptOutOfRange(index, matcher.group(), list.size()));
            }
            elem = list.get(index);
        }
        return elem;
    }

    @Override
    public Class<SiteMinderContext> getContextObjectClass() {
        return SiteMinderContext.class;
    }
}
