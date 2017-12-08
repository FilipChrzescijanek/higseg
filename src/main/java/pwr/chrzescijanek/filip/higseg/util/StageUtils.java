package pwr.chrzescijanek.filip.higseg.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import pwr.chrzescijanek.filip.higseg.controller.ImageController;
import pwr.chrzescijanek.filip.higseg.view.FXView;

/**
 * Provides utility methods for handling stages.
 */
public final class StageUtils {

	private StageUtils() { }

	/**
	 * @param window application window
	 * @return new undecorated modal dialog
	 */
	public static Stage initDialog(final Window window) {
		final Stage dialog = new Stage();
		dialog.initOwner(window);
		dialog.initStyle(StageStyle.UNDECORATED);
		dialog.initModality(Modality.APPLICATION_MODAL);
		return dialog;
	}

	/**
	 * @return about dialog
	 */
	public static Alert getAboutDialog() {
		final Alert alert = new Alert(AlertType.INFORMATION,
		                              "Histopathological images segmentator\n" +
		                              "GitHub repository: https://github.com/FilipChrzescijanek/higseg/\n" +
		                              "\nCopyright © 2018 Filip Chrześcijanek\nfilip.chrzescijanek@gmail.com",
		                              ButtonType.OK);
		alert.setTitle("About");
		alert.setHeaderText("higseg");
		alert.setGraphic(new ImageView(new Image(StageUtils.class.getResourceAsStream("/images/icon-big.png"))));
		((Stage) alert.getDialogPane().getScene().getWindow())
				.getIcons().add(new Image(StageUtils.class.getResourceAsStream("/images/icon-small.png")));
		return alert;
	}

	/**
	 * @param content alert content
	 * @return error alert
	 */
	public static Alert getErrorAlert(final String content) {
		return new Alert(AlertType.ERROR, content, ButtonType.OK);
	}

	/**
	 * Constructs new application's compare view's stage with FXML view from given path, label, title and compare views
	 * of vertex with given index.
	 *
	 * @param viewPath    FXML view path
	 * @param title       stage title
	 * @return new application's compare stage
	 */
	public static ImageController loadImageStage(final Stage stage, final String viewPath, final String title) {
		final FXView fxView = new FXView(viewPath);
		final ImageController controller = (ImageController) fxView.getController();
		prepareStage(stage, title, fxView);
		return controller;
	}
	
	private static void setTitleAndIcon(final Stage stage, final String title) {
		stage.setTitle(title);
		final Image icon = new Image(StageUtils.class.getResourceAsStream("/images/icon-small.png"));
		stage.getIcons().add(icon);
	}

	private static void setScene(final Stage stage, final Parent root, final int width, final int height) {
		final Scene scene = new Scene(root, width, height);
		stage.setScene(scene);
	}

	/**
	 * Prepares application's help view stage.
	 *
	 * @param stage stage on which help will be shown
	 * @param root  help view
	 */
	public static void prepareHelpStage(final Stage stage, final Parent root) {
		setTitleAndIcon(stage, "Help");
		setScene(stage, root, 960, 540);
	}

	/**
	 * Prepares application stages.
	 *
	 * @param stage stage on which given view will be shown
	 * @param title stage title
	 * @param view  FXML view
	 */
	public static void prepareStage(final Stage stage, final String title, final FXView view) {
		setTitleAndIcon(stage, title);
		if (title.equals("higseg")) {
			setScene(stage, view, 480, 300);
		} else {
			setScene(stage, view, 800, 600);
		}
	}

	private static void setScene(final Stage stage, final FXView fxView, final int width, final int height) {
		final Parent root = fxView.getView();
		final Scene scene = new Scene(root, width, height);
		stage.setScene(scene);
	}

}
