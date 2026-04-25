package app.musiceditorclient.commands;

import app.musiceditorclient.EditorContext;
import app.musiceditorclient.EditorServices;
import javafx.event.Event;

public abstract class EditorCommand implements Command{

    protected EditorContext context;
    protected EditorServices services;
    protected final Event event;

    public EditorCommand(Event event) {
        this.event = event;
    }

    public void setContext(EditorContext context) {
        this.context = context;
    }

    public void setServices(EditorServices services) {
        this.services = services;
    }
}
