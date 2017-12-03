package pwr.chrzescijanek.filip.higseg.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Callback;
import pwr.chrzescijanek.filip.higseg.inject.Injector;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that handles FXML view loading process.
 */
public class FXView {

	private static final Logger LOGGER = Logger.getLogger(FXView.class.getName());

	private final ObjectProperty<Object> controllerProperty = new SimpleObjectProperty<>();

	private final URL resource;

	private FXMLLoader fxmlLoader;

	/**
	 * Constructs a FXView from the given path.
	 *
	 * @param path FXML view path
	 */
	public FXView(final String path) {
		resource = getClass().getResource(path);
	}

	/**
	 * @return FXML view controller
	 */
	public Object getController() {
		initializeFXMLLoader();
		return controllerProperty.get();
	}

	private void initializeFXMLLoader() {
		if (Objects.isNull(fxmlLoader)) {
			fxmlLoader = load(resource);
			controllerProperty.set(fxmlLoader.getController());
		}
	}

	private FXMLLoader load(final URL resource) throws IllegalStateException {
		final FXMLLoader loader = new FXMLLoader(resource);
		final Callback<Class<?>, Object> controllerFactory = Injector::instantiate;
		loader.setControllerFactory(controllerFactory);
		tryToLoad(loader);
		return loader;
	}

	private void tryToLoad(final FXMLLoader loader) {
		try {
			loader.load();
		} catch (final IOException e) {
			final IllegalStateException ex = new IllegalStateException(
					"Could not load view: " + loader.getLocation().getPath(), e);
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
			throw ex;
		}
	}

	/**
	 * @return FXML view root
	 */
	public Parent getView() {
		initializeFXMLLoader();
		return fxmlLoader.getRoot();
	}

}