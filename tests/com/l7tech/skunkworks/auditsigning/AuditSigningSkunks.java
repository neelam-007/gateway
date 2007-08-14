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
            "d0483d1a7e86e61099056a0098a88864:1187041899550:FINE:Server:Server Starting:192.168.15.110:::-1:1200000:Starting:",
            "d0483d1a7e86e61099056a0098a88864:1187041902433:INFO:Server:Server Started:192.168.15.110:::-1:1200000:Started::2020:",
            "d0483d1a7e86e61099056a0098a88864:1187041978390:WARNING:SendSmsService [/foo]:Message processed successfully:127.0.0.1:::-1:0:000001146135cf10-0000000000000000:720896:sendSms:0::-1:-1:::200:0:",
            "d0483d1a7e86e61099056a0098a88864:1187042157519:INFO::User logged in:127.0.0.1:admin:3:-2:<none>:0:L:",
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
                    "</soapenv:Envelope>:<blahness/>:200:0:"
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
        String type;
        int nrRecords;
        public String toString() {
            return "Parsed audit record: " + parsed + "\n" +
                   "Signed: " + signed + "\n" +
                   "Type: " + type + "\n" +
                   "Number or records " + nrRecords;
        }
    }

    private static final String SEPARATOR_PATTERN = ":";
    private static ParsedSignedAuditRecord process(String input) {
        if (input == null) return null;
        input = input.trim();
        // make sure there are no non-escaped '\n'
        // todo
        // removed escaped \n characters (replace '\\\n' with '\n')
        input = input.replace("\\\n", "\n");
        
        ParsedSignedAuditRecord out = new ParsedSignedAuditRecord();
        out.raw = input;
        ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
        int pos = 0;
        int tmp = input.indexOf(SEPARATOR_PATTERN, pos);
        Long reqLength = null;
        Long resLength = null;
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp+1;
            int currentSize = separatorPositions.size();
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
            }
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

        // find out if we're looking at an AdminAuditRecord, a MessageSummaryAuditRecord or a SystemAuditRecord
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


        out.parsed = parsedTmp.toString();
        return out;
    }
}
