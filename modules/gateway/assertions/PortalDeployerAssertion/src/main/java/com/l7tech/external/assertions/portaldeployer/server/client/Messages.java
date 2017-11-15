package com.l7tech.external.assertions.portaldeployer.server.client;

import java.util.ArrayList;
import java.util.List;

/**
 * @author raqri01, 2017-10-20
 */
public class Messages {

  public Messages() {
    messages = new ArrayList<Message>();
  }

  public List<Message> getMessages() {
    return messages;
  }

  public void setMessages(List<Message> messages) {
    this.messages = messages;
  }

  private List<Message> messages;
}
