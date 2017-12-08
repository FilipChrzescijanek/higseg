package pwr.chrzescijanek.filip.higseg;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import javafx.application.Application;
import javafx.application.Platform;
import pwr.chrzescijanek.filip.fuzzyclassifier.Classifier;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestDataSet;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.Utils;

/**
 * Main application class.
 */
public class Main {

	private static final String LOGGING_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";

	private static final String LOGGING_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s: %5$s%6$s%n";

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.setProperty(LOGGING_FORMAT_PROPERTY, LOGGING_FORMAT);
		initializeLogger();
	}
    
	@Parameter(names={"--headless", "-h"},  description = "Headless mode")
    private boolean headless = false;
    
	@Parameter(names={"--input", "-i"},     description = "Input image"  )
    private String  inputPath  = "";
	
	@Parameter(names={"--output", "-o"},    description = "Output image" )
    private String  outputPath = "";
    
	@Parameter(names={"--model", "-m"},     description = "Model file"   )
    private String  modelPath  = "";

	private Main() { }

	private static void initializeLogger() {
		try {
			final Handler fileHandler = new FileHandler("log", 10000, 5, true);
			fileHandler.setFormatter(new SimpleFormatter());
			Logger.getLogger(Main.class.getPackage().getName()).addHandler(fileHandler);
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
	}

	/**
	 * Starts the application.
	 *
	 * @param args launch arguments
	 * @throws IOException 
	 */
	public static void main(final String... args) throws IOException {
		Main main = new Main();
		JCommander.newBuilder().addObject(main).build().parse(args);
        main.run(args);
	}

	private void run(final String... args) throws IOException {
		if (headless) {
            headless();
		} else {
			Application.launch(MainApplication.class, args);
		}
	}

	private void headless() throws IOException {
		List<String> attributes = Arrays.asList("Hue", "Saturation", "Value");
		
		Classifier classifier = tryLoadingClassifier();
		
		Mat image = tryLoadingImage();
		Map<List<String>, Set<Coordinates>> initialMapping = Utils.getInitialMapping(image);
		Map<List<String>, TestRecord>       resultMapping  = Utils.getMapping(attributes, initialMapping.keySet());
		
		List<TestRecord> uniqueTestRecords = new ArrayList<>(resultMapping.values());
		
		classifier.test(new TestDataSet(attributes, uniqueTestRecords));
		
		Map<TestRecord, Set<Coordinates>> mapping = initialMapping.entrySet()
				.parallelStream()
				.collect(Collectors.toMap(e -> resultMapping.get(e.getKey()), e -> e.getValue()));
		
		Mat result = Utils.createMat(image, mapping);
		tryWritingImage(result);
		
		Platform.exit();
	}

	private Classifier tryLoadingClassifier() throws IOException {
		Classifier c;
		try {
			c = Utils.loadModel(new File(modelPath));
		} catch (IOException e) {
			throw new IOException("Could not read model file. Path: " + modelPath, e);
		}
		return c;
	}

	private Mat tryLoadingImage() throws IOException {
		Mat image;
		try {
			image = Imgcodecs.imread(new File(inputPath).getCanonicalPath(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
		} catch (IOException e) {
			throw new IOException("Could not read input image file. Path: " + inputPath, e);
		}
		if (image.dataAddr() == 0)
			throw new CvException("Failed to load image! Check if file path contains only ASCII symbols");
		return image;
	}

	private void tryWritingImage(Mat result) throws IOException {
		try {
			imwrite(new File(outputPath).getCanonicalPath(), result);
		} catch (IOException e) {
			throw new IOException("Could not write output image file. Path: " + outputPath, e);
		}
	}

}
