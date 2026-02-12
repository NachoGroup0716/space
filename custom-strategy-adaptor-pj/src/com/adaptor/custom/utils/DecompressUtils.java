package com.adaptor.custom.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
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

	public static List<String> preview(Path source, CompressFormats format) throws IOException {
		validateSource(source);
		
		if (format == null) {
			format = detectFormat(source);
			log.debug("Detected compress file format: {}", format);
			if (format == null) {
				throw new IOException("Unsupported or unknown compress format: " + source.getFileName());
			}
		}
		
		log.info("Prepare for preview :: [FILE:{}] [FORMAT:{}]", source.toAbsolutePath(), format);
		List<String> entryList = new ArrayList<String>();
		
		switch (format) {
		case TAR :
		case TAR_GZIP :
		case ZIP :
		case JAR :
			try (InputStream fis = Files.newInputStream(source);
					BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
					ArchiveInputStream ais = createArchiveInputStream(format, bis);) {
				ArchiveEntry entry;
				while ((entry = ais.getNextEntry()) != null) {
					if (!ais.canReadEntryData(entry)) continue;
					if (!entry.isDirectory()) entryList.add(entry.getName());
				}
			}
			break;
		case LINUX_Z :
		case GZIP :
			String expectedName = getUncompressedFileName(source.getFileName().toString(), format);
			entryList.add(expectedName);
			break;
		default :
			throw new IOException("Unsupported format for preview logic: " + format);
		}
		
		return entryList;
	}
	
	public static List<Path> decompress(Path source, Path target, Duration timeout, CompressFormats format, CompressOptions... options) throws IOException {
		boolean ignoreTreeStructure = false;
		boolean deleteSource = false;
		validateSource(source);
		if (options != null && options.length > 0) {
			for (CompressOptions option : options) {
				switch (option) {
				case IGNORE_TREE_STRUCTURES : ignoreTreeStructure = true; break;
				case DELETE_SOURCE : deleteSource = true; break;
				default : throw new IOException("Not supported option on decompress progress: " + option);
				}
			}
		}
		
		if (target == null) target = source.getParent();
		
		if (format == null) {
			format = detectFormat(source);
			log.debug("Detected compress file format: {}", format);
			if (format == null) {
				throw new IOException("Unsupported or unknown compress format: " + source.getFileName());
			}
		}
		
		log.info("Prepare for decompress :: [FORMAT:{}] [FILE:{}] [IGNORE_TREE_STRUCTURE:{]] [DELETE_SOURCE:{}]", format, source.getFileName(), (ignoreTreeStructure ? "TRUE" : "FALSE"), (deleteSource ? "TRUE" : "FALSE"));
		long start = System.currentTimeMillis();
		List<Path> resultList = new ArrayList<Path>();
		
		switch (format) {
		case LINUX_Z : 
			decompressZFile(source, target, timeout, resultList);
			break;
		case TAR :
		case TAR_GZIP :
		case ZIP :
		case JAR :
			extractArchiveFile(source, target, format, ignoreTreeStructure, resultList);
			break;
		case GZIP :
			decompressSingleFile(source, target, format, resultList);
			break;
		default :
			throw new IOException("Unsupported format for decompression logic: " + format);
		}
		
		if (deleteSource) {
			try {
				Files.deleteIfExists(source);
				log.debug("Source file deleted successfully: {}", source.toAbsolutePath());
			} catch(IOException e) {
				log.warn("Failed to delete source file: {}", source.toAbsolutePath(), e);
			}
		}
		
		long end = System.currentTimeMillis();
		log.info("Success to decompress in {}ms :: [SOURCE:{]] [FILE_COUNT:{}]", (end - start), source.toAbsolutePath(), resultList.size());
		return resultList;
	}
	
	private static CompressFormats detectFormat(Path source) {
		try (InputStream fis = Files.newInputStream(source);
				BufferedInputStream bis = new BufferedInputStream(fis, SIGNATURE_READ_LIMIT);) {
			bis.mark(SIGNATURE_READ_LIMIT);
			byte[] signature = new byte[SIGNATURE_READ_LIMIT];
			int length = IOUtils.readFully(bis, signature);
			bis.reset();
			if (length < 2) {
				return null;
			}
			
			if (GzipCompressorInputStream.matches(signature, length)) {
				try (GzipCompressorInputStream gis = new GzipCompressorInputStream(bis)) {
					byte[] header = new byte[512];
					int read = IOUtils.readFully(gis, header);
					if (read >= 512 && TarArchiveInputStream.matches(header, 512)) {
						return CompressFormats.TAR_GZIP;
					}
				} catch(IOException ignore) {	}
			}
			
			if (ZCompressorInputStream.matches(signature, length)) {
				return CompressFormats.LINUX_Z;
			}
			
			if (TarArchiveInputStream.matches(signature, length)) {
				return CompressFormats.TAR;
			}
			
			if (ZipArchiveInputStream.matches(signature, length)) {
				if (source.getFileName().toString().toLowerCase().endsWith(".jar")) {
					return CompressFormats.JAR;
				}
				return CompressFormats.ZIP;
			}
		} catch(IOException e) {
			log.warn("Failed to detect compress format by header bytes: {}", source.getFileName());
		}
		
		String name = source.getFileName().toString().toLowerCase();
		if (name.endsWith(".tar.gz")) {
			return CompressFormats.TAR_GZIP;
		} else if (name.endsWith(".tar")) {
			return CompressFormats.TAR;
		} else if (name.endsWith(".zip")) {
			return CompressFormats.ZIP;
		} else if (name.endsWith(".jar")) {
			return CompressFormats.JAR;
		} else if (name.endsWith(".gz")) {
			return CompressFormats.GZIP;
		} else if (name.endsWith(".z")) {
			return CompressFormats.LINUX_Z;
		} else {
			return null;
		}
	}
	
	private static void extractArchiveFile(Path source, Path target, CompressFormats format, boolean ignoreTreeStructure, List<Path> resultList) throws IOException {
		try (InputStream fis = Files.newInputStream(source);
				BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
				ArchiveInputStream ais = createArchiveInputStream(format, bis);) {
			ArchiveEntry entry;
			while ((entry = ais.getNextEntry()) != null) {
				if (!ais.canReadEntryData(entry)) continue;
				
				String entryName = entry.getName();
				Path result = resolveEntryPath(target, entryName, ignoreTreeStructure);
				
				if (entry.isDirectory()) {
					if (!ignoreTreeStructure) {
						Files.createDirectories(result);
						resultList.add(result);
					}
				} else {
					if (Files.notExists(result.getParent())) {
						Files.createDirectories(result.getParent());
					}
					
					try (OutputStream fos = Files.newOutputStream(result);
							BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);) {
						IOUtils.copy(ais, bos);
					}
					log.debug("Decompressing :: [ENTRY_NAME:{}] --> [FILE:{}]", entryName, result.toAbsolutePath());
					resultList.add(result);
				}
			}
		}
	}
	
	private static void decompressSingleFile(Path source, Path target, CompressFormats format, List<Path> resultList) throws IOException {
		Path result = null;
		String outputFileName = getUncompressedFileName(source.getFileName().toString(), format);
		if (Files.exists(target) && Files.isDirectory(target)) {
			result = target.resolve(outputFileName);
		} else {
			result = target;
		}
		
		try (InputStream fis = Files.newInputStream(source);
				BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
				CompressorInputStream cis = createCompressorInputStream(format, bis);
				OutputStream fos = Files.newOutputStream(result);
				BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);) {
			IOUtils.copy(cis, bos);
		}
		resultList.add(result);
	}
	
	private static void decompressZFile(Path source, Path target, Duration timeout, List<Path> resultList) throws IOException {
		Path result = null;
		String outputFileName = getUncompressedFileName(source.getFileName().toString(), CompressFormats.LINUX_Z);
		if (Files.exists(target) && Files.isDirectory(target)) {
			result = target.resolve(outputFileName);
		} else {
			result = target;
		}
		
		List<String> command = Arrays.asList("uncompress", "-c", result.toAbsolutePath().toString());
		try {
			log.debug("Executing command: {} --> {}", command, result.toAbsolutePath());
			CommandUtils.run(command, result, null, timeout);
		} catch (InterruptedException | TimeoutException | IOException e) {
			throw new IOException("Failed to decompress using external command: " + command, e);
		}
	}
	
	private static Path resolveEntryPath(Path target, String entryName, boolean ignoreTreeStructure) {
		String normalizedName = entryName.replace("\\", File.separator).replace("/", File.separator);
		if (normalizedName.startsWith(File.separator)) {
			normalizedName = normalizedName.substring(1);
		}
		
		Path entryPath = Paths.get(normalizedName);
		if (entryPath.isAbsolute()) {
			entryPath = entryPath.getRoot().relativize(entryPath);
		}
		
		if (ignoreTreeStructure) {
			return target.resolve(entryPath.getFileName());
		} else {
			Path resolvedPath = target.resolve(entryPath).normalize();
			if (!resolvedPath.startsWith(target.normalize())) {
				log.warn("Security Risk detected (Zip Slip or Absolute Path: '{}'. Force to ignore file tree.", entryName);
				return target.resolve(entryPath.getFileName());
			} else {
				return target.resolve(entryPath);
			}
		}
	}
	
	private static ArchiveInputStream createArchiveInputStream(CompressFormats format, InputStream is) throws IOException {
		switch (format) {
		case TAR : return new TarArchiveInputStream(is);
		case TAR_GZIP: return new TarArchiveInputStream(new GzipCompressorInputStream(is));
		case ZIP :
		case JAR : return new ZipArchiveInputStream(is);
		default : throw new IOException("Not an archive format: " + format);
		}
	}
	
	private static CompressorInputStream createCompressorInputStream(CompressFormats format, InputStream is) throws IOException {
		switch (format) {
		case GZIP : return new GzipCompressorInputStream(is);
		default : throw new IOException("Not a compressor format: " + format);
		}
	}
	
	private static String getUncompressedFileName(String fileName, CompressFormats format) {
		String expectedExt = "." + format.get();
		
		if (fileName.toLowerCase().endsWith(expectedExt)) {
			return fileName.substring(0, fileName.length() - expectedExt.length());
		} else {
			return fileName;
		}
	}
	
	private static void validateSource(Path source) throws IOException {
		if (source == null || Files.notExists(source)) throw new IOException("Source path does not exists: " + source.toAbsolutePath());
		if (Files.isDirectory(source)) throw new IOException("Source path is a directory: " + source.toAbsolutePath());
	}
}
