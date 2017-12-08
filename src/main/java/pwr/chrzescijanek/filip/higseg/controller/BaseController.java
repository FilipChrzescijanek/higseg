package pwr.chrzescijanek.filip.higseg.controller;

import static pwr.chrzescijanek.filip.higseg.util.Utils.getHelpView;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.higseg.Main;
import pwr.chrzescijanek.filip.higseg.util.StageUtils;

/**
 * Base class for controllers.
 */
public class BaseController {

	private static final Logger LOGGER = Logger.getLogger(BaseController.class.getName());

	private static final String THEME_PREFERENCE_KEY = "higseg.theme";

	private static final String THEME_DARK = "/css/theme-dark.css";

	/**
	 * Controller theme.
	 */
	protected static final StringProperty theme = new SimpleStringProperty(THEME_DARK);

	private static final String THEME_LIGHT = "/css/theme-light.css";

	private static final Preferences preferences;

	static {
		preferences = Preferences.userNodeForPackage(Main.class);
		final String s = preferences.get(THEME_PREFERENCE_KEY, THEME_DARK);
		if (s.equals(THEME_LIGHT))
			theme.set(THEME_LIGHT);
		else
			theme.set(THEME_DARK);
	}

	/**
	 * Injects current theme stylesheets to given parent and adds on theme changed listener.
	 *
	 * @param parent parent to be styled
	 */
	protected void injectStylesheets(final Parent parent) {
		setTheme(parent);
		final ChangeListener<String> themeChangeListener = (observable, oldValue, newValue) -> {
			if (newValue != null && !newValue.isEmpty())
				setTheme(parent);
		};
		theme.addListener(themeChangeListener);
		setOnCloseRequest(parent, themeChangeListener);
	}

	private void setTheme(final Parent node) {
		node.getStylesheets().clear();
		node.getStylesheets().add(theme.get());
	}

	private void setOnCloseRequest(final Parent parent, final ChangeListener<String> themeChangeListener) {
		if (parent.getScene() != null && parent.getScene().getWindow() != null) {
			parent.getScene().getWindow().setOnCloseRequest(e -> {
				theme.removeListener(themeChangeListener);
				parent.getScene().getWindow().setOnCloseRequest(null);
			});
		}
	}

	/**
	 * Sets dark theme.
	 */
	protected void setDarkTheme() {
		theme.set(THEME_DARK);
		preferences.put(THEME_PREFERENCE_KEY, THEME_DARK);
	}

	/**
	 * Sets light theme.
	 */
	protected void setLightTheme() {
		theme.set(THEME_LIGHT);
		preferences.put(THEME_PREFERENCE_KEY, THEME_LIGHT);
	}

	/**
	 * @return true if light theme is selected; false otherwise
	 */
	protected boolean isLightThemeSelected() {
		return theme.get().equals(THEME_LIGHT);
	}

	/**
	 * @return true if dark theme is selected; false otherwise
	 */
	protected boolean isDarkThemeSelected() {
		return theme.get().equals(THEME_DARK);
	}

	protected void showAlert(final String content) {
		final Alert alert = StageUtils.getErrorAlert(content);
		final DialogPane dialogPane = alert.getDialogPane();
		injectStylesheets(dialogPane);
		alert.showAndWait();
	}

	protected void handleException(final Exception e, final String alert) {
		LOGGER.log(Level.SEVERE, e.toString(), e);
		Platform.runLater(() -> showAlert(alert));
	}

	@FXML
	protected void help() {
		final Stage stage = new Stage();
		final WebView helpView = getHelpView();
		StageUtils.prepareHelpStage(stage, helpView);
		injectStylesheets(helpView);
		stage.show();
	}

}
