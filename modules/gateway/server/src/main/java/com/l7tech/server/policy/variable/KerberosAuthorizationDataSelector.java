package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.StringUtils;
import org.jaaslounge.decoding.kerberos.KerberosAuthData;
import org.jaaslounge.decoding.kerberos.KerberosEncData;
import org.jaaslounge.decoding.kerberos.KerberosPacAuthData;
import org.jaaslounge.decoding.kerberos.KerberosRelevantAuthData;
import org.jaaslounge.decoding.pac.Pac;
import org.jaaslounge.decoding.pac.PacLogonInfo;
import org.jaaslounge.decoding.pac.PacSid;
import org.jaaslounge.decoding.pac.PacSignature;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ymoiseyenko
 * Date: 12/9/11
 * Time: 9:01 AM
 * Variable selector that supports Kerberos Authorization Data sent by the AD domain controller with the kerberos ticket
 */
public class KerberosAuthorizationDataSelector implements ExpandVariables.Selector<KerberosEncData> {

    public static final String KERBEROS_DATA = "kerberos.data";
    public static final String KERBEROS_AUTHORIZATION ="authorizations";
    public static final String PAC = "pac";
    public static final String PAC_LOGON_INFO = PAC + ".logoninfo";
    public static final String PAC_KDC_SIGNATURE = PAC + ".kdc.signature";
    public static final String PAC_SERVER_SIGNATURE = PAC + ".server.signature";
    public static final String KERBEROS_RELEVANT_DATA = "relevant";

    protected static final Pattern KERBEROS_AUTHORIZATION_PATTERN = Pattern.compile("authorizations(\\.(\\d+)((\\.\\w{1,})*))?");

    @Override
    public Selection select(String contextName, KerberosEncData data, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Selection selection = null;

        final String lname = name.toLowerCase();

        if(null == data) return null;// empty data

        List<KerberosAuthData> authDataList = data.getUserAuthorizations();
        return selectAuthorizations(contextName, authDataList, lname, handler, strict);
    }

    @Override
    public Class<KerberosEncData> getContextObjectClass() {
        return KerberosEncData.class;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //- PRIVATE

    private Selection selectAuthorizations(String contextName, List<KerberosAuthData> authorizations, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Selection selection = null;
        Matcher matcher = KERBEROS_AUTHORIZATION_PATTERN.matcher(name);
        if(matcher.find()) {
            if(StringUtils.isNotEmpty(matcher.group(1))){
                KerberosAuthData context = getKerberosAuthData(authorizations, matcher, handler);
                if(StringUtils.isNotEmpty(matcher.group(3))){
                    String authDataPath = matcher.group(3).substring(1);//PAC data found
                    if(authDataPath.startsWith(PAC + ".") || authDataPath.equals(PAC)){
                        if(context instanceof KerberosPacAuthData){
                            KerberosPacAuthData pacAuthData = (KerberosPacAuthData)context;
                            if(authDataPath.startsWith(PAC_LOGON_INFO)){
                                selection = selectPacAuthData(context,authDataPath,handler,strict);
                            }
                            else if(authDataPath.startsWith(PAC_KDC_SIGNATURE) || authDataPath.startsWith(PAC_SERVER_SIGNATURE)){
                                selection = selectPacSignature(pacAuthData, authDataPath, handler, strict);
                            }
                        }
                    }
                    else if(authDataPath.startsWith(KERBEROS_RELEVANT_DATA)){
                        if(context instanceof KerberosRelevantAuthData) {
                            KerberosRelevantAuthData relevantAuthData = (KerberosRelevantAuthData)context;
                            List<KerberosAuthData> auths = relevantAuthData.getAuthorizations();
                            selection = selectAuthorizations(contextName, auths, authDataPath, handler, strict);
                        }
                    }
                }
                 else {
                    selection = new Selection(getKerberosAuthData(authorizations, matcher, handler), name.substring(KERBEROS_AUTHORIZATION.length()));
                }
            }
            else {
               selection = new Selection(authorizations, name);
            }
        }

        return selection;
    }


    private KerberosAuthData getKerberosAuthData(List<KerberosAuthData> authDataList, Matcher matcher, Syntax.SyntaxErrorHandler handler) {
        KerberosAuthData authData = null;
        int authIndex;
        try {
            authIndex = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            throw new RuntimeException(handler.handleBadVariable(matcher.group(), e));
        }
        if(null != authDataList){
            if(authIndex >= authDataList.size()){
                throw new RuntimeException(handler.handleSubscriptOutOfRange(authIndex, matcher.group(), authDataList.size()));
            }
            authData = authDataList.get(authIndex);

        }
        return authData;
    }

    private Selection selectPacSignature(KerberosPacAuthData context, String name, Syntax.SyntaxErrorHandler handler, boolean strict){
        if(name.startsWith(PAC_KDC_SIGNATURE)){
           PacSignature kdsSignature = context.getPac().getKdcSignature();
           return getSignatureAttributes(kdsSignature, name, PAC_KDC_SIGNATURE);
        }
        else if(name.startsWith(PAC_SERVER_SIGNATURE)){
           PacSignature serverSignature = context.getPac().getServerSignature();
           return getSignatureAttributes(serverSignature, name, PAC_SERVER_SIGNATURE);
        }
        return null;
    }


    private Selection getSignatureAttributes(PacSignature signature, final String name, final String prefix){
        if(name.equals(prefix + ".checksum"))
               return new Selection(HexUtils.encodeBase64(signature.getChecksum(), true));
        else if(name.equals(prefix + ".type"))
               return new Selection(signature.getType());
        return null;
    }

    private Selection selectPacAuthData(KerberosAuthData context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(context instanceof KerberosPacAuthData){
            Pac pac = ((KerberosPacAuthData)context).getPac();
            PacLogonInfo logonInfo = pac.getLogonInfo();
            if (name.startsWith(PAC_LOGON_INFO)){
                if(name.equals(PAC_LOGON_INFO + ".logontime"))
                    return new Selection(timeAsString(logonInfo.getLogonTime()));
                else if(name.equals(PAC_LOGON_INFO + ".logofftime"))
                    return new Selection(timeAsString(logonInfo.getLogoffTime()));
                else if(name.equals(PAC_LOGON_INFO + ".kickofftime"))
                    return  new Selection(timeAsString(logonInfo.getKickOffTime()));
                else if(name.equals(PAC_LOGON_INFO + ".pwdlastchangetime"))
                    return  new Selection(timeAsString(logonInfo.getPwdLastChangeTime()));
                else if(name.equals(PAC_LOGON_INFO + ".pwdcanchangetime"))
                    return  new Selection(timeAsString(logonInfo.getPwdCanChangeTime()));
                else if(name.equals(PAC_LOGON_INFO + ".pwdmustchangetime"))
                    return  new Selection(timeAsString(logonInfo.getPwdMustChangeTime()));
                else if(name.equals(PAC_LOGON_INFO + ".user.displayname"))
                    return  new Selection(logonInfo.getUserDisplayName());
                else if(name.equals(PAC_LOGON_INFO + ".user.name"))
                    return  new Selection(logonInfo.getUserName());
                else if(name.equals(PAC_LOGON_INFO + ".logonscript"))
                    return  new Selection(logonInfo.getLogonScript());
                else if(name.equals(PAC_LOGON_INFO + ".profilepath"))
                    return  new Selection(logonInfo.getProfilePath());
                else if(name.equals(PAC_LOGON_INFO + ".homedir"))
                    return  new Selection(logonInfo.getHomeDirectory());
                else if(name.equals(PAC_LOGON_INFO + ".homedrive"))
                    return  new Selection(logonInfo.getHomeDrive());
                else if(name.equals(PAC_LOGON_INFO + ".logoncount"))
                    return  new Selection(logonInfo.getLogonCount());
                else if(name.equals(PAC_LOGON_INFO + ".badpasswordcount"))
                    return  new Selection(logonInfo.getBadPasswordCount());
                else if(name.equals(PAC_LOGON_INFO + ".userid"))
                    return  new Selection(logonInfo.getUserSid().toString());
                else if(name.equals(PAC_LOGON_INFO + ".groupid"))
                    return  new Selection(logonInfo.getGroupSid().toString());
                else if(name.equals(PAC_LOGON_INFO + ".groupcount"))
                    return  new Selection(logonInfo.getGroupSids().length);
                else if(name.equals(PAC_LOGON_INFO + ".groupids"))
                    return  new Selection(getGroupIds(logonInfo.getGroupSids()));
                else if(name.equals(PAC_LOGON_INFO + ".user.flags"))
                    return  new Selection(logonInfo.getUserFlags());
                else if(name.equals(PAC_LOGON_INFO + ".servername"))
                    return  new Selection(logonInfo.getServerName());
                else if(name.equals(PAC_LOGON_INFO + ".domain"))
                    return  new Selection(logonInfo.getDomainName());
                else if(name.equals(PAC_LOGON_INFO + ".extrasids"))
                    return  new Selection(getGroupIds(logonInfo.getExtraSids()));
                else if(name.equals(PAC_LOGON_INFO + ".resourcesids"))
                    return new Selection(getGroupIds(logonInfo.getResourceGroupSids()));
                else if(name.equals(PAC_LOGON_INFO + ".user.accountcontrol"))
                    return new Selection(logonInfo.getUserAccountControl());
                else
                    return new Selection(logonInfo, name.substring(PAC_LOGON_INFO.length()));

            }
        }
        return null;
    }



     private static String timeAsString(Date time){
         return (null != time)?Long.toString(time.getTime()):"";
     }

     private static List<String> getGroupIds(PacSid[] pacSids) {
         List<String> sSids = new ArrayList<String>();
         if(null != pacSids){
             for(PacSid sid : pacSids){
                sSids.add(sid.toString());
             }
         }
         return sSids;
     }

}
