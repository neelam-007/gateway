package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.odata4j.command.Command;
import org.odata4j.command.CommandContext;
import org.odata4j.command.CommandResult;

public class NoopCommand implements Command<CommandContext> {

  @Override
  public CommandResult execute(CommandContext context) throws Exception {
    return CommandResult.CONTINUE;
  }

}