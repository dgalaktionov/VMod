import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


class Capture implements Runnable {
	Thread		thread;
	final int	bufSize	= 16384;


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


	private AudioInputStream capture() {
		// define the required attributes for our line,
		// and make sure a compatible line is supported.

		AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line = null;

		if (!AudioSystem.isLineSupported(info)) {
			shutDown("Line matching " + info + " not supported.");
		}

		// get and open the target data line for capture.

		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format, line.getBufferSize());
		} catch (LineUnavailableException ex) {
			shutDown("Unable to open the line: " + ex);
		} catch (SecurityException ex) {
			shutDown(ex.toString());
		} catch (Exception ex) {
			shutDown(ex.toString());
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
		return new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);
	}


	private void playback(AudioInputStream audioInputStream) {
		SourceDataLine line = null;

		// make sure we have something to play
		if (audioInputStream == null) {
			shutDown("No loaded audio to play back");
			return;
		}

		// reset to the beginnning of the stream
		try {
			audioInputStream.reset();
		} catch (Exception e) {
			shutDown("Unable to reset the stream\n" + e);
			return;
		}

		// get an AudioInputStream of the desired format for playback
		AudioFormat format = audioInputStream.getFormat();
		AudioInputStream playbackInputStream = AudioSystem.getAudioInputStream(format,
				audioInputStream);

		if (playbackInputStream == null) {
			shutDown("Unable to convert stream of format " + audioInputStream + " to format "
					+ format);
			return;
		}

		// define the required attributes for our line,
		// and make sure a compatible line is supported.

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			shutDown("Line matching " + info + " not supported.");
			return;
		}

		// get and open the source data line for playback.

		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, bufSize);
		} catch (LineUnavailableException ex) {
			shutDown("Unable to open the line: " + ex);
			return;
		}

		// play back the captured audio data

		int frameSizeInBytes = format.getFrameSize();
		int bufferLengthInFrames = line.getBufferSize() / 8;
		int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
		byte[] data = new byte[bufferLengthInBytes];
		int numBytesRead = 0;

		System.out.println("Playing back...");
		// start the source data line
		line.start();

		try {
			while ((numBytesRead = playbackInputStream.read(data)) != -1) {
				int numBytesRemaining = numBytesRead;
				while (numBytesRemaining > 0) {
					numBytesRemaining -= line.write(data, 0, numBytesRemaining);
				}
			}
		} catch (Exception e) {
			shutDown("Error during playback: " + e);
		}

		// we reached the end of the stream. let the data play out, then
		// stop and close the line.
		line.drain();
		line.stop();
		line.close();

		System.out.println("Stopped playing back.");

		line = null;
		shutDown(null);
	}


	@Override
	public void run() {
		playback(capture());
	}
}