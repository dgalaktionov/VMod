import java.io.IOException;


/**
 * 
 * @author DaGal
 * @version 1.0
 * @since 30/09/2014
 */

public class Main {
	public static void main(String args[]) {
		Capture c = new Capture();
		c.start();

		// Stop capturing when input is given
		try {
			while (System.in.available() == 0) {
			}

			System.in.skip(System.in.available());
			c.stop();
		} catch (IOException e) {
			System.err.println("IO Error: " + e.toString());
		}
	}
}
