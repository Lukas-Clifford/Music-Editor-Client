package app.musiceditorclient;

import app.musiceditorclient.commands.Command;
import app.musiceditorclient.commands.EditorCommand;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Stack;

public class CommandManager {
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    private final EditorContext context;
    private final EditorServices services;

    private final ObservableList<String> actionLog = FXCollections.observableArrayList();

    public CommandManager(EditorContext context, EditorServices services) {
        this.context = context;
        this.services = services;
    }


    public void executeCommand(EditorCommand command) {
        command.setContext(context);
        command.setServices(services);

        command.execute();
        undoStack.push(command);
        redoStack.clear();

        actionLog.addFirst("Execute: " + command.getDescription());
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);

            actionLog.addFirst("Undo: " + command.getDescription());
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);

            actionLog.addFirst("Redo: " + command.getDescription());
        }
    }

    public ObservableList<String> getActionLog() {
        return actionLog;
    }
}
