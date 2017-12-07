package pwr.chrzescijanek.filip.higseg.controller;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static pwr.chrzescijanek.filip.higseg.util.ControllerUtils.startTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.raw.Record;
import pwr.chrzescijanek.filip.fuzzyclassifier.data.test.TestRecord;
import pwr.chrzescijanek.filip.higseg.util.ControllerUtils;
import pwr.chrzescijanek.filip.higseg.util.Coordinates;
import pwr.chrzescijanek.filip.higseg.util.Decision;

/**
 * Application controller class.
 */
public class ImageController extends BaseController implements Initializable {
	
	private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    private final ObjectProperty<Mat> image = new SimpleObjectProperty<>();
    
    private final BooleanProperty markable = new SimpleBooleanProperty(false);

	@FXML GridPane root;
    @FXML MenuBar menuBar;
    @FXML Menu fileMenu;
    @FXML MenuItem fileMenuExit;
    @FXML Menu editMenu;
    @FXML MenuItem editMenuZoomIn;
    @FXML MenuItem editMenuZoomOut;
	@FXML Menu optionsMenu;
	@FXML Menu optionsMenuMode;
	@FXML RadioMenuItem optionsMenuModeMark;
	@FXML ToggleGroup modeToggleGroup;
	@FXML RadioMenuItem optionsMenuModeErase;
	@FXML RadioButton modeMark;
	@FXML ToggleGroup modeRadioToggleGroup;
	@FXML RadioButton modeErase;
	@FXML HBox modeBox;
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
	void setMarkMode() {
		modeToggleGroup.selectToggle(optionsMenuModeMark);
		modeRadioToggleGroup.selectToggle(modeMark);
	}
	
	@FXML
	void setEraseMode() {
		modeToggleGroup.selectToggle(optionsMenuModeErase);
		modeRadioToggleGroup.selectToggle(modeErase);
	}
	
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
        ObjectBinding<Image> binding = Bindings.createObjectBinding(() -> image.isNull().get() ? null : createImage(image.get()), image);
		alignImageView.imageProperty().bind(binding);
	}
	
	private void initializeComponents(final URL location, final ResourceBundle resources) {
		bindScrollPaneSize();
		initializeStyle();
		initializeComboBoxes();
		setMarkMode();
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
		alignScaleCombo.setValue("100%");
	}
	
	private void initializeStyle() {
		injectStylesheets(root);
		canvas.setOpacity(0.5);
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
		canvas.setOnMouseMoved(event -> mousePositionLabel.setText(
				(((int) event.getX()) + 1) + " : " + (((int) event.getY()) + 1)));
		canvas.setOnMouseExited(event -> mousePositionLabel.setText("- : -"));
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
		alignInfo.visibleProperty().bind(markable);
		alignImageViewGroup.visibleProperty().bind(alignImageIsPresent);
		alignBottomGrid.visibleProperty().bind(alignImageIsPresent);
		optionsMenu.visibleProperty().bind(markable);
		modeBox.visibleProperty().bind(markable);
	}

	void setMarkable(boolean markable) {
		this.markable.set(markable);
	}
	
	void setImage(Mat img) {
        image.set(img);
        alignImageSizeLabel.setText(img.width() + "x" + img.height() + " px");
        bindCanvas();
	}

	private void bindCanvas() {
		canvas.widthProperty().bind(alignImageView.imageProperty().get().widthProperty());
		canvas.heightProperty().bind(alignImageView.imageProperty().get().heightProperty());
		canvas.scaleXProperty().bind(alignImageView.scaleXProperty());
		canvas.scaleYProperty().bind(alignImageView.scaleYProperty());
		canvas.translateXProperty().bind(alignImageView.translateXProperty());
		canvas.translateYProperty().bind(alignImageView.translateYProperty());
		if (markable.get()) {
			canvas.getGraphicsContext2D().setFill(Color.BLACK);
			canvas.getGraphicsContext2D().fillOval(0, 5, 36, 30);
		}
	}

    private Image createImage(final Mat image) {
        final MatOfByte byteMat = new MatOfByte();
        imencode(".png", image, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }

	Map<List<String>, Set<Coordinates>> getInitialMapping() {
        if (image.get().channels() == 3) {
			Mat rgb = image.get();
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

	void grayscale(Map<TestRecord, Set<Coordinates>> mapping) {
        if (image.get().channels() == 3) {
			Mat result = createMat(mapping);
	        Platform.runLater(() -> image.set(result));
        }
    }

    private Mat createMat(Map<TestRecord, Set<Coordinates>> mapping) {
    	final byte[] data = new byte[(int) image.get().total()];
		final int width  = image.get().width();
    	mapping.forEach((k, v) -> {
			v.forEach(p -> {
				data[p.getY() * width + p.getX()] = k.getValue().byteValue();
			});
		}); 
		final Mat result = new Mat(image.get().size(), CvType.CV_8UC1);
		result.put(0, 0, data);
		return result;
	}

	void threshold() {
        if (image.get().channels() == 1) {
	        Mat result = new Mat();
	        Imgproc.threshold(image.get(), result, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
	        Platform.runLater(() -> image.set(result));
        }
    }

    void writeImage(File selectedDirectory) {
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
    
    List<Record> getRecords(List<String> attributes) {
    	List<Record> records = new ArrayList<>();
    	Mat rgb = image.get();
		Mat hsv = new Mat();
		Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_BGR2HSV_FULL);
		
		final int channels     = hsv.channels();
		final int width        = hsv.width();
		final int noOfBytes    = (int) hsv.total() * channels;
		final byte[] imageData = new byte[noOfBytes];
		hsv.get(0, 0, imageData);
		
		PixelReader pr = getCanvasPixelReader();
		
		for (int i = 0; i < imageData.length; i += channels) {
			Map<String, Double> attributeValues = new HashMap<>();
			attributeValues.put(attributes.get(0), (double) Byte.toUnsignedInt(imageData[i + 0]));
			attributeValues.put(attributes.get(1), (double) Byte.toUnsignedInt(imageData[i + 1]));
			attributeValues.put(attributes.get(2), (double) Byte.toUnsignedInt(imageData[i + 2]));
			records.add(new Record(isCovered(pr, (i / channels) % width, (i / channels) / width), attributeValues));
		}

    	return records;
    }

	private PixelReader getCanvasPixelReader() {
		final FutureTask<WritableImage> query = getCanvasQuery();
		Platform.runLater(query);
		
		PixelReader pr = null;
		try {
			pr = query.get().getPixelReader();
		} catch (Exception e) {
            handleException(e, "Reading marked data failed!");
		}
		
		return pr;
	}

	private FutureTask<WritableImage> getCanvasQuery() {
		final FutureTask<WritableImage> query = new FutureTask<>(() -> {
			double currentScale = canvas.getScaleX();			
			SnapshotParameters snapshotParameters = new SnapshotParameters();
			snapshotParameters.setTransform(Transform.scale(1.0 / currentScale, 1.0 / currentScale));
			WritableImage img = canvas.snapshot(snapshotParameters, null);
			return img;
		});
		return query;
	}

	private String isCovered(PixelReader pr, int x, int y) {
		return pr.getColor(x, y).equals(Color.WHITE) ? Decision.NO.toString() : Decision.YES.toString();
	}
    
}

