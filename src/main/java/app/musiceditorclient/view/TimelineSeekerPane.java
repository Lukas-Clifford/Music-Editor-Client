package app.musiceditorclient.view;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class TimelineSeekerPane extends Pane {

    public SimpleIntegerProperty seekerPosition; // place it aims in timeline (Milliseconds)
    private Rectangle seekerRectangle;

    private ScrollPane timelineScrollPane;
    private Pane timelineSeekerBodyPane;

    public FloatProperty zoomFactor;


    public TimelineSeekerPane(FloatProperty zoomFactor) {

        prefWidthProperty().bind(zoomFactor.multiply(600_000)); // max length 10 mins
        prefHeightProperty().bind(zoomFactor.multiply(25));

        timelineSeekerBodyPane = new Pane();

        timelineSeekerBodyPane.prefWidthProperty().bind(this.prefWidthProperty());


        // Seeker config
        seekerPosition = new SimpleIntegerProperty(0);
        seekerRectangle = new Rectangle(0,0,2,50);
        seekerRectangle.xProperty().bind(seekerPosition.multiply(zoomFactor));

//        seekerPosition.addListener((observable, oldValue, newValue) -> System.out.println(oldValue + " -> " + newValue));

        timelineSeekerBodyPane.getChildren().add(seekerRectangle);


        timelineScrollPane = new ScrollPane(timelineSeekerBodyPane);

        getChildren().add(timelineScrollPane);


        timelineScrollPane.addEventFilter(ScrollEvent.SCROLL, Event::consume);
        timelineScrollPane.setMouseTransparent(true);
        timelineScrollPane.setFocusTraversable(false);
        timelineScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        timelineScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        timelineScrollPane.getContent().setStyle("-fx-background-color:lightgray;");


    }

    public ScrollPane getTimelineScrollPane() {
        return timelineScrollPane;
    }
}
