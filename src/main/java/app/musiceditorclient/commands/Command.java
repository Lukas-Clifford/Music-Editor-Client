package app.musiceditorclient.commands;

public interface Command {
    void execute();
    void undo();
}
