package com.l7tech.logging;

import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 17, 2003
 * Time: 1:39:25 PM
 * To change this template use Options | File Templates.
 */
public class LogMessage {

    private String time = null;
    private String severity = null;
    private String msgClass = null;
    private String msgMethod = null;
    private String msgDetail = null;

    public LogMessage(String log){

        StringTokenizer st = new StringTokenizer(log, "|");
        //System.out.println("number of fields is: " + fields.length);

        int index = 0;
        while(st.hasMoreTokens()){
          String s = st.nextToken();
               if(index == 0){
                   time = s;
                   index++;
               }
               else if (index == 1){
                   severity = s;
                   index++;
               }
               else if (index == 2) {
                   msgClass = s;
                   index++;
               }
                else if(index == 3){
                    msgMethod = s;
                    index++;
                }
                else if(index == 4){
                    msgDetail = s;
                    index++;
                }
                else
                {
                    msgDetail = msgDetail + s;
                }

          }

    }

    public String getTime() {
        return time;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessageClass() {
        return msgClass;
    }

    public String getMessageMethod() {
        return msgMethod;
    }

    public String getMessageDetail(){
        return msgDetail;
    }


    public static void main(String[] args) {
        LogMessage logRec = new LogMessage("20030917 13:07:36.281|INFO|com.l7tech.identity.internal.InternalIdentityProviderServer|authenticate|Couldn't find user with login admin");

        System.out.println("time: " + logRec.getTime());
        System.out.println("severity: " + logRec.getSeverity());
        System.out.println("msgClass: " + logRec.getMessageClass());
        System.out.println("msgMethod: " + logRec.getMessageMethod());
        System.out.println("msgDetail: " + logRec.getMessageDetail());

    }
}
