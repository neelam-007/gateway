package com.l7tech.common;

import com.l7tech.server.BootMessages;
import com.l7tech.server.MessageProcessingMessages;
import com.l7tech.common.audit.AuditMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MessageMap {
    private static Collection allMessages = new ArrayList();
    private static MessageMap instance = null;

    private MessageMap() {
        // add all message categories here
        allMessages.add(new BootMessages());
        allMessages.add(new AuditMessages());
        allMessages.add(new MessageProcessingMessages());
    }

    public static MessageMap getInstance() {
        if(instance != null) return instance;
        return instance = new MessageMap();
    }

    public String getMessageById(int id) {
        String message = null;
        Iterator itr = allMessages.iterator();
        while (itr.hasNext()) {
            Messages messages = (Messages) itr.next();
            if((message = messages.getMessageById(id)) != null) {
                break;
            }
        }
        return message;
    }

    public String getSeverityLevelNameById(int id) {
        String levelName = null;
        Iterator itr = allMessages.iterator();
        while (itr.hasNext()) {
            Messages messages = (Messages) itr.next();
            if((levelName = messages.getSeverityLevelNameById(id)) != null) {
                break;
            }
        }
        return levelName;
    }
}
