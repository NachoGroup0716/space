package com.adaptor.custom.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommandUtils {
	private final static Charset CHAR_SET = Charset.defaultCharset();
	
	public static void run(List<String> command, Path output, Path work, Duration timeout) throws InterruptedException, TimeoutException, IOException {
		ProcessBuilder pb = new ProcessBuilder(command);
		if (output != null) {
			pb.redirectOutput(output.toFile());
		}
		if (work != null) {
			pb.directory(work.toFile());
		}
		pb.redirectError(ProcessBuilder.Redirect.PIPE);
		
		Process proc = pb.start();
		StringBuilder errSb = new StringBuilder();
		ExecutorService es = Executors.newSingleThreadExecutor();
		Future<?> errF = es.submit(streamGobbler(proc.getErrorStream(), errSb));
		boolean finished = proc.waitFor(timeout == null ? 0 : timeout.toMillis(), TimeUnit.MILLISECONDS);
		try {
			errF.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {	}
		es.shutdown();
		if (!finished) {
			proc.destroyForcibly();
			throw new TimeoutException("Timed out: " + String.join(" ", command));
		}
		if (proc.exitValue() != 0) {
			throw new IOException("Command execute failed: " + String.join(" ", command) + System.lineSeparator() + "STDERR:" + System.lineSeparator() + errSb.toString().trim());
		}
	}
	
	private static Runnable streamGobbler(InputStream is, StringBuilder sb) {
		return () -> {
			try (InputStreamReader isr = new InputStreamReader(is, CHAR_SET);
					BufferedReader br = new BufferedReader(isr);) {
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line).append(System.lineSeparator());
				}
			} catch(IOException ignore) {	}
		};
	}
}
