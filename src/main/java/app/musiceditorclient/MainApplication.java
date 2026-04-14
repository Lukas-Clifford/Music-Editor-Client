package app.musiceditorclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        MainController controller = fxmlLoader.getController();

        stage.setTitle("Music Editor");
        stage.setMaximized(true);
        stage.setFullScreen(false);
        stage.setScene(scene);

        scene.setOnScroll(event -> {
            if (event.isControlDown()) {
                controller.zoomFactor.set(
                        (float) (controller.zoomFactor.get() + (Math.signum(event.getDeltaY()) / 10))
                );
                event.consume();

            } else if (event.isShiftDown()) {

                float nextOffset = controller.clipStartOffset.get() + (float) Math.signum(event.getDeltaX()) * 50;
                controller.clipStartOffset.set(Math.max(0f, nextOffset));
                event.consume();

            }
        });

        stage.show();
    }
}
