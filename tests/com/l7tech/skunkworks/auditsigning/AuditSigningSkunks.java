package com.l7tech.skunkworks.auditsigning;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

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
    private static final Logger logger = Logger.getLogger(AuditSigningSkunks.class.getName());

    private static String[] signable = {
            "d0483d1a7e86e61099056a0098a88864:1187115431578:FINE:Server:Server Starting:192.168.15.110:::-1:1200000:Starting:",
            "d0483d1a7e86e61099056a0098a88864:1187115434359:INFO:Server:Server Started:192.168.15.110:::-1:1200000:Started:[2020\\:Not starting FTP server (no listeners enabled).]",
            "d0483d1a7e86e61099056a0098a88864:1187115487162:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:0:000001146597ddae-0000000000000000:720896:sendSms:0::502:11:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "        <soapenv:Body>\n" +
                    "                <sendSms xmlns=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\">\n" +
                    "                        <destAddressSet/>\n" +
                    "                        <senderName>sender</senderName>\n" +
                    "                        <charging>charging</charging>\n" +
                    "                        <message>message</message>\n" +
                    "                </sendSms>\n" +
                    "        </soapenv:Body>\n" +
                    "</soapenv:Envelope>:<blahness/>:200:0:[-4\\:oh yea!,-4\\:blahness audit detail 2]",
            "d0483d1a7e86e61099056a0098a88864:1187115566163:INFO::User logged in:127.0.0.1:admin:3:-2:<none>:0:L:",
            "d0483d1a7e86e61099056a0098a88864:1187115609199:INFO:SendSmsService:PublishedService #720896 (SendSmsService) updated (changed policyXml):127.0.0.1:admin:3:-2:com.l7tech.service.PublishedService:720896:U:",
            "d0483d1a7e86e61099056a0098a88864:1187115634918:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:0:000001146597ddae-0000000000000001:720896:sendSms:0::-1:-1:::200:0:[-4\\:oh yea!,-4\\:blahness audit detail 2]"
    };
    private static String[] downloaded = {
            "2719744:d0483d1a7e86e61099056a0098a88864:1187115431578:FINE:Server:Server Starting:192.168.15.110:::-1:eyUelocvrZ8oB3KzlE5ThhP1R1ufloULJB9fbPjEl7B3f1i4MZsl34Mp8MqPiK3dTpBUTcq5sHXKYZHyyMSu5f84jvjKCYLtLR+LV/xMf5IjoKGiOy+FCAyb1jCk/dUJiNLUJQhBuopwI8vSDtJ14l9D9nivgS48Vf7TFbk7zRA=::::::::::::::::::2719744:1200000:Starting:",
            "2719745:d0483d1a7e86e61099056a0098a88864:1187115434359:INFO:Server:Server Started:192.168.15.110:::-1:EM8w44e+JciKgwQs4nmgE0L6lP6lKTwkD0cPab9yVSXHxjYNTSMheA4LL1o65MOFiMmg/GGLxdqp+r5WS+h6BZhF0zH4ET6xQ8ZoIa1yVbvufArWeliOekF8nxmwqArceYgYKITvZjvK8kE86NvaHeqz3JH5zfCoeuK2jkPpWlg=::::::::::::::::::2719745:1200000:Started:[2020\\:Not starting FTP server (no listeners enabled).]",
            "2719746:d0483d1a7e86e61099056a0098a88864:1187115487162:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:etfcqBkN9DAa0Z4xMFuCWuk/CAfmTMNuwPDD+eryyEIlS8bAKcxqj9qVrPIysJSXl8mGpr65WtfgcZ7PKaDk+uXOqvnD+nPZgSke3R/62KO6H4/B3ufgHIh5AbG0rAgVJQRrmm1qxNJRZ66u3ChLBlQivP+mpPCDFI4WUpRgeKw=:::::2719746:0:000001146597ddae-0000000000000000:720896:sendSms:0::502:11:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\\\n" +
                    "<soapenv\\:Envelope xmlns\\:soapenv=\"http\\://schemas.xmlsoap.org/soap/envelope/\">\\\n" +
                    "        <soapenv\\:Body>\\\n" +
                    "                <sendSms xmlns=\"http\\://www.csapi.org/schema/parlayx/sms/send/v1_0/local\">\\\n" +
                    "                        <destAddressSet/>\\\n" +
                    "                        <senderName>sender</senderName>\\\n" +
                    "                        <charging>charging</charging>\\\n" +
                    "                        <message>message</message>\\\n" +
                    "                </sendSms>\\\n" +
                    "        </soapenv\\:Body>\\\n" +
                    "</soapenv\\:Envelope>:<blahness/>:200:0::::[-4\\:oh yea!,-4\\:blahness audit detail 2]",
            "2719747:d0483d1a7e86e61099056a0098a88864:1187115566163:INFO::User logged in:127.0.0.1:admin:3:-2:SVgiceaAHTKs/F71mWoBRQQcf2cWWqIYxVE++XPpC45onufEwkeQojm45aYety8gfuStoigVG86SPtUrm9L8CqIakAhi7skXndaOfcRx97wlK3JHmAcSozl0u/x5mut84g2nCZTwKh6RvQb0yZYblkuYs7TySW0XVWomDTORcPM=:2719747:<none>:0:L:::::::::::::::::",
            "2719748:d0483d1a7e86e61099056a0098a88864:1187115609199:INFO:SendSmsService:PublishedService #720896 (SendSmsService) updated (changed policyXml):127.0.0.1:admin:3:-2:L7LOy0NJsgcqjJgiPeVMkKx68JxFcurbC6OaPohnYk+cGqeWuRYJwPfRpCPHcnSFvIhmdaNEWyKaIFZIKAvZqXAuxcY7+1oHh337Lpdnha6zl2izFxtL4MeGUZFmAIUdcY0SArm8es9mlXAfdBIf8g5gIlKaml+ERQ195zSdybM=:2719748:com.l7tech.service.PublishedService:720896:U:::::::::::::::::",
            "2719749:d0483d1a7e86e61099056a0098a88864:1187115634918:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:PGc0QM5m07xxU1iDrIKQtwc+Tc1SEroPNIlmmlcVWl2tMKJVuA3zRQF3f5YolxxJn/tw+iyUoGyFgvfozjjLf0wrL1DXPSw1yB/Hs2fUH5uo4789fyjHWxLH1wScIpNjzclcfu0lufxp4vGE2vj9dCgZOYRZSUsMU76j70/Eg5A=:::::2719749:0:000001146597ddae-0000000000000001:720896:sendSms:0::-1:-1:::200:0::::[-4\\:oh yea!,-4\\:blahness audit detail 2]"
    };

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < downloaded.length; i++) {
            ParsedSignedAuditRecord p = process(downloaded[i]);
            if (!signable[i].equals(p.parsed)) {
                System.out.println("ERROR, parsed record does not match downloaded audit record: " + p);
            } else {
                System.out.println("Perfect match");
            }
        }
    }

    public static class ParsedSignedAuditRecord {
        String signature;
        String raw;
        String parsed;
        boolean signed = false;
        String type;
        int nrRecords;
        public String toString() {
            return "Parsed audit record: " + parsed + "\n" +
                   "Signed: " + signed + "\n" +
                   "Type: " + type + "\n" +
                   "Number or records " + nrRecords;
        }
    }

    private static int nextUnescapedSeparator(String input, int startPos) {
        int res = input.indexOf(SEPARATOR_PATTERN, startPos);
        if (res < 1) return res;
        if (input.charAt(res-1) == '\\') {
            return nextUnescapedSeparator(input, res+1);
        } else return res;
    }

    private static final String SEPARATOR_PATTERN = ":";
    private static ParsedSignedAuditRecord process(String input) {
        if (input == null) return null;
        input = input.trim();
        // removed escaped \n characters (replace '\\\n' with '\n')
        input = input.replace("\\\n", "\n");
        
        ParsedSignedAuditRecord out = new ParsedSignedAuditRecord();
        out.raw = input;
        ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
        int pos = 0;
        int tmp = nextUnescapedSeparator(input, pos);
        /*Long reqLength = null;
        Long resLength = null;*/
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp+1;
            /*int currentSize = separatorPositions.size();
            // 0 based index of separators 21_req_length_22_res_length_23_reqxml_24_resxml
            // lookout for xml which contains a whole bunch of ':' separators
            switch (currentSize) {
                case 22:
                    tmp = input.indexOf(SEPARATOR_PATTERN, pos);
                    // check if a request length is specified
                    if ((tmp - pos) > 1) {
                        String maybelength = input.substring(pos, tmp);
                        try {
                            reqLength = Long.parseLong(maybelength);
                        } catch (NumberFormatException e) {
                            logger.log(Level.SEVERE, "Expected request length at " + maybelength);
                        }
                    }
                    break;
                case 23:
                    tmp = input.indexOf(SEPARATOR_PATTERN, pos);
                    // check if a response length is specified
                    if ((tmp - pos) > 1) {
                        String maybelength = input.substring(pos, tmp);
                        try {
                            resLength = Long.parseLong(maybelength);
                        } catch (NumberFormatException e) {
                            logger.log(Level.SEVERE, "Expected response length at " + maybelength);
                        }
                    }
                    break;
                case 24:
                    if (reqLength != null && reqLength > 0) {
                        tmp = input.indexOf(SEPARATOR_PATTERN, pos + reqLength.intValue());
                        String maybeXML = input.substring(pos, tmp);
                        logger.fine("XML parsed out " + maybeXML);
                    } else {
                        // no request length specified, not expecting xml here
                        tmp = input.indexOf(SEPARATOR_PATTERN, pos);
                    }
                    break;
                case 25:
                    if (resLength != null && resLength > 0) {
                        tmp = input.indexOf(SEPARATOR_PATTERN, pos + resLength.intValue());
                        String maybeXML = input.substring(pos, tmp);
                        logger.fine("XML parsed out " + maybeXML);
                    } else {
                        // no response length specified, not expecting xml here
                        tmp = input.indexOf(SEPARATOR_PATTERN, pos);
                    }
                    break;
                default:
                    tmp = input.indexOf(SEPARATOR_PATTERN, pos);
            }*/
            tmp = nextUnescapedSeparator(input, pos);
        }

        out.nrRecords = separatorPositions.size();
        if (out.nrRecords < 30) {
            throw new RuntimeException("This does not appear to be a valid audit record (" + out.nrRecords + ")");
        }

        StringBuffer parsedTmp = new StringBuffer();

        // extract signature, remove initial ID and signature the signature starts after the 10th ':' and has a length of 173
        out.signature = input.substring(separatorPositions.get(9) + 1, separatorPositions.get(10));
        if (out.signature == null || out.signature.length() < 1) {
            // we're dealing with a record which does not contain a signature
            out.signed = false;
        } else if (out.signature.length() != 172) {
            throw new IllegalArgumentException("Unexpected signature length " + out.signature.length() + ". " + out.signature);
        } else {
            out.signed = true;
        }
        parsedTmp.append(input.substring(separatorPositions.get(0) +1, separatorPositions.get(9)));

        // append either the AdminAuditRecord, MessageSummaryAuditRecord or the SystemAuditRecord
        boolean isadminrecord = false;
        boolean ismsgsummaryrecord = false;
        boolean issystemrecord = false;
        int tmpstart = separatorPositions.get(10);
        int tmpend = separatorPositions.get(11);
        if ((tmpend - tmpstart) > 1) {
            isadminrecord = true;
            if (out.type != null) {
                throw new RuntimeException("record cannot be admin AND " + out.type);
            }
            out.type = "Admin";
            parsedTmp.append(input.substring(separatorPositions.get(11), separatorPositions.get(14)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(14);
        tmpend = separatorPositions.get(15);
        if ((tmpend - tmpstart) > 1) {
            ismsgsummaryrecord = true;
            if (out.type != null) {
                throw new RuntimeException("record cannot be summary AND " + out.type);
            }
            out.type = "Msg Summary";
            parsedTmp.append(input.substring(separatorPositions.get(15), separatorPositions.get(27)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(27);
        tmpend = separatorPositions.get(28);
        if ((tmpend - tmpstart) > 1) {
            issystemrecord = true;
            if (out.type != null) {
                throw new RuntimeException("record cannot be system AND " + out.type);
            }
            out.type = "System";
            parsedTmp.append(input.substring(separatorPositions.get(28), separatorPositions.get(30)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }

        // Unescape : separators until now
        String parsingResult = parsedTmp.toString();
        parsingResult = parsingResult.replace("\\:", ":");


        // Append the audit details if any
        tmpstart = input.indexOf("[", separatorPositions.get(30));
        if (tmpstart > 0) {
            parsingResult = parsingResult + input.substring(tmpstart);
        }

        out.parsed = parsingResult;
        return out;
    }
}
