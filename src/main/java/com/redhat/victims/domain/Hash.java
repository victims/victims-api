package com.redhat.victims.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BsonArray;
import org.bson.BsonValue;
import org.bson.Document;

import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.Fingerprint;
import com.redhat.victims.fingerprint.JarFile;
import com.redhat.victims.fingerprint.Key;
import com.redhat.victims.fingerprint.Algorithms;

import io.vertx.core.json.JsonObject;

/*
 * {
  "fingerprint" : {
    "SHA512" : "36ddea854952d397215a32070a1bcd5565547d133701341b1059a8525e3df8fd1af4c2195b6a6aa90ba74a1f0744969f6d1b92d662e0474ba6228ad785e9df09"
  },
  "fileName" : "camel-snakeyaml",
  "record" : {
    "METADATA" : {
      "MANIFEST.MF" : {
        "Implementation-Title" : "Apache Camel",
        "Implementation-Version" : "2.17.4",
        "Manifest-Version" : "1.0"
      },
      "META-INF/maven/org.apache.camel/camel-snakeyaml/pom.properties" : {
        "groupId" : "org.apache.camel",
        "artifactId" : "camel-snakeyaml",
        "version" : "2.17.4"
      }
    },
    "FILENAME" : "camel-snakeyaml",
    "CONTENT" : [ {
      "FILENAME" : "org/apache/camel/component/snakeyaml/SnakeYAMLDataFormat.class",
      "FILETYPE" : ".class",
      "FINGERPRINT" : {
        "SHA512" : "cb1e80599bd7de814b63ad699849360b6c5d6dd33b7b7a2da6df753197eee137541c6bfde704c5ab8521e6b7dfb436d57f102f369fc0af36738668e4d1d0ff55"
      }
    } ],
    "FILETYPE" : "",
    "EMBEDDED" : [ ],
    "FINGERPRINT" : {
      "SHA512" : "36ddea854952d397215a32070a1bcd5565547d133701341b1059a8525e3df8fd1af4c2195b6a6aa90ba74a1f0744969f6d1b92d662e0474ba6228ad785e9df09"
    }
  }
}
 */

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
				File file = new File("SHA512", (String) fingerprint.get(Algorithms.SHA512));
				this.files.add(file);
			}
		}
    }
    
    public Document asDocument() {
        return asDocument(true);
    }
    
    public Document asDocument(boolean includeCve) {
        Document doc = new Document();
        doc.append("name", getName());
        doc.append("format", getFormat());
        doc.append("submitter", getSubmitter());
        doc.append("hash", getHash());
        if(includeCve) {
            doc.append("cves", getCves());
        }
        return doc;
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
