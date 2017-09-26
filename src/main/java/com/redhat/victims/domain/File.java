package com.redhat.victims.domain;

public class File {
	private String type;
	private String hash;
	
	public File(String name, String hash) {
		this.type = name;
		this.hash = hash;
	}
	
	public String getName() {
		return type;
	}
	public void setName(String name) {
		this.type = name;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
}
