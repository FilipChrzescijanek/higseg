package pwr.chrzescijanek.filip.higseg.controller;

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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
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
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.CustomTypeOneDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneFuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.CustomTypeTwoDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.TypeTwoClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.TypeTwoFuzzifier;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.Decision;
import pwr.chrzescijanek.filip.higseg.util.ModelDto;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Application controller class.
 */
public class Controller extends BaseController implements Initializable {
	
	private final ObservableList<ImageController> controllers         = FXCollections.observableArrayList();
	private final ObservableList<ImageController> markableControllers = FXCollections.observableArrayList();

	private final ObjectProperty<Classifier> classifier = new SimpleObjectProperty<Classifier>();

	@FXML GridPane root;
	@FXML MenuBar menuBar;
	@FXML Menu fileMenu;
	@FXML MenuItem fileMenuExit;
	@FXML Menu alignMenu;
	@FXML MenuItem alignMenuLoadImages;
	@FXML MenuItem alignMenuCreateModel;
	@FXML MenuItem alignMenuSaveModel;
	@FXML MenuItem runMenuAlign;
	@FXML Menu optionsMenu;
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
	@FXML Button processButton;
	@FXML Button saveButton;
	@FXML VBox alignRightVBox;

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
	void setTypeOne() {
		modelToggleGroup.selectToggle(optionsMenuModelTypeOne);
	}
	
	@FXML
	void setTypeTwo() {
		modelToggleGroup.selectToggle(optionsMenuModelTypeTwo);
	}
	
	@FXML
	void exit() {
		Platform.exit();
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
					Platform.runLater(() -> {
						dialog.close();
					});
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
	void createModel() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null && !selectedFiles.isEmpty()) {
			final Task<? extends Void> task = createLoadImagesTask(selectedFiles, true);
			startTask(task);
		}
	}

	@FXML
	void loadImages() {
		final List<File> selectedFiles = getImageFiles(root.getScene().getWindow());
		if (selectedFiles != null && !selectedFiles.isEmpty()) {
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
			String filePath = "";
			try {
				filePath = f.getCanonicalPath();
				final Mat image = getImage(filePath);
				addNewImage(filePath, image, markable);
			} catch (IOException | CvException e) {
				handleException(e,
				                "Loading failed!\nImage " + filePath + " might be corrupted, paths may contain non-ASCII symbols or "
				                + "you do not have sufficient read permissions.");
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
            	controller.setClassifier(classifier);
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
		markableControllers.addListener((ListChangeListener<ImageController>) change -> classifier.set(null));
	}
	
	private void setBindings() {
		setEnablementBindings();
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		initializeStyle();
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
		final BooleanBinding noImages    = Bindings.isEmpty(controllers);
		final BooleanBinding notCreating = Bindings.isEmpty(markableControllers);

		runMenuAlign           .disableProperty().bind(Bindings.or(noImages, notCreating));
		processButton          .disableProperty().bind(Bindings.or(noImages, notCreating));
		loadImagesButton       .disableProperty().bind(Bindings.not(noImages));
		alignMenuSaveModel     .disableProperty().bind(notCreating);
		saveButton     .disableProperty().bind(notCreating);
		optionsMenuModel       .disableProperty().bind(notCreating);
	}
	
	@FXML
	void process() {
		final Stage dialog = showPopup("Processing...");
		startTask(new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				process(dialog);
				return null;
			}
		});
	}
	
	private void process(final Stage dialog) {
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
	        	controller.process(initialMapping.entrySet()
	        			.parallelStream()
	        			.collect(Collectors.toMap(e -> mapping.get(e.getKey()), e -> e.getValue())));
	        });
        }
        
		Platform.runLater(() -> dialog.close());
	}

	private Classifier chooseClassifier(List<String> attributes) {
		Classifier c = buildClassifier(attributes);
        return c;
	}

	private Classifier buildClassifier(List<String> attributes) {
		if (classifier.isNotNull().get())
			return classifier.get();
        String clazz = "stain";
        List<String> clazzValues = Arrays.asList(Decision.YES.toString(), Decision.NO.toString());
        
		List<Record> records = Collections.unmodifiableList(
				markableControllers
		        .parallelStream()
				.flatMap(controller -> controller.getRecords(attributes).stream())
				.collect(Collectors.toList()));
        
		if (optionsMenuModelTypeOne.isSelected())
	        classifier.set(buildTypeOneClassifier(attributes, clazz, clazzValues, records));
	    else 
	    	classifier.set(buildTypeTwoClassifier(attributes, clazz, clazzValues, records));
		return classifier.get();
	}

	private Classifier buildTypeOneClassifier(List<String> attributes, String clazz, List<String> clazzValues,
			List<Record> records) {
		Map<String, Double> sharpValues = new HashMap<>();
		sharpValues.put(Decision.YES.toString(), 255.0);
		sharpValues.put(Decision.NO.toString(),    0.0);
		
		Classifier c = new TypeOneClassifier.Builder(new TypeOneFuzzifier(), new ConflictResolver(), new AttributeReductor())
		        .withDefuzzifier(new CustomTypeOneDefuzzifier(sharpValues))
				.build()
				.train(new DataSet(clazz, clazzValues, attributes, records));
		
		return c;
	}

	private Classifier buildTypeTwoClassifier(List<String> attributes, String clazz, List<String> clazzValues,
			List<Record> records) {
		Map<String, Double> bottomSharpValues = new HashMap<>();
		bottomSharpValues.put(Decision.YES.toString(), 240.0);
		bottomSharpValues.put(Decision.NO.toString(),    0.0);
		Map<String, Double> topSharpValues = new HashMap<>();
		topSharpValues.put(Decision.YES.toString(), 255.0);
		topSharpValues.put(Decision.NO.toString(),   15.0);
		
		Classifier c = new TypeTwoClassifier.Builder(new TypeTwoFuzzifier(), new ConflictResolver(), new AttributeReductor())
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

	private Stage showPopup(final String info) {
		final Stage dialog = StageUtils.initDialog(root.getScene().getWindow());
		final HBox box = Utils.getHBoxWithLabelAndProgressIndicator(info);
		final Scene scene = new Scene(box);
		injectStylesheets(box);
		dialog.setScene(scene);
		dialog.show();
		return dialog;
	}

}
