package com.paytrace.patterns.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Singleton stack-based command history. Keeps the last N commands
 * for undo, and a redo stack for re-applying.
 */
public class CommandHistory {

    private static final CommandHistory INSTANCE = new CommandHistory();
    public static CommandHistory getInstance() { return INSTANCE; }

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    private CommandHistory() {}

    public void executeAndStore(Command cmd) throws Exception {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();   // any new action invalidates redo history
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public String undoLast() throws Exception {
        if (undoStack.isEmpty()) throw new Exception("Nothing to undo.");
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        return c.getDescription();
    }

    public String redoLast() throws Exception {
        if (redoStack.isEmpty()) throw new Exception("Nothing to redo.");
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
        return c.getDescription();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}