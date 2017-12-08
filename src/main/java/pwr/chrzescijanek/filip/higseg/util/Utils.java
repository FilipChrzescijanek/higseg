package pwr.chrzescijanek.filip.higseg.util;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import pwr.chrzescijanek.filip.fuzzyclassifier.Classifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.Stats;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.fuzzyclassifier.model.Rule;
import pwr.chrzescijanek.filip.fuzzyclassifier.model.SimpleClassifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.model.SimpleModel;
import pwr.chrzescijanek.filip.fuzzyclassifier.postprocessor.BasicDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.postprocessor.CustomDefuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.postprocessor.Defuzzifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.one.TypeOneRule;
import pwr.chrzescijanek.filip.fuzzyclassifier.type.two.TypeTwoRule;

/**
 * Provides utility methods for handling controllers.
 */
public final class Utils {

	private Utils() {}

	/**
	 * Writes image of sample to given directory.
	 *
	 * @param selectedDirectory directory
	 * @throws IOException if image could not be written
	 */
	public static void writeImage(final Mat image, final File selectedDirectory, final String title) throws IOException {
		imwrite(selectedDirectory.getCanonicalPath()
		        + File.separator + title, image);
	}

	public static Classifier loadModel(String filePath) throws IOException {
		return loadModel(new File(filePath));
	}

	public static Classifier loadModel(File file) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			List<String> lines = br.lines().collect(Collectors.toList());
			int type = Integer.parseInt(lines.get(0));
			List<String> classValues = getClassValues(lines.get(1));
			List<Rule> rules = getRules(lines.get(2), type);
			Map<String, Double> means = getMeans(lines.get(3));
			Map<String, Double> variances = getVariances(lines.get(4));
			Defuzzifier defuzzifier = lines.size() > 5 ? 
					new CustomDefuzzifier(getSharpValues(lines.get(5))) 
					: new BasicDefuzzifier(classValues);
			return new SimpleClassifier(new SimpleModel(classValues, rules, new Stats(means, variances)), defuzzifier);
		}
	}

	private static List<String> getClassValues(String string) {
		String[] inputs = string.replace("[", "").replaceAll("]", "").trim().split("\\s*,\\s*");
		return Arrays.asList(inputs);
	}

	private static List<Rule> getRules(String string, int type) {
		String[] inputs = string.replace("[", "").replaceAll("]", "").trim().split("\\s*,\\s*");
		List<Rule> rules = new ArrayList<>();
		for (String input : inputs) {
			String[] parts = input.split("\\s*=\\s*");
			String clazz = parts[0];
			String expression = parts[1];
			Expression<String> expr = RuleSet.simplify(ExprParser.parse(expression));
			rules.add(type == 1 ? new TypeOneRule(clazz, expr) : new TypeTwoRule(clazz, expr));
		}
		return rules;
	}

	private static Map<String, Double> getMeans(String string) {
		return parseMap(string);
	}

	private static Map<String, Double> getVariances(String string) {
		return parseMap(string);
	}
	
    private static Map<String, Double> getSharpValues(String string) {
		return parseMap(string);
	}
    
    private static Map<String, Double> parseMap(String string) {
    	Map<String, Double> map = new HashMap<>();
		String[] inputs = string.replace("{", "").replace("}", "").trim().split("\\s*,\\s*");
		for (String input : inputs) {
			String[] parts = input.split("\\s*=\\s*");
			String attribute = parts[0];
			Double value = Double.parseDouble(parts[1]);
			map.put(attribute, value);
		}
		return map;
    }

	public static Mat createMat(Mat image, Map<TestRecord, Set<Coordinates>> mapping) {
    	final byte[] data = new byte[(int) image.total()];
		final int width  = image.width();
    	mapping.forEach((k, v) -> {
			v.forEach(p -> {
				data[p.getY() * width + p.getX()] = k.getValue().byteValue();
			});
		}); 
		final Mat result = new Mat(image.size(), CvType.CV_8UC1);
		result.put(0, 0, data);
		return result;
	}
	
	public static Map<List<String>, TestRecord> getMapping(List<String> attributes, Set<List<String>> uniqueValues) {
		Map<List<String>, TestRecord> mapping  = new HashMap<>();
		
		for (List<String> values : uniqueValues) {
			Map<String, Double> attributeValues = new HashMap<>();
			attributeValues.put(attributes.get(0), Double.parseDouble(values.get(0)));
			attributeValues.put(attributes.get(1), Double.parseDouble(values.get(1)));
			attributeValues.put(attributes.get(2), Double.parseDouble(values.get(2)));
			mapping.put(values, new TestRecord(attributeValues));
		}
		
		return mapping;
	}
	
	public static Map<List<String>, Set<Coordinates>> getInitialMapping(Mat image) { 
		if (image.channels() == 3) {
			Mat rgb = image;
			Mat hsv = new Mat();
			Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_BGR2HSV_FULL);
			
			final int channels     = hsv.channels();
			final int width        = hsv.width();
			final int noOfBytes    = (int) hsv.total() * channels;
			final byte[] imageData = new byte[noOfBytes];
			
			hsv.get(0, 0, imageData);
			
			Map<List<String>, Set<Coordinates>> initialMapping = new HashMap<>();
			
			for (int i = 0; i < imageData.length; i += channels) {
				List<String> values = Arrays.asList(
						String.valueOf(Byte.toUnsignedInt(imageData[i + 0])),
						String.valueOf(Byte.toUnsignedInt(imageData[i + 1])),
						String.valueOf(Byte.toUnsignedInt(imageData[i + 2])));
				
				Set<Coordinates> coordinates = initialMapping.getOrDefault(values, new HashSet<>());
				coordinates.add(new Coordinates((i / channels) % width, (i / channels) / width));
				initialMapping.put(values, coordinates);
			}
			
			return initialMapping;
        }
        return Collections.emptyMap();
	}

	/**
	 * Shows file chooser dialog and gets CSV file.
	 *
	 * @param window application window
	 * @return CSV file
	 */
	public static File getCSVFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export results to CSV file");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Comma-separated values", "*.csv"));
		return fileChooser.showSaveDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static List<File> getImageFiles(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load images");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp", "*.tif"));
		return fileChooser.showOpenMultipleDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static File getModelFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load model");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Model Files", "*.hgmodel"));
		return fileChooser.showOpenDialog(window);
	}

	/**
	 * Shows file chooser dialog and gets image files.
	 *
	 * @param window application window
	 * @return image files
	 */
	public static File saveModelFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save model");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Model Files", "*.hgmodel"));
		return fileChooser.showSaveDialog(window);
	}

	/**
	 * Shows directory chooser dialog and gets directory.
	 *
	 * @param window application window
	 * @return directory
	 */
	public static File getDirectory(final Window window) {
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose directory");
		return chooser.showDialog(window);
	}

	/**
	 * @return help view
	 */
	public static WebView getHelpView() {
		final WebView view = new WebView();
		view.getEngine().load(Utils.class.getResource("/help.html").toExternalForm());
		return view;
	}

	/**
	 * @param info label
	 * @return customized, centered horizontal box with given label and progress indicator
	 */
	public static HBox getHBoxWithLabelAndProgressIndicator(final String info) {
		final Label label = new Label(info);
		label.setAlignment(Pos.CENTER);
		final HBox box = new HBox(label, new ProgressIndicator(-1.0));
		box.setSpacing(30.0);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(25));
		box.getStyleClass().add("modal-dialog");
		return box;
	}

	/**
	 * Starts given task
	 *
	 * @param task task to start
	 */
	public static void startTask(final Task<? extends Void> task) {
		final Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * @param color JavaFX color
	 * @return given color in web color format
	 */
	public static String getWebColor(final Color color) {
		return String.format("#%02X%02X%02X%02X",
		                     (int) (color.getRed() * 255),
		                     (int) (color.getGreen() * 255),
		                     (int) (color.getBlue() * 255),
		                     (int) (color.getOpacity() * 255));
	}

}
