package app.musiceditorclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    private final EditorContext context = new EditorContext();
    private final EditorServices services = new EditorServices(context);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        MainController controller = new MainController(context, services);
        fxmlLoader.setController(controller);

        Scene scene = new Scene(fxmlLoader.load());

        services.projectPersistenceService().setOnProjectLoadedListener(projectPath ->
                stage.setTitle(projectPath.getFileName().toString().replace(".musicproject", "")));

        stage.setTitle("Music Editor");
        stage.setMaximized(true);
        stage.setFullScreen(false);
        stage.setScene(scene);

        scene.setOnScroll(event -> {
            if (event.isControlDown()) {
                context.ui().zoomFactorProperty().set(
                        (float) (context.ui().zoomFactorProperty().get() + (Math.signum(event.getDeltaY()) / 100))
                );
                event.consume();

            } else if (event.isShiftDown()) {

                float nextOffset = context.ui().clipStartOffsetProperty().get() + (float) Math.signum(event.getDeltaX()) * 50;
                context.ui().clipStartOffsetProperty().set(Math.max(0f, nextOffset));
                event.consume();

            }
        });

        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S)
                services.projectPersistenceService().saveProject();

            if (event.isShiftDown() && event.getCode() == KeyCode.ESCAPE)
                stage.close();

            if (event.getCode() == KeyCode.CONTROL)
                services.selectionService().enableSelection();

            if (event.isShiftDown() && event.isControlDown() && event.getCode() == KeyCode.A)
                services.selectionService().clearSelection();

        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                controller.onPlay();
                event.consume();
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.CONTROL)
                services.selectionService().disableSelection();
        });

        stage.show();
    }
}
