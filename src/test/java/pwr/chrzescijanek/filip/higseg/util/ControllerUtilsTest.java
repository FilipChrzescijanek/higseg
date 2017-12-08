package pwr.chrzescijanek.filip.higseg.util;

import javafx.scene.paint.Color;
import org.junit.Test;

import static javafx.scene.paint.Color.ALICEBLUE;
import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.GRAY;
import static javafx.scene.paint.Color.WHITE;
import static javafx.scene.paint.Color.color;
import static org.junit.Assert.assertEquals;

import static pwr.chrzescijanek.filip.higseg.util.Utils.getWebColor;

public class ControllerUtilsTest {

	@Test
	public void getWebColorTest() throws Exception {
		testColor(ALICEBLUE, "#F0F8FFFF");
		testColor(GRAY, "#808080FF");
		testColor(WHITE, "#FFFFFFFF");
		testColor(BLACK, "#000000FF");
		testColor(color(1.0, 1.0, 0.0, 0.0), "#FFFF0000");
		testColor(color(0.5, 0.5, 0.1, 0.5), "#7F7F197F");
	}

	private void testColor(final Color color, final String expected) {
		final String webColor = getWebColor(color);
		assertEquals(expected, webColor);
	}

}