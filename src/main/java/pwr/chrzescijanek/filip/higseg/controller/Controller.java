package pwr.chrzescijanek.filip.higseg.controller;

import static pwr.chrzescijanek.filip.higseg.util.Utils.getDirectory;
import static pwr.chrzescijanek.filip.higseg.util.Utils.getImageFiles;
import static pwr.chrzescijanek.filip.higseg.util.Utils.startTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.fuzzyclassifier.AbstractClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.Classifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.DataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.Record;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestDataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.fuzzyclassifier.model.AbstractModel;
import pwr.chrzescijanek.filip.fuzzyclassifier.postprocessor.Defuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.preprocessor.AttributeReductor;
import pwr.chrzescijanek.filip.fuzzyclassifier.preprocessor.ConflictResolver;
import pwr.chrzescijanek.filip.fuzzyclassifier.preprocessor.Fuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.CustomTypeOneDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.CustomTypeTwoDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.TypeTwoClassifier;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.Decision;
import pwr.chrzescijanek.filip.higseg.util.ModelDto;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {
	
	private static final String DEFAULT_INFO = "Default models loaded.";
	
	private final ObservableList<ImageController> controllers         = FXCollections.observableArrayList();
	private final ObservableList<ImageController> markableControllers = FXCollections.observableArrayList();
	
	private final ObjectProperty<Classifier> classifier = new SimpleObjectProperty<>();

	@FXML GridPane root;
	@FXML MenuBar menuBar;
	@FXML Menu fileMenu;
	@FXML MenuItem fileMenuExportToPng;
	@FXML MenuItem fileMenuExit;
	@FXML Menu alignMenu;
	@FXML MenuItem alignMenuLoadImages;
	@FXML MenuItem alignMenuCreateModel;
	@FXML MenuItem alignMenuSaveModel;
	@FXML MenuItem alignMenuLoadModel;
	@FXML MenuItem alignMenuUnloadModel;
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
	@FXML Button loadModelButton;
	@FXML Button saveModelButton;
	@FXML Button createModelButton;
	@FXML VBox alignLeftVBox;
	@FXML Button loadImagesButton;
	@FXML VBox alignCenterVBox;
	@FXML Button grayscaleButton;
	@FXML VBox alignRightVBox;
	@FXML Button thresholdButton;
	@FXML Label info;

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
	void saveModel() {
		final File file = Utils.saveModelFile(root.getScene().getWindow());
		if (file != null) {
			final Stage dialog = showPopup("Saving model...");
			startTask(new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					try {  
			            saveModel(file);
					} catch (IOException e) {
						handleException(e, "Saving failed!\nPlease check your write permissions.");
					}
					Platform.runLater(() -> dialog.close());
					return null;
				}
			});
		}
	}

	private void saveModel(final File file) throws IOException {
		List<String> attributes  = Arrays.asList("Hue", "Saturation", "Value");
		AbstractClassifier<?> classifier = (AbstractClassifier<?>) buildClassifier(attributes);
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			AbstractModel<?> model = (AbstractModel<?>) classifier.getModel();
			Defuzzifier<?> defuzzifier = classifier.getDefuzzifier();
			Map<String, Double> bottomValues = null;
			Map<String, Double> topValues = null;
			if (defuzzifier instanceof CustomTypeOneDefuzzifier) {
				bottomValues = ((CustomTypeOneDefuzzifier) defuzzifier).getSharpValues();
				topValues    = ((CustomTypeOneDefuzzifier) defuzzifier).getSharpValues();
			} else if (defuzzifier instanceof CustomTypeTwoDefuzzifier) {
				bottomValues = ((CustomTypeTwoDefuzzifier) defuzzifier).getBottomValues();
				topValues    = ((CustomTypeTwoDefuzzifier) defuzzifier).getTopValues();
			}
			
			bw.write(new Gson().toJson(new ModelDto(
					classifier instanceof TypeOneClassifier ? 1 : 2, 
					model.getClazzValues(), 
					model.getRules().toString(), 
					model.getStats().getMeans(), 
					model.getStats().getVariances(), bottomValues, topValues)));
		}
	}
	
	@FXML
	void unloadModel() {
		classifier.set(null);
		info.setText(DEFAULT_INFO);
	}
	
	@FXML
	void loadModel() {
		final File file = Utils.getModelFile(root.getScene().getWindow());
		if (file != null) {
			final Stage dialog = showPopup("Loading model...");
			startTask(new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					try {
						classifier.set(Utils.loadModel(file));
						Platform.runLater(() -> info.setText("Currently loaded model: " + file.getName()));
					} catch (IOException e) {
						handleException(e, "Loading failed!\nPlease check file path and your read permissions.");
					}
					Platform.runLater(() -> dialog.close());
					return null;
				}
			});
		}
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
            String name = getTitle(markable, fileName);
            final ImageController controller = StageUtils.loadImageStage(newStage, viewPath, markable ? name + " (markable)" : name);
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

	private String getTitle(final boolean markable, final String fileName) {
		long count = (markable ? markableControllers : controllers)
				.stream()
				.map(c -> ((Stage) c.root.getScene().getWindow()).getTitle())
				.filter(t -> t.startsWith(fileName))
				.count();
		String name = fileName;
		if (count > 0) {
			name = name + " (" + (count + 1) + ")";
		}
		return name;
	}
	
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		
		initializeComponents(location, resources);
		setBindings();
	}
	
	private void setBindings() {
		setEnablementBindings();
		info.visibleProperty().bind(Bindings.isEmpty(markableControllers));
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		initializeStyle();
		setDiaminobenzidine();
		setTypeTwo();
		info.setText(DEFAULT_INFO);
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
		final BooleanBinding noImages    = Bindings.isEmpty(controllers);
		final BooleanBinding creating = Bindings.isNotEmpty(markableControllers);
		final BooleanBinding modelLoaded = Bindings.isNotNull(classifier);
		final BooleanBinding cannotSave  = Bindings.not(creating);

		fileMenuExportToPng    .disableProperty().bind(noImages);
		runMenuAlign           .disableProperty().bind(noImages);
		runMenuCalculateResults.disableProperty().bind(noImages);
		grayscaleButton        .disableProperty().bind(noImages);
		thresholdButton        .disableProperty().bind(noImages);
		thresholdButton        .disableProperty().bind(noImages);
		loadModelButton        .disableProperty().bind(creating);
		alignMenuUnloadModel   .disableProperty().bind(Bindings.not(modelLoaded));
		alignMenuLoadModel     .disableProperty().bind(creating);
		saveModelButton        .disableProperty().bind(cannotSave);
		alignMenuSaveModel     .disableProperty().bind(cannotSave);
		optionsMenuStain       .disableProperty().bind(Bindings.or(creating, modelLoaded));
		optionsMenuModel       .disableProperty().bind(Bindings.and(Bindings.not(creating), modelLoaded));
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
        List<String> attributes  = Arrays.asList("Hue", "Saturation", "Value");
        Classifier c = chooseClassifier(attributes);
        
        if (c != null) {     
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

	private Classifier chooseClassifier(List<String> attributes) {
		Classifier c = null;
		
        if (!markableControllers.isEmpty()) {
	        c = buildClassifier(attributes);
        } else if (!Objects.isNull(classifier.get())) {
        	c = classifier.get();
        } else if (optionsMenuModelTypeTwo.isSelected() && optionsMenuStainDab.isSelected()) {	//@TODO
        	try {
				c = Utils.loadModel(getClass().getResource("/default-ii-dab.hgmodel").getFile());
			} catch (IOException e) {
				handleException(e, "Loading default type two DAB model failed!");
			}
        } else if (optionsMenuModelTypeTwo.isSelected() && optionsMenuStainH.isSelected()) {
        	try {
				c = Utils.loadModel(getClass().getResource("/default-ii-h.hgmodel").getFile());
			} catch (IOException e) {
				handleException(e, "Loading default type two H model failed!");
			}
        } else if (optionsMenuModelTypeOne.isSelected() && optionsMenuStainDab.isSelected()) {
        	try {
				c = Utils.loadModel(getClass().getResource("/default-i-dab.hgmodel").getFile());
			} catch (IOException e) {
				handleException(e, "Loading default type one DAB model failed!");
			}
        } else if (optionsMenuModelTypeOne.isSelected() && optionsMenuStainH.isSelected()) {
        	try {
				c = Utils.loadModel(getClass().getResource("/default-i-h.hgmodel").getFile());
			} catch (IOException e) {
				handleException(e, "Loading default type one H model failed!");
			}
        }
        
		return c;
	}

	private Classifier buildClassifier(List<String> attributes) {  
        String clazz = "stain";
        List<String> clazzValues = Arrays.asList(Decision.YES.toString(), Decision.NO.toString());
        
		List<Record> records = Collections.unmodifiableList(
				markableControllers
		        .parallelStream()
				.flatMap(controller -> controller.getRecords(attributes).stream())
				.collect(Collectors.toList()));
        
		if (optionsMenuModelTypeOne.isSelected())
	        return buildTypeOneClassifier(attributes, clazz, clazzValues, records);
	    else 
	        return buildTypeTwoClassifier(attributes, clazz, clazzValues, records);
	}

	private Classifier buildTypeOneClassifier(List<String> attributes, String clazz, List<String> clazzValues,
			List<Record> records) {
		Map<String, Double> sharpValues = new HashMap<>();
		sharpValues.put(Decision.YES.toString(),   0.0);
		sharpValues.put(Decision.NO.toString(),  255.0);
		
		Classifier c = new TypeOneClassifier.Builder(new Fuzzifier(), new ConflictResolver(), new AttributeReductor())
		        .withDefuzzifier(new CustomTypeOneDefuzzifier(sharpValues))
				.build()
				.train(new DataSet(clazz, clazzValues, attributes, records));
		
		return c;
	}

	private Classifier buildTypeTwoClassifier(List<String> attributes, String clazz, List<String> clazzValues,
			List<Record> records) {
		Map<String, Double> bottomSharpValues = new HashMap<>();
		bottomSharpValues.put(Decision.YES.toString(),   0.0);
		bottomSharpValues.put(Decision.NO.toString(),  240.0);
		Map<String, Double> topSharpValues = new HashMap<>();
		topSharpValues.put(Decision.YES.toString(),   15.0);
		topSharpValues.put(Decision.NO.toString(),   255.0);
		
		Classifier c = new TypeTwoClassifier.Builder(new Fuzzifier(), new ConflictResolver(), new AttributeReductor())
		        .withDefuzzifier(new CustomTypeTwoDefuzzifier(bottomSharpValues, topSharpValues))
				.build()
				.train(new DataSet(clazz, clazzValues, attributes, records));
		
		return c;
	}

	private Map<List<String>, TestRecord> getMapping(List<String> attributes, 
			Map<ImageController, Map<List<String>, Set<Coordinates>>> controllersInitialMappings) {
		Set<List<String>> uniqueValues = controllersInitialMappings.entrySet()
				.parallelStream()
				.flatMap(e -> e.getValue().keySet().stream())
				.collect(Collectors.toSet());
		
		return Utils.getMapping(attributes, uniqueValues);
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
		final HBox box = Utils.getHBoxWithLabelAndProgressIndicator(info);
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
