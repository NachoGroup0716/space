package com.adaptor.custom.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.adaptor.custom.utils.constants.CompressFormats;
import com.adaptor.custom.utils.constants.CompressOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecompressUtils {
	private static final int BUFFER_SIZE = 16 * 1024;
	private static final int SIGNATURE_READ_LIMIT = 10 * 1024;
	
	public static void decompress(Path source, Path target, Duration timeout, CompressFormats format, CompressOptions... options) throws IOException {
		boolean ignoreTreeStructure = false;
		if (source == null || Files.exists(source)) throw new IOException("Source path does not exists '" + source.toAbsolutePath() + "'");
		if (Files.isDirectory(source)) throw new IOException("Source path is directory, Compress file required '" + source.toAbsolutePath() + "'");
		if (target == null) target = source.getParent();
		if (options != null && options.length > 0) {
			for (CompressOptions option : options) {
				switch (option) {
					case IGNORE_TREE_STRUCTURES : ignoreTreeStructure = true; break;
					default : throw new IOException("Not supported option on decompress process '" + option + "'");
				}
			}
		}
	}
	
	private static CompressFormats detectFormat(Path source) {
		try (InputStream fis = Files.newInputStream(source);
				BufferedInputStream bis = new BufferedInputStream(fis, SIGNATURE_READ_LIMIT);) {
			bis.mark(SIGNATURE_READ_LIMIT);
			byte[] signature = new byte[SIGNATURE_READ_LIMIT];
			int readLength = IOUtils.readFully(bis, signature);
			bis.reset();
			
			if (readLength < 2) {
				return null;
			}
			
			if (GzipCompressorInputStream.matches(signature, readLength)) {
				try (GzipCompressorInputStream gis = new GzipCompressorInputStream(bis)) {
					byte[] header = new byte[512];
					int read = IOUtils.readFully(gis, header);
					if (read >= 512 && TarArchiveInputStream.matches(header, 512)) {
						return CompressFormats.TAR_GZIP;
					}
				} catch(IOException e) {
					log.warn("Failed while parse tar gzip format");
				}
				return CompressFormats.GZIP;
			}
			
			if (ZCompressorInputStream.matches(signature, readLength)) {
				return CompressFormats.LINUX_Z;
			}
			
			if (TarArchiveInputStream.matches(signature, readLength)) {
				return CompressFormats.TAR;
			}
			
			if (ZipArchiveInputStream.matches(signature, readLength)) {
				if (source.getFileName().toString().toLowerCase().endsWith(".jar")) {
					return CompressFormats.JAR;
				}
				return CompressFormats.ZIP;
					
			}
		} catch(IOException e) {
			log.warn("Fail to detect file compress format by header bytes");
		}
		
		String fullName = source.getFileName().toString();
		String lower = fullName.toLowerCase();
		if (lower.endsWith(".tar.gz")) {
			return CompressFormats.TAR_GZIP;
		} else if (lower.endsWith(".tar")) {
			return CompressFormats.TAR;
		} else if (lower.endsWith(".zip")) {
			return CompressFormats.ZIP;
		} else if (lower.endsWith(".jar")) { 
			return CompressFormats.JAR;
		} else if (lower.endsWith(".gz")) {
			return CompressFormats.GZIP;
		} else if (lower.endsWith(".z")) {
			return CompressFormats.LINUX_Z;
		} else {
			return null;
		}
	}
}
