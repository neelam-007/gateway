package com.l7tech.common.audit;

import com.l7tech.server.AssertionMessages;
import com.l7tech.server.BootMessages;
import com.l7tech.server.MessageProcessingMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

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
        allMessages.add(new MessageProcessingMessages());
        allMessages.add(new AssertionMessages());
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
        Level level;

        if((level = getSeverityLevelById(id)) != null) {
            return level.getName();
        } else {
            return null;
        }
    }

    public Level getSeverityLevelById(int id) {
        Level level = null;
        Iterator itr = allMessages.iterator();
        while (itr.hasNext()) {
            Messages messages = (Messages) itr.next();
            if((level = messages.getSeverityLevelById(id)) != null) {
                break;
            }
        }
        return level;
    }
}
