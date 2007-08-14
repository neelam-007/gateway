package com.l7tech.skunkworks.auditsigning;

import java.util.ArrayList;

/**
 * [todo jdoc this class]
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 13, 2007<br/>
 */
public class AuditSigningSkunks {
    private static String[] signable = {
            "d0483d1a7e86e61099056a0098a88864:1187041899550:FINE:Server:Server Starting:192.168.15.110:::-1:1200000:Starting::",
            "d0483d1a7e86e61099056a0098a88864:1187041902433:INFO:Server:Server Started:192.168.15.110:::-1:1200000:Started::2020::",
            "d0483d1a7e86e61099056a0098a88864:1187041978390:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:0:000001146135cf10-0000000000000000:720896:sendSms:0::-1:-1:::200:0::",
            "d0483d1a7e86e61099056a0098a88864:1187042157519:INFO::User logged in:127.0.0.1:admin:3:-2:<none>:0:L::",
            "d0483d1a7e86e61099056a0098a88864:1187049036804:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:0:000001146135cf10-0000000000000001:720896:sendSms:0::502:11:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "        <soapenv:Body>\n" +
                    "                <sendSms xmlns=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\">\n" +
                    "                        <destAddressSet/>\n" +
                    "                        <senderName>sender</senderName>\n" +
                    "                        <charging>charging</charging>\n" +
                    "                        <message>message</message>\n" +
                    "                </sendSms>\n" +
                    "        </soapenv:Body>\n" +
                    "</soapenv:Envelope>:<blahness/>:200:0::"
    };
    private static String[] downloaded = {
            "2228224:d0483d1a7e86e61099056a0098a88864:1187041899550:FINE:Server:Server Starting:192.168.15.110:::-1:X3rb8HEe0PU3vfSmqpyaGZIsH4iEmjsr0H7XZDNgeUyJRJSFGibQ35yDoZVlrNXwtJsKkSfGGGFkgq63QX1loP6cxXF0CDgoOzuqtuPzY8Mhp0hdeS0jmZc+mxsTl/F7NDuSn5sz5EuZ6L/muvTqTnLHeeWv+sjNpQYBWwI5FgI=::::::::::::::::::2228224:1200000:Starting:",
            "2228225:d0483d1a7e86e61099056a0098a88864:1187041902433:INFO:Server:Server Started:192.168.15.110:::-1:AKjnIxnu5AFZ2rfJNgs/EY0rioaV1gJaqjngqoQJveeqzS1ZOZuMh+8OIH/+mgsARtYnLXLl9vE2/akWaRVyBf/cHWsdgVjPLhJQUfd7jUnfCql5bT6idEsIKneyTAh7kzGKzetJxvFm2apJuPMiXm9psGaxfaeq5koy4Wrzd6Q=::::::::::::::::::2228225:1200000:Started:",
            "2228226:d0483d1a7e86e61099056a0098a88864:1187041978390:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:MGI3ebCuf+9jrAHEAVl12CDoyYk1JDvK2yEdBQA3+RvAiTacFrBBaI+L6QarfmAbqQgXJLlQzJkBzKa/kTHoNWC8wGIcqn1PkDeRrFMs4oTWyN1JBToOMprhAibl73hGOfFmPK9hXxpl/yDmDIXqXqqe6lnRrQrG+9nHCSyChE0=:::::2228226:0:000001146135cf10-0000000000000000:720896:sendSms:0::-1:-1:::200:0::::",
            "2228227:d0483d1a7e86e61099056a0098a88864:1187042157519:INFO::User logged in:127.0.0.1:admin:3:-2:ALli4SHl+hcdGORUGDbxQDXCRtWWL01G0kLC3LdGmoBdcUVtmnQ7G7G+6wgot2tbw6oYqtaFZYSSKVGx1uyEKrHwVZLPdjK4j1SvCI3N6T6/bA0qyG72mkVbc/rH9f5+qqHeuT3nfcVvDHe+wGTe/Q2Zaf2XV2599n7p7G+u4Zk=:2228227:<none>:0:L:::::::::::::::::",
            "2228231:d0483d1a7e86e61099056a0098a88864:1187049036804:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:GmyQWGRY01Eiv5d9/vl+nnu09TmIuHtSTgtREmz1PiqetHP8JiK0wqOQXP5tN0ZjxFCnrRAviCLnele9YCf+Ihw6p/Tgrgn4qZuCVCL41IGFMFvWi7r8GkW5QyXaa8Z6cZkpfmX/cW2cPyGn3ywlKTV645x0OAICdJsFHr1Dd9o=:::::2228231:0:000001146135cf10-0000000000000001:720896:sendSms:0::502:11:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\\\n" +
                    "<soapenv\\:Envelope xmlns\\:soapenv=\"http\\://schemas.xmlsoap.org/soap/envelope/\">\\\n" +
                    "        <soapenv\\:Body>\\\n" +
                    "                <sendSms xmlns=\"http\\://www.csapi.org/schema/parlayx/sms/send/v1_0/local\">\\\n" +
                    "                        <destAddressSet/>\\\n" +
                    "                        <senderName>sender</senderName>\\\n" +
                    "                        <charging>charging</charging>\\\n" +
                    "                        <message>message</message>\\\n" +
                    "                </sendSms>\\\n" +
                    "        </soapenv\\:Body>\\\n" +
                    "</soapenv\\:Envelope>:<blahness/>:200:0::::"
    };

    public static void main(String[] args) throws Exception {
        for (String s : downloaded) {
            System.out.println("\n\n" + process(s));
        }
    }

    public static class ParsedSignedAuditRecord {
        String signature;
        String raw;
        String parsed;
        boolean signed = false;
        public String toString() {
            return "Parsed audit record: " + parsed + "\n" +
                   "Signed: " + signed;
        }
    }

    private static ParsedSignedAuditRecord process(String input) {
        if (input == null) return null;
        input = input.trim();
        // make sure there are no non-escaped '\n'
        // todo
        // removed escaped \n characters (replace '\\\n' with '\n')
        // todo
        
        ParsedSignedAuditRecord out = new ParsedSignedAuditRecord();
        out.raw = input;
        ArrayList separatorPositions = new ArrayList();
        int pos = 0;
        int tmp = input.indexOf(":", pos);
        Integer reqLength = null;
        Integer resLength = null;
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp+1;
            if (separatorPositions.size() >= 22 && separatorPositions.size() <= 25) {
                // 0 based index of separators 21_req_length_22_res_length_23_reqxml_24_resxml
                // lookout for xml which contains a whole bunch of ':' separators
                // todo, use previously recorded length for request and response xml
            }
            tmp = input.indexOf(":", pos);
        }

        StringBuffer parsedTmp = new StringBuffer();

        // extract signature, remove initial ID and signature the signature starts after the 10th ':' and has a length of 173
        out.signature = input.substring((Integer)(separatorPositions.get(9)) + 1, (Integer)(separatorPositions.get(10)));
        if (out.signature == null || out.signature.length() < 1) {
            // we're dealing with a record which does not contain a signature
            out.signed = false;
        }
        if (out.signature.length() != 173) {
            throw new IllegalArgumentException("Unexpected signature length " + out.signature.length() + ". " + out.signature);
        }
        parsedTmp.append(input.substring((Integer)(separatorPositions.get(0))+1, (Integer)(separatorPositions.get(9))));

        // find out if we're looking at an AdminAuditRecord, a MessageSummaryAuditRecord or a SystemAuditRecord
        int tmpstart = (Integer)(separatorPositions.get(10));
        int tmpend = (Integer)(separatorPositions.get(11));
        boolean isadminrecord = false;
        boolean ismsgsummaryrecord = false;
        boolean issystemrecord = false;
        if ((tmpend - tmpstart) > 1) {
            isadminrecord = true;
        }
        // todo, check for other types

        out.parsed = parsedTmp.toString();
        return out;
    }
}
