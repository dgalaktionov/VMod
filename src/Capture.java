import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


class Capture implements Runnable {
	TargetDataLine	line;
	Thread			thread;


	public void start() {
		thread = new Thread(this);
		thread.setName("Capture");
		thread.start();
	}


	public void stop() {
		thread = null;
	}


	private void shutDown(String message) {
		if (thread != null) {
			thread = null;
			System.err.println(message);
		}
	}


	@Override
	public void run() {
		// define the required attributes for our line,
		// and make sure a compatible line is supported.

		AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			shutDown("Line matching " + info + " not supported.");
			return;
		}

		// get and open the target data line for capture.

		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format, line.getBufferSize());
		} catch (LineUnavailableException ex) {
			shutDown("Unable to open the line: " + ex);
			return;
		} catch (SecurityException ex) {
			shutDown(ex.toString());
			return;
		} catch (Exception ex) {
			shutDown(ex.toString());
			return;
		}

		// play back the captured audio data
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int frameSizeInBytes = format.getFrameSize();
		int bufferLengthInFrames = line.getBufferSize() / 8;
		int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
		byte[] data = new byte[bufferLengthInBytes];
		int numBytesRead;

		line.start();

		while (thread != null) {
			if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
				break;
			}
			out.write(data, 0, numBytesRead);

			System.out.println("Captured " + numBytesRead + " bytes.");
		}

		System.out.println("Stopped capturing.");

		// we reached the end of the stream. stop and close the line.
		line.stop();
		line.close();
		line = null;

		// stop and close the output stream
		try {
			out.flush();
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// load bytes into the audio input stream for playback
		byte audioBytes[] = out.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
		AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioBytes.length
				/ frameSizeInBytes);

		long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format
				.getFrameRate());

		try {
			audioInputStream.reset();
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		// TODO Actual playback coming soon :D
	}
}