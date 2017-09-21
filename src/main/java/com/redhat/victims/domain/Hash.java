package com.redhat.victims.domain;

import java.util.List;

import io.vertx.core.json.JsonObject;

public class Hash {
	private final String id;
	private String hash;
	private String name;
	private String format;
	private List<String> cves;
	private String submitter;
    private List<File> files;
    
    public Hash(String hash, String name, String format, List<String> cves, String submitter, List<File> files) {
    		this.id = "";
    		this.hash = hash;
    		this.name = name;
    		this.format = format;
    		this.cves = cves;
    		this.submitter = submitter;
    		this.files = files;
    }
    
    public Hash(JsonObject json) {
    	throw new RuntimeException("not implemented");
    }
    
    
	public String getId() {
		return id;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public List<String> getCves() {
		return cves;
	}

	public void setCves(List<String> cves) {
		this.cves = cves;
	}

	public String getSubmitter() {
		return submitter;
	}

	public void setSubmitter(String submitter) {
		this.submitter = submitter;
	}

	public List<File> getFiles() {
		return files;
	}

	public void setFiles(List<File> files) {
		this.files = files;
	}
}
