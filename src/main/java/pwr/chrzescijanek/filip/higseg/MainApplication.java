package pwr.chrzescijanek.filip.higseg;

import static pwr.chrzescijanek.filip.higseg.util.StageUtils.prepareStage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.higseg.inject.Injector;
import pwr.chrzescijanek.filip.higseg.view.FXView;

public class MainApplication extends Application {

	/**
	 * Prepares primary stage and shows GUI.
	 *
	 * @param primaryStage application's primary stage
	 * @throws Exception unhandled exception
	 * 
	 */
	@Override
	public void start(final Stage primaryStage) throws Exception {
		final FXView fxView = new FXView("/static/main.fxml");
		prepareStage(primaryStage, "higseg", fxView);
		primaryStage.setOnCloseRequest(event -> Platform.exit());
		primaryStage.show();
	}

	/**
	 * Resets state on application stop.
	 *
	 * @throws Exception unhandled exception
	 */
	@Override
	public void stop() throws Exception {
		Injector.reset();
	}

}
