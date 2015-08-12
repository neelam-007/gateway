package com.l7tech.internal.signer.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 * The command registry contains the different commands available for the Skar Signer.<br/>
 * For now this tool can only sign files i.e. only {@link SignCommand} is registered.
 */
public final class CommandRegistry {
    private static final TreeMap<String, Command> registeredCommands = new TreeMap<>();

    // for now this tool can only sign files
    static {
        Command command = new SignCommand();
        registeredCommands.put(command.getName(), command);

        command = new EncodePasswordCommand();
        registeredCommands.put(command.getName(), command);
    }

    /**
     * A read only view of registered commands.
     */
    @NotNull
    public static Collection<Command> getCommands() {
        return Collections.unmodifiableCollection(registeredCommands.values());
    }

    /**
     * Check whether the command, specified with the command name, is registered.
     *
     * @param commandName    the command name.  Required and cannot be {@code null}.
     */
    public static boolean isCommandRegistered(@NotNull final String commandName) {
        return registeredCommands.containsKey(commandName);
    }

    /**
     * Get the command specified with the command name.
     *
     * @param commandName    the command name.  Required and cannot be {@code null}.
     */
    @Nullable
    public static Command getCommand(@NotNull final String commandName) {
        return registeredCommands.get(commandName);
    }
}
