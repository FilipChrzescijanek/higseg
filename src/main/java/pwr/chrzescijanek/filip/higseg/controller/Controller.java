package pwr.chrzescijanek.filip.higseg.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.getDirectory;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.getImageFiles;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.startTask;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {

	private final ObservableList<ImageController> controllers = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

	@FXML GridPane root;
	@FXML MenuBar menuBar;
	@FXML Menu fileMenu;
	@FXML MenuItem fileMenuExportToPng;
	@FXML MenuItem fileMenuExit;
	@FXML Menu alignMenu;
	@FXML MenuItem alignMenuLoadImages;
	@FXML MenuItem alignMenuClearImages;
	@FXML MenuItem runMenuAlign;
	@FXML MenuItem runMenuCalculateResults;
	@FXML Menu optionsMenu;
	@FXML Menu optionsMenuTheme;
	@FXML RadioMenuItem optionsMenuThemeDark;
	@FXML ToggleGroup themeToggleGroup;
	@FXML RadioMenuItem optionsMenuThemeLight;
	@FXML Menu helpMenu;
	@FXML MenuItem helpMenuHelp;
	@FXML MenuItem helpMenuAbout;
	@FXML GridPane alignMainPane;
	@FXML VBox alignLeftVBox;
	@FXML Button loadImagesButton;
	@FXML VBox alignCenterVBox;
	@FXML Button grayscaleButton;
	@FXML VBox alignRightVBox;
	@FXML Button thresholdButton;

	@FXML
	void about() {
		final Alert alert = StageUtils.getAboutDialog();
		final DialogPane dialogPane = alert.getDialogPane();
		injectStylesheets(dialogPane);
		alert.show();
	}
	
	@FXML
	void applyDarkTheme() {
		setDarkTheme();
	}
	
	@FXML
	void applyLightTheme() {
		setLightTheme();
	}
	
	@FXML
	void exit() {
		root.getScene().getWindow().hide();
	}

	@FXML
	void loadImages() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null) {
			final Task<? extends Void> task = createLoadImagesTask(selectedFiles);
			startTask(task);
		}
	}
	
	private Task<? extends Void> createLoadImagesTask(final List<File> selectedFiles) {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				loadImages(selectedFiles);
				return null;
			}
		};
	}
	
	private void loadImages(final List<File> selectedFiles) {
		for (final File f : selectedFiles) {
			final String filePath;
			try {
				filePath = f.getCanonicalPath();
				final Mat image = getImage(filePath);
				addNewImage(filePath, image);
			} catch (IOException | CvException e) {
				handleException(e,
				                "Loading failed!\nImages might be corrupted, paths may contain non-ASCII symbols or "
				                + "you do not have sufficient read permissions.");
				break;
			}
		}
	}
	
	private Mat getImage(final String filePath) {
		final Mat image = Imgcodecs.imread(filePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
		if (image.dataAddr() == 0)
			throw new CvException("Failed to load image! Check if file path contains only ASCII symbols");
		return image;
	}
	
	private void addNewImage(final String filePath, final Mat image) {
		final String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
		Platform.runLater(() -> {
            final Stage newStage = new Stage();
            final String viewPath = "/static/image.fxml";
            final ImageController controller = StageUtils.loadImageStage(newStage, viewPath, fileName);
            controllers.add(controller);
            newStage.setOnHidden(e -> {
                controllers.remove(controller);
            });
            controller.setImage(image);
            newStage.show();
		});
	}
	
	@FXML
	void clearImages() {
        final Task<? extends Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            	while(!controllers.isEmpty()) {
            		Platform.runLater(() -> controllers.get(0).exit());
            	}
                return null;
            }
        };
        startTask(task);
	}
	
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		
		initializeComponents(location, resources);
		setBindings();
	}
	
	private void setBindings() {
		setEnablementBindings();
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		initializeStyle();
		setTooltips();
	}
	
	private void initializeStyle() {
		injectStylesheets(root);
		if (isLightThemeSelected()) {
			themeToggleGroup.selectToggle(optionsMenuThemeLight);
		}
		else {
			themeToggleGroup.selectToggle(optionsMenuThemeDark);
		}
	}

	
	private void setTooltips() {
		setImagesControlsTooltips();
	}
	
	private void setImagesControlsTooltips() {
		loadImagesButton.setTooltip(new Tooltip("Load images"));
		grayscaleButton.setTooltip(new Tooltip("Grayscale"));
		thresholdButton.setTooltip(new Tooltip("Threshold"));
	}
	
	private void setEnablementBindings() {
		final BooleanBinding noImages = Bindings.isEmpty(controllers);

		fileMenuExportToPng.disableProperty().bind(noImages);
		alignMenuClearImages.disableProperty().bind(noImages);
		runMenuAlign.disableProperty().bind(noImages);
		runMenuCalculateResults.disableProperty().bind(noImages);
		grayscaleButton.disableProperty().bind(noImages);
		thresholdButton.disableProperty().bind(noImages);
	}

	@FXML
	void grayscale() {
        controllers.forEach(ImageController::grayscale);
	}

	@FXML
	void threshold() {
        controllers.forEach(ImageController::threshold);
	}

	@FXML
    void exportToPng() {
        final File selectedDirectory = getDirectory(root.getScene().getWindow());
        final Task<? extends Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                controllers.forEach(controller -> controller.writeImage(selectedDirectory));
                return null;
            }
        };
        startTask(task);
    }

}