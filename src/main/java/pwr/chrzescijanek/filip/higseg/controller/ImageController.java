package pwr.chrzescijanek.filip.higseg.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;
import pwr.chrzescijanek.filip.higseg.util.ControllerUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.getDirectory;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.startTask;

/**
 * Application controller class.
 */
public class ImageController extends BaseController implements Initializable {
	
	private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    private final ObjectProperty<Mat> image = new SimpleObjectProperty<>();

	@FXML GridPane root;
    @FXML MenuBar menuBar;
    @FXML Menu fileMenu;
    @FXML MenuItem fileMenuExit;
    @FXML Menu editMenu;
    @FXML MenuItem editMenuZoomIn;
    @FXML MenuItem editMenuZoomOut;
    @FXML Menu helpMenu;
    @FXML MenuItem helpMenuHelp;
    @FXML BorderPane borderPane;
	@FXML HBox alignTopHBox;
	@FXML Label alignInfo;
	@FXML ScrollPane alignScrollPane;
	@FXML Group alignImageViewGroup;
	@FXML AnchorPane alignImageViewAnchor;
	@FXML ImageView alignImageView;
	@FXML Canvas canvas;
	@FXML GridPane alignBottomGrid;
	@FXML Label alignImageSizeLabel;
	@FXML ComboBox<String> alignScaleCombo;
	@FXML Label alignMousePositionLabel;

    @FXML
    void exit() {
        root.getScene().getWindow().hide();
    }
	
	@FXML
	void zoomIn() {
		updateScrollbars(alignImageView, alignScrollPane, 1);
	}
	
	private void updateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane, final double deltaY) {
		final double oldScale = imageView.getScaleX();
		final double hValue = imageScrollPane.getHvalue();
		final double vValue = imageScrollPane.getVvalue();
		if (deltaY > 0) {
			imageView.setScaleX(imageView.getScaleX() * 1.05);
		}
		else {
			imageView.setScaleX(imageView.getScaleX() / 1.05);
		}
		final double scale = imageView.getScaleX();
		validateScrollbars(imageView, imageScrollPane, scale, oldScale, hValue, vValue);
	}
	
	private void validateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane, final double scale,
	                                final double oldScale, final double hValue, final double vValue) {
		validateHorizontalScrollbar(imageView, imageScrollPane, scale, oldScale, hValue);
		validateVerticalScrollbar(imageView, imageScrollPane, scale, oldScale, vValue);
	}
	
	private void validateHorizontalScrollbar(final ImageView imageView, final ScrollPane imageScrollPane,
	                                         final double scale, final double oldScale, final double hValue) {
		if ((scale * imageView.getImage().getWidth() > imageScrollPane.getWidth())) {
			final double oldHDenominator = calculateDenominator(oldScale, imageView.getImage().getWidth(),
			                                                    imageScrollPane.getWidth());
			final double newHDenominator = calculateDenominator(scale, imageView.getImage().getWidth(),
			                                                    imageScrollPane.getWidth());
			imageScrollPane.setHvalue(calculateValue(scale, oldScale, hValue, oldHDenominator, newHDenominator));
		}
	}
	
	private void validateVerticalScrollbar(final ImageView imageView, final ScrollPane imageScrollPane, final double
			scale, final double
			                                       oldScale, final double vValue) {
		if ((scale * imageView.getImage().getHeight() > imageScrollPane.getHeight())) {
			final double oldVDenominator = calculateDenominator(oldScale, imageView.getImage().getHeight(),
			                                                    imageScrollPane.getHeight());
			final double newVDenominator = calculateDenominator(scale, imageView.getImage().getHeight(),
			                                                    imageScrollPane.getHeight());
			imageScrollPane.setVvalue(calculateValue(scale, oldScale, vValue, oldVDenominator, newVDenominator));
		}
	}
	
	private double calculateDenominator(final double scale, final double imageSize, final double paneSize) {
		return (scale * imageSize - paneSize) * 2 / paneSize;
	}
	
	private double calculateValue(final double scale, final double oldScale, final double value, final double
			oldDenominator, final double newDenominator) {
		return ((scale - 1) + (value * oldDenominator - (oldScale - 1)) / oldScale * scale) / newDenominator;
	}
	
	@FXML
	void zoomOut() {
		updateScrollbars(alignImageView, alignScrollPane, -1);
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		//FXML fields load assertions
		assert alignImageViewAnchor != null
				: "fx:id=\"alignImageViewAnchor\" was not injected: check your FXML file 'image.fxml'.";
		assert alignScaleCombo != null
				: "fx:id=\"alignScaleCombo\" was not injected: check your FXML file 'image.fxml'.";
		assert root != null
				: "fx:id=\"root\" was not injected: check your FXML file 'image.fxml'.";
		assert alignBottomGrid != null
				: "fx:id=\"alignBottomGrid\" was not injected: check your FXML file 'image.fxml'.";
		assert alignImageViewGroup != null
				: "fx:id=\"alignImageViewGroup\" was not injected: check your FXML file 'image.fxml'.";
		assert alignTopHBox != null
				: "fx:id=\"alignTopHBox\" was not injected: check your FXML file 'image.fxml'.";
		assert alignImageView != null
				: "fx:id=\"alignImageView\" was not injected: check your FXML file 'image.fxml'.";
		assert alignImageSizeLabel != null
				: "fx:id=\"alignImageSizeLabel\" was not injected: check your FXML file 'image.fxml'.";
		assert alignInfo != null
				: "fx:id=\"alignInfo\" was not injected: check your FXML file 'image.fxml'.";
		assert alignMousePositionLabel != null
				: "fx:id=\"alignMousePositionLabel\" was not injected: check your FXML file 'image.fxml'.";
		assert alignScrollPane != null
				: "fx:id=\"alignScrollPane\" was not injected: check your FXML file 'image.fxml'.";
		
		initializeComponents(location, resources);
		setBindings();
		addListeners();
	}
	
	private void addListeners() {
		addOnMouseReleasedListeners();
		addOnMouseClickedListeners();
		setImageViewControls(alignImageView, alignScrollPane, alignImageViewGroup, alignScaleCombo,
		                     alignMousePositionLabel);
	}
	
	private void setBindings() {
		setVisibilityBindings();
        ObjectBinding<Image> binding = Bindings.createObjectBinding(() -> createImage(image.get()), image);
		alignImageView.imageProperty().bind(binding);
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		bindScrollPaneSize();
		initializeStyle();
		initializeComboBoxes();
	}

	private void bindScrollPaneSize() {
		alignScrollPane.prefHeightProperty().bind(root.heightProperty());
		alignScrollPane.prefWidthProperty().bind(root.widthProperty());
	}

	private void addOnMouseClickedListeners() {
		setOnAlignImageMouseClicked();
	}
	
	private void setOnAlignImageMouseClicked() {
		alignImageViewGroup.setOnMouseClicked(event -> {
		});
	}

	private void addOnMouseReleasedListeners() {
		root.setOnMouseReleased(event -> {
			alignImageViewGroup.getScene().setCursor(Cursor.DEFAULT);
		});
	}
	
	private void initializeComboBoxes() {
		initializeScaleComboBoxes();
	}
	
	private void initializeScaleComboBoxes() {
		alignScaleCombo.itemsProperty().get().addAll(
			"25%", "50%", "75%", "100%", "125%", "150%", "175%", "200%", "250%", "500%", "1000%"
		);
	}
	
	private void initializeStyle() {
		injectStylesheets(root);
	}
	
	private void setImageViewControls(final ImageView imageView, final ScrollPane imageScrollPane,
	                                  final Group imageViewGroup, final ComboBox<String> scaleCombo,
	                                  final Label mousePositionLabel) {
		setImageViewGroupListeners(imageView, imageScrollPane, imageViewGroup, mousePositionLabel);
		setImageScrollPaneEventFilter(imageView, imageScrollPane);
		setImageViewScaleListener(imageView, imageScrollPane, scaleCombo);
		setComboBoxListener(imageView, scaleCombo);
	}
	
	private void setImageViewGroupListeners(final ImageView imageView, final ScrollPane imageScrollPane,
	                                        final Group imageViewGroup, final Label mousePositionLabel) {
		imageViewGroup.setOnMouseMoved(event -> mousePositionLabel.setText(
				(int) (event.getX() / imageView.getScaleX()) + " : " + (int) (event.getY() / imageView.getScaleY())));
		imageViewGroup.setOnMouseExited(event -> mousePositionLabel.setText("- : -"));
		imageViewGroup.setOnScroll(event -> {
			if (event.isControlDown() && imageView.getImage() != null) {
				final double deltaY = event.getDeltaY();
				updateScrollbars(imageView, imageScrollPane, deltaY);
			}
		});
	}
	
	private void setImageScrollPaneEventFilter(final ImageView imageView, final ScrollPane imageScrollPane) {
		imageScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
			if (event.isControlDown() && imageView.getImage() != null) {
				final double deltaY = event.getDeltaY();
				updateScrollbars(imageView, imageScrollPane, deltaY);
				event.consume();
			}
		});
	}
	
	private void setImageViewScaleListener(final ImageView imageView, final ScrollPane imageScrollPane,
	                                       final ComboBox<String> scaleCombo) {
		imageView.scaleXProperty().addListener((observable, oldValue, newValue) -> {
			final double oldScale = oldValue.doubleValue();
			final double hValue = imageScrollPane.getHvalue();
			final double vValue = imageScrollPane.getVvalue();
			final double scale = newValue.doubleValue();
			imageView.setScaleY(scale);
			setImageViewTranslates(imageView);
			updateScrollbars(imageView, imageScrollPane, oldScale, hValue, vValue, scale);
			updateComboBox(scaleCombo, newValue);
		});
	}

	private void setImageViewTranslates(final ImageView view) {
		view.setTranslateX(view.getImage().getWidth() * 0.5 * (view.getScaleX() - 1.0));
		view.setTranslateY(view.getImage().getHeight() * 0.5 * (view.getScaleY() - 1.0));
	}
	
	private void updateScrollbars(final ImageView imageView, final ScrollPane imageScrollPane,
	                              final double oldScale, final double hValue, final double vValue, final double
			                              scale) {
		if (Math.round(oldScale * 100) != Math.round(scale * 100)) {
			validateScrollbars(imageView, imageScrollPane, scale, oldScale, hValue, vValue);
		}
	}
	
	private void updateComboBox(final ComboBox<String> scaleCombo, final Number newValue) {
		final String asString = String.format("%.0f%%", newValue.doubleValue() * 100);
		if (!scaleCombo.getValue().equals(asString))
			scaleCombo.setValue(asString);
	}
	
	private void setComboBoxListener(final ImageView imageView, final ComboBox<String> scaleCombo) {
		scaleCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("[1-9]\\d*%"))
				scaleCombo.setValue(oldValue);
			else
				imageView.setScaleX(Double.parseDouble(newValue.substring(0, newValue.length() - 1)) / 100.0);
		});
	}
	
	private void setVisibilityBindings() {
		onAlignImageIsPresent();
	}
	
	private void onAlignImageIsPresent() {
		final BooleanBinding alignImageIsPresent = alignImageView.imageProperty().isNotNull();
		alignInfo.visibleProperty().bind(alignImageIsPresent);
		alignImageViewGroup.visibleProperty().bind(alignImageIsPresent);
		alignBottomGrid.visibleProperty().bind(alignImageIsPresent);
	}

	public void setImage(Mat img) {
        image.set(img);
        alignImageSizeLabel.setText(img.cols() + "x" + img.rows() + " px");
	}

    private Image createImage(final Mat image) {
        final MatOfByte byteMat = new MatOfByte();
        imencode(".png", image, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }

	void grayscale() {
        Mat result = new Mat();
        Imgproc.cvtColor(image.get(), result, Imgproc.COLOR_BGRA2GRAY);
        image.set(result);
    }

    void threshold() {
        Mat result = new Mat();
        Imgproc.threshold(image.get(), result, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        image.set(result);
    }

    void writeImage() {
        final File selectedDirectory = getDirectory(root.getScene().getWindow());
        if (selectedDirectory != null) {
            if (selectedDirectory.canWrite()) {
                final Task<Void> task = createWriteImagesTask(selectedDirectory);
                startTask(task);
            }
            else
                Platform.runLater(() -> showAlert("Save failed! Check your write permissions."));
        }
    }

    private Task<Void> createWriteImagesTask(final File selectedDirectory) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String title = ((Stage) root.getScene().getWindow()).getTitle();
                    ControllerUtils.writeImage(image.get(), selectedDirectory, title);
                } catch (final IOException e) {
                    handleException(e, "Save failed! Check your write permissions.");
                }
                return null;
            }
        };
    }
}

