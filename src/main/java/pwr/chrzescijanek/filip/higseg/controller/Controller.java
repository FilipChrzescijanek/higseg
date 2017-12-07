package pwr.chrzescijanek.filip.higseg.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import com.google.common.base.Functions;

import pwr.chrzescijanek.filip.fuzzyclassifier.Classifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.DataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.Record;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestDataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.fuzzyclassifier.postprocessor.CustomDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.preprocessor.AttributeReductor;
import pwr.chrzescijanek.filip.fuzzyclassifier.preprocessor.ConflictResolver;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneFuzzifier;
import pwr.chrzescijanek.filip.higseg.util.ControllerUtils;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.Decision;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.getDirectory;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.getImageFiles;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.startTask;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {

	private final ObservableList<ImageController> controllers = FXCollections.observableArrayList();
	private final ObservableList<ImageController> markableControllers = FXCollections.observableArrayList();

	@FXML GridPane root;
	@FXML MenuBar menuBar;
	@FXML Menu fileMenu;
	@FXML MenuItem fileMenuExportToPng;
	@FXML MenuItem fileMenuExit;
	@FXML Menu alignMenu;
	@FXML MenuItem alignMenuLoadImages;
	@FXML MenuItem runMenuAlign;
	@FXML MenuItem runMenuCalculateResults;
	@FXML Menu optionsMenu;
	@FXML Menu optionsMenuStain;
	@FXML RadioMenuItem optionsMenuStainDab;
	@FXML ToggleGroup stainToggleGroup;
	@FXML RadioMenuItem optionsMenuStainH;
	@FXML Menu optionsMenuModel;
	@FXML RadioMenuItem optionsMenuModelTypeOne;
	@FXML ToggleGroup modelToggleGroup;
	@FXML RadioMenuItem optionsMenuModelTypeTwo;
	@FXML Menu optionsMenuTheme;
	@FXML RadioMenuItem optionsMenuThemeDark;
	@FXML ToggleGroup themeToggleGroup;
	@FXML RadioMenuItem optionsMenuThemeLight;
	@FXML Menu helpMenu;
	@FXML MenuItem helpMenuHelp;
	@FXML MenuItem helpMenuAbout;
	@FXML GridPane alignMainPane;
	@FXML Button createModelButton;
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
	void setDiaminobenzidine() {
		stainToggleGroup.selectToggle(optionsMenuStainDab);
	}
	
	@FXML
	void setHaematoxylin() {
		stainToggleGroup.selectToggle(optionsMenuStainH);
	}
	
	@FXML
	void setTypeOne() {
		modelToggleGroup.selectToggle(optionsMenuModelTypeOne);
	}
	
	@FXML
	void setTypeTwo() {
		modelToggleGroup.selectToggle(optionsMenuModelTypeTwo);
	}
	
	@FXML
	void exit() {
		root.getScene().getWindow().hide();
	}
	
	@FXML
	void createModel() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null) {
			final Task<? extends Void> task = createLoadImagesTask(selectedFiles, true);
			startTask(task);
		}
	}

	@FXML
	void loadImages() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null) {
			final Task<? extends Void> task = createLoadImagesTask(selectedFiles, false);
			startTask(task);
		}
	}
	
	private Task<? extends Void> createLoadImagesTask(final List<File> selectedFiles, final boolean markable) {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				loadImages(selectedFiles, markable);
				return null;
			}
		};
	}
	
	private void loadImages(final List<File> selectedFiles, final boolean markable) {
		for (final File f : selectedFiles) {
			final String filePath;
			try {
				filePath = f.getCanonicalPath();
				final Mat image = getImage(filePath);
				addNewImage(filePath, image, markable);
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
	
	private void addNewImage(final String filePath, final Mat image, final boolean markable) {
		final String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
		Platform.runLater(() -> {
            final Stage newStage = new Stage();
            final String viewPath = "/static/image.fxml";
            final ImageController controller = StageUtils.loadImageStage(newStage, viewPath, markable ? fileName + " (markable)" : fileName);
        	controller.setMarkable(markable);
            if (!markable) {
	            controllers.add(controller);
	            newStage.setOnHidden(e -> {
	                controllers.remove(controller);
	            });
            } else {
            	markableControllers.add(controller);
	            newStage.setOnHidden(e -> {
	            	markableControllers.remove(controller);
	            });
            }
            controller.setImage(image);
            newStage.show();
		});
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
		setDiaminobenzidine();
		setTypeTwo();
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
	
	private void setEnablementBindings() {
		final BooleanBinding noImages = Bindings.isEmpty(controllers);

		fileMenuExportToPng.disableProperty().bind(noImages);
		runMenuAlign.disableProperty().bind(noImages);
		runMenuCalculateResults.disableProperty().bind(noImages);
		grayscaleButton.disableProperty().bind(noImages);
		thresholdButton.disableProperty().bind(noImages);
	}
	
	@FXML
	void grayscale() {
		final Stage dialog = showPopup("Converting to grayscale...");
		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				grayscale(dialog);
				return null;
			}
		});
	}
	
	private void grayscale(final Stage dialog) {
        String clazz = "stain";
        List<String> clazzValues = Arrays.asList(Decision.YES.toString(), Decision.NO.toString());
        List<String> attributes  = Arrays.asList("Hue", "Saturation", "Value");
        
        Map<String, Double> sharpValues = new HashMap<>();
        sharpValues.put(Decision.YES.toString(),   0.0);
        sharpValues.put(Decision.NO.toString(),  255.0);
        
        if (!markableControllers.isEmpty()) {  
	        List<Record> records = Collections.unmodifiableList(
	        		markableControllers
	                .parallelStream()
	        		.flatMap(controller -> controller.getRecords(attributes).stream())
	        		.collect(Collectors.toList()));
	        
	        Classifier c = new TypeOneClassifier.Builder(new TypeOneFuzzifier(), new ConflictResolver(), new AttributeReductor())
	                .withDefuzzifier(new CustomDefuzzifier(sharpValues))
	        		.build()
	        		.train(new DataSet(clazz, clazzValues, attributes, records));
	        
	        Map<ImageController, Map<List<String>, Set<Coordinates>>> controllersInitialMappings = 
	        		controllers
	        		.parallelStream()
	        		.collect(Collectors.toMap(Function.identity(), controller -> controller.getInitialMapping()));
	        
	        Map<List<String>, TestRecord> mapping = getMapping(attributes, controllersInitialMappings);
	    	List<TestRecord> uniqueTestRecords    = new ArrayList<>(mapping.values());
			
	    	c.test(new TestDataSet(attributes, uniqueTestRecords));
	        
	        controllersInitialMappings.forEach((controller, initialMapping) -> {
	        	controller.grayscale(initialMapping.entrySet()
	        			.parallelStream()
	        			.collect(Collectors.toMap(e -> mapping.get(e.getKey()), e -> e.getValue())));
	        });
        }
        
		Platform.runLater(() -> dialog.close());
	}

	private Map<List<String>, TestRecord> getMapping(List<String> attributes, 
			Map<ImageController, Map<List<String>, Set<Coordinates>>> controllersInitialMappings) {
		Map<List<String>, TestRecord> mapping  = new HashMap<>();
		
		List<List<String>> uniqueValues = controllersInitialMappings.entrySet()
				.parallelStream()
				.flatMap(e -> e.getValue().keySet().stream())
				.collect(Collectors.toList());
		
		for (List<String> values : uniqueValues) {
			Map<String, Double> attributeValues = new HashMap<>();
			attributeValues.put(attributes.get(0), Double.parseDouble(values.get(0)));
			attributeValues.put(attributes.get(1), Double.parseDouble(values.get(1)));
			attributeValues.put(attributes.get(2), Double.parseDouble(values.get(2)));
			mapping.put(values, new TestRecord(attributeValues));
		}
		
		return mapping;
	}

	@FXML
	void threshold() {
		final Stage dialog = showPopup("Thresholding...");
		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				threshold(dialog);
				return null;
			}
		});
	}
	
	private void threshold(final Stage dialog) {
        controllers.forEach(ImageController::threshold);
        Platform.runLater(() -> dialog.close());
	}

	private Stage showPopup(final String info) {
		final Stage dialog = StageUtils.initDialog(root.getScene().getWindow());
		final HBox box = ControllerUtils.getHBoxWithLabelAndProgressIndicator(info);
		final Scene scene = new Scene(box);
		injectStylesheets(box);
		dialog.setScene(scene);
		dialog.show();
		return dialog;
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
