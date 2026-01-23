package com.adaptor.custom.utils.constants;

public enum CompressFormats {
	TAR("tar"),
	ZIP("zip"),
	JAR("jar"),
	GZIP("gz"),
	TAR_GZIP("tar.gz"),
	LINUX_Z("z");
	
	private String ext;
	
	private CompressFormats(String ext) {
		this.ext = ext;
	}
	
	public String get() {
		return this.ext;
	}
}
