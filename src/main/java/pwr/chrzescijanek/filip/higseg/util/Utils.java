package pwr.chrzescijanek.filip.higseg.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;

/**
 * Provides utility methods for handling controllers.
 */
public final class Utils {

	private Utils() {}

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

	public static File saveModelFile(final Window window) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save model");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Model Files", "*.hgmodel"));
		return fileChooser.showSaveDialog(window);
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
