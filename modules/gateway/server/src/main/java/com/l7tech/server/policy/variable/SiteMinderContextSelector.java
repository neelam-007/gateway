package com.l7tech.server.policy.variable;

import com.ca.siteminder.SiteMinderAgentConstants;
import com.ca.siteminder.SiteMinderContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

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
    protected static final Pattern REALMDEF_PATTERN = Pattern.compile("realmdef\\.(\\w+)");
    protected static final Pattern RESDEF_PATTERN = Pattern.compile("resourcedef\\.(\\w+)");
    protected static final Pattern SESSDEF_PATTERN = Pattern.compile("sessdef\\.(\\w+)");

    @Override
    public Selection select(String contextName, SiteMinderContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {

        final String lname = name.toLowerCase();

        if(null == context) return null;// empty data

        final SiteMinderContext ctx = context;

        if(lname.startsWith("attributes")) {
            Matcher m = ATTRUBUTES_PATTERN.matcher(lname);
            if(m.find()){
                final List<SiteMinderContext.Attribute> attributes = getSiteMinderAttributes(ctx);

                if(StringUtils.isNotEmpty(m.group(1))){
                    if(StringUtils.isNotEmpty(m.group(3))){
                        SiteMinderContext.Attribute attribute = getElement(attributes, m, handler);
                        String remaining = m.group(3).substring(1);
                        if(remaining.equals("name")) {
                            return new Selection(attribute.getName());
                        }
                        else if(remaining.equals("value")) {
                            return new Selection(attribute.getValue());
                        }
                        else if(remaining.equalsIgnoreCase("value.tostring")) {
                            return new Selection(convertAttrValueType(attribute.getValue()));
                        }
                        else{
                            return null;
                        }
                    }
                }
                else if (lname.equals("attributes.length")){
                    if(attributes != null) {
                        return new Selection(Integer.toString(attributes.size()));
                    }
                }
                else if(lname.equals("attributes")) {
                    return new Selection(attributes, name.substring("attributes".length()));
                }
                else {
                    if(lname.length() > "attributes".length() + 1){
                        String remaining = lname.substring("attributes".length() + 1);
                        for(SiteMinderContext.Attribute attribute : attributes) {
                            if(remaining.equalsIgnoreCase(attribute.getName() + ".toString")) {
                                return new Selection(convertAttrValueType(attribute.getValue()));
                            }
                            else if(remaining.equalsIgnoreCase(attribute.getName())){
                                return new Selection(attribute.getValue());
                            }
                        }
                    }
                }
            }
        }
        else if(lname.equals("ssotoken")){
            return new Selection(ctx.getSsoToken());
        }
        else if(lname.equals("transactionid")) {
            return new Selection(ctx.getTransactionId());
        }
        else if(lname.equals("sourceipaddress")) {
            return new Selection(ctx.getSourceIpAddress());
        }
        else if(lname.startsWith("authschemes")){
            List<SiteMinderContext.AuthenticationScheme> authSchemeList = ctx.getAuthSchemes();
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
        else if(lname.startsWith("realmdef")){
           return matchPattern(REALMDEF_PATTERN, lname, new Functions.Unary<Selection, String>() {
               @Override
               public Selection call(String remaining) {
                   SiteMinderContext.RealmDef realmDef = ctx.getRealmDef();
                   if(remaining.equals("formlocation")){
                       return new Selection(realmDef.getFormLocation());
                   }
                   else if(remaining.equals("credentials")){
                       return new Selection((Integer.toString(realmDef.getCredentials())));
                   }
                   else if(remaining.equals("oid")){
                       return new Selection(realmDef.getOid());
                   }
                   else if(remaining.equals("domoid")){
                       return new Selection(realmDef.getDomOid());
                   }
                   else if(remaining.equals("name")){
                       return new Selection(realmDef.getName());
                   }

                   return null;
               }
           });

        }
        else if(lname.startsWith("resourcedef")) {
            return matchPattern(RESDEF_PATTERN, lname, new Functions.Unary<Selection, String>(){
                @Override
                public  Selection call(String remaining) {
                    SiteMinderContext.ResourceContextDef resourceContextDef = ctx.getResContextDef();
                    if(remaining.equals("agent")){
                        return new Selection(resourceContextDef.getAgent());
                    }
                    else if(remaining.equals("action")){
                        return new Selection(resourceContextDef.getAction());
                    }
                    else if(remaining.equals("resource")) {
                        return new Selection(resourceContextDef.getResource());
                    }
                    else if(remaining.equals("server")){
                        return new Selection(resourceContextDef.getServer());
                    }

                    return null;
                }
            });
        }
        else if(lname.startsWith("sessdef")) {
            return matchPattern(SESSDEF_PATTERN, lname, new Functions.Unary<Selection, String>(){
                @Override
                public  Selection call(String remaining) {
                    SiteMinderContext.SessionDef sessionDef = ctx.getSessionDef();
                    if(remaining.equals("id")){
                        return new Selection(sessionDef.getId());
                    }
                    else if(remaining.equals("spec")){
                        return new Selection(sessionDef.getSpec());
                    }
                    else if(remaining.equals("idletimeout")) {
                        return new Selection(Integer.toString(sessionDef.getIdleTimeout()));
                    }
                    else if(remaining.equals("maxtimeout")){
                        return new Selection(Integer.toString(sessionDef.getMaxTimeout()));
                    }
                    else if(remaining.equals("reason")){
                        return new Selection(Integer.toString(sessionDef.getReason()));
                    }
                    else if(remaining.equals("starttime")){
                        return new Selection(Integer.toString(sessionDef.getSessionStartTime()));
                    }
                    else if(remaining.equals("lasttime")){
                        return new Selection(Integer.toString(sessionDef.getSessionLastTime()));
                    }
                    else if(remaining.equals("currenttime")){
                        return new Selection(Integer.toString(sessionDef.getCurrentServerTime()));
                    }

                    return null;
                }
            });
        }


        return null;
    }

    @NotNull
    private List<SiteMinderContext.Attribute> getSiteMinderAttributes(SiteMinderContext ctx) {
        final List<SiteMinderContext.Attribute> attributes = new ArrayList<>(ctx.getAttrList());
        //add session attributes to the list of attributes
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_SESSIONID, ctx.getSessionDef().getId()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_SESSIONSPEC, ctx.getSessionDef().getSpec()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_STARTSESSIONTIME, ctx.getSessionDef().getSessionStartTime()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_LASTSESSIONTIME, ctx.getSessionDef().getSessionLastTime()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_MAXSESSIONTIMEOUT, ctx.getSessionDef().getMaxTimeout()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_IDLESESSIONTIMEOUT, ctx.getSessionDef().getIdleTimeout()));
        attributes.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_CURRENTSERVERTIME, ctx.getSessionDef().getCurrentServerTime()));
        return attributes;
    }

    private Selection matchPattern(Pattern pattern, String str, Functions.Unary<Selection, String> func) {
        Matcher m = pattern.matcher(str);
        if(m.find()){
            String remaining = m.group(1);
            if(StringUtils.isNotEmpty(remaining)){
                return func.call(remaining);
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

    private String convertAttrValueType(Object val) {
        if(val.getClass().isAssignableFrom(byte[].class)) {
           return HexUtils.hexDump((byte[])val);
        }
        else if(val.getClass().isAssignableFrom(int.class)) {
            return Integer.toString((int)val);
        }
        else if(val.getClass().isAssignableFrom(String.class)) {
            return (String)val;
        }
        else {
            return val.toString();
        }
    }

    @Override
    public Class<SiteMinderContext> getContextObjectClass() {
        return SiteMinderContext.class;
    }
}
