package com.adaptor.custom.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.adaptor.custom.utils.constants.CompressFormats;
import com.adaptor.custom.utils.constants.CompressOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompressUtils {
	private static final int BUFFER_SIZE = 3 * 1024;
	private static final List<CompressFormats> ARCHIVE_FORMATS = Arrays.asList(new CompressFormats[] {CompressFormats.TAR, CompressFormats.ZIP, CompressFormats.JAR});
	
	public static void compress(String prefix, Path source, List<Path> list, Path base, Path target, Duration timeout, CompressFormats format, CompressOptions... options) throws IOException, ArchiveException {
		boolean ignoreTreeStructure = false;
		boolean ignoreParentsDirectory = false;
		boolean preserveAbsolutePath = false;
		if (options != null) {
			for (CompressOptions option : options) {
				switch (option) {
				case IGNORE_TREE_STRUCTURES : ignoreTreeStructure = true; break;
				case IGNORE_PARENTS_DIRECTORY : ignoreParentsDirectory = true; break;
				case PRESERVE_ABSOLUTE_PATH : preserveAbsolutePath = true; break;
				default : throw new IOException("Not supported options on compress process '" + option + "'");
				}
			}
		}
		
		if (format == null) {
			format = extractFormat(target);
		}
		
		validateCompressArguments(source, list, base, target, format, options);
		
		if (prefix == null) {
			prefix = "";
		} else if (!prefix.isEmpty()) {
			if (!prefix.endsWith(File.separator)) {
				prefix = prefix + File.separator;
			}
		}
		
		log.info("Prepare form compress :: [FORMAT:{}] [IGNORE_TREE_STRUCTUR:{}] [IGNORE_PARENTS_DIRECTORY:{}] [PRESERVE_ABSOLUTE_PATH:{}] [PREFIX:{}]", format.get(), ignoreTreeStructure, ignoreParentsDirectory, preserveAbsolutePath, prefix);
		long start = System.currentTimeMillis();
		
		if (ARCHIVE_FORMATS.contains(format)) {
			try (OutputStream fos = Files.newOutputStream(target);
					BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
					ArchiveOutputStream aos = new ArchiveStreamFactory().createArchiveOutputStream(format.get(), bos);) {
				if (aos instanceof TarArchiveOutputStream) {
					TarArchiveOutputStream tos = (TarArchiveOutputStream) aos;
					tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
					tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
				}
				addToEntry(aos, prefix, list, base, preserveAbsolutePath, ignoreParentsDirectory, ignoreTreeStructure);
			}
		} else {
			switch (format) {
				case GZIP : 
					try (OutputStream fos = Files.newOutputStream(target);
							BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
							GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos);
							InputStream fis = Files.newInputStream(list.get(0));
							BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);) {
						IOUtils.copy(bis, gos);
					}
					break;
				case TAR_GZIP :
					try (OutputStream fos = Files.newOutputStream(target);
							BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
							GzipCompressorOutputStream gos = new GzipCompressorOutputStream(bos);
							TarArchiveOutputStream tos = new TarArchiveOutputStream(gos);) {
						tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
						tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
						addToEntry(tos, prefix, list, base, preserveAbsolutePath, ignoreParentsDirectory, ignoreTreeStructure);
						
					}
					break;
				case LINUX_Z :
					List<String> command = Arrays.asList("compress", "-f", "-c", list.get(0).toAbsolutePath().toString());
					try {
						CommandUtils.run(command, target, null, timeout);
					} catch (InterruptedException | TimeoutException | IOException e) {
						log.error("Fail to command 'compress'{}", System.lineSeparator(), e);
						throw new IOException(e);
					}
					break;
				default :
					throw new IOException("Unsupported compress format '" + format.get() + "'");
			}
		}
	}
	
	private static CompressFormats extractFormat(Path target) throws IOException {
		if (target != null) {
			String fileName = target.getFileName().toString();
			String lower = fileName.toLowerCase();
			if (lower.endsWith(".tar.gz")) {
				return CompressFormats.TAR_GZIP;
			} else if (lower.endsWith(".tar")) {
				return CompressFormats.TAR;
			} else if (lower.endsWith(".zip")) {
				return CompressFormats.ZIP;
			} else if (lower.endsWith(".jar")) {
				return CompressFormats.JAR;
			} else if (lower.endsWith(".z")) {
				return CompressFormats.LINUX_Z;
			} else if (lower.endsWith(".gz")) {
				return CompressFormats.GZIP;
			} else {
				throw new IOException("Unsupported file compress format '" + lower + "' of '" + fileName + "'");
			}
		} else {
			throw new NullPointerException("Target path is null");
		}
	}
	
	private static void addToEntry(ArchiveOutputStream aos, String prefix, List<Path> list, Path base, boolean preserveAbsolutePath, boolean ignoreParentsDirectory, boolean ignoreTreeStructure) throws IOException {
		for (int i = 0; i < list.size(); i++) {
			Path file = list.get(i);
			String name = null;
			if (preserveAbsolutePath) {
				name = file.toAbsolutePath().toString();
			} else if (ignoreParentsDirectory && base != null) {
				name = base.relativize(file).toString();
			} else if (ignoreTreeStructure) {
				name = file.getFileName().toString();
			}
			
			if (name == null) {
				 if (base == null) {
					 name = file.getFileName().toString();
				 } else {
					 name = base.getParent().relativize(file).toString();
				 }
			}
			
			name = prefix + name;
			log.debug("Processing {}/{} :: [FILE:{}] --> [ENTRY_NAME:{}]", (i + 1), list.size(), file.toAbsolutePath(), name);
			ArchiveEntry entry = aos.createArchiveEntry(file.toFile(), name);
			aos.putArchiveEntry(entry);
			try (InputStream fis = Files.newInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);) {
				IOUtils.copy(bis, aos);
			}
		}
	}
	
	private static void validateCompressArguments(Path source, List<Path> list, Path base, Path target, CompressFormats format, CompressOptions... options) throws IOException {
		if (source == null) {
			if (list == null || list.size() == 0) {
				throw new IOException("Source path or Source path list required");
			} else {
				if (base == null) {
					throw new IOException("Compress base on List<Path> required real base directory path");
				}
			}
		} else {
			try (Stream<Path> stream = Files.walk(source)) {
				list = stream.filter(path -> Files.isRegularFile(path)).collect(Collectors.toList());
			}
			
			if (base == null) {
				if (Files.isDirectory(source)) {
					base = source;
				}
			}
		}
		
		if (format == CompressFormats.GZIP || format == CompressFormats.LINUX_Z) {
			if (source == null) {
				if (list.size() == 1) {
					if (Files.isDirectory(list.get(0))) {
						throw new IOException("Can not compress directory into '" + format + "'");
					}
				} else {
					throw new IOException("Can not compress multiple files into '" + format + "'");
				}
			} else {
				if (Files.isDirectory(source)) {
					throw new IOException("Can not compress directory into '" + format + "'");
				}
			}
		}
		
		if (target == null && format != null) {
			if (source == null) {
				Path temp = list.stream().filter(Files::isRegularFile).findFirst().get();
				String fileName = FileUtils.createFileName(temp.getFileName().toString(), format.get());
				target = temp.getParent().resolve(fileName);
			} else {
				String fileName = FileUtils.createFileName(source.getFileName().toString(), format.get());
				target = source.getParent().resolve(fileName);
			}
		}
	}
}
