package com.paytrace.patterns.command;

/**
 * Command pattern — every reversible action implements this interface.
 */
public interface Command {
    void execute() throws Exception;
    void undo() throws Exception;
    String getDescription();
}