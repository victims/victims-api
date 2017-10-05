package com.redhat.victims.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.redhat.victims.fingerprint.Algorithms;
import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.JarFile;
import com.redhat.victims.fingerprint.Key;

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
    
    public Hash(JarFile jarFile, String cve, String submitter) {
    	this.id = "";
    	this.hash = jarFile.getFingerprint().get(Algorithms.SHA512);
    	this.name = jarFile.getFileName();
    	this.format = "SHA512";
    	this.cves = new ArrayList<String>();
    	this.cves.add(cve);
    	this.submitter = submitter;
    	List<Artifact> contents = (List<Artifact>) jarFile.getRecord().get(Key.CONTENT);
    	this.files = new ArrayList<File>();
		for( Artifact a : contents) {
			Map<Algorithms, String> fingerprint = (Map<Algorithms, String>) a.get(Key.FINGERPRINT);
			if(fingerprint.containsKey(Algorithms.SHA512)) {
				String filename = a.filename();
				//MongoDB doesn't allow field names with '.'
				String withoutExtention = filename.substring(0, filename.lastIndexOf('.'));
				File file = new File(withoutExtention, (String) fingerprint.get(Algorithms.SHA512));
				this.files.add(file);
			}
		}
    }
    
    public Document asDocument() {
        return asDocument(true);
    }
    
    //Don't include the CVE when upserting
    public Document asDocument(boolean includeCve) {
        Document doc = new Document();
        doc.append("name", getName());
        doc.append("format", getFormat());
        doc.append("submitter", getSubmitter());
        doc.append("hash", getHash());
        if(includeCve) {
            doc.append("cves", getCves());
        }
        if(!files.isEmpty()) {
        	List<Document> newFiles = new ArrayList();
        	for( File f : files) {
        		Document theFile = new Document();
        		theFile.append(f.getName(), f.getHash());
        		newFiles.add(theFile);
        	}
        	doc.append("files", newFiles);
        }
        return doc;
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
