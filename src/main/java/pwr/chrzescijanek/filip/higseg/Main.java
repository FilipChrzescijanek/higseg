package pwr.chrzescijanek.filip.higseg;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.opencv.core.Core;

import com.beust.jcommander.JCommander;

import javafx.application.Application;

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
		Application.launch(MainApplication.class, args);
	}

}
