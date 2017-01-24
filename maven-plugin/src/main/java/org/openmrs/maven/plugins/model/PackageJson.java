package org.openmrs.maven.plugins.model;

import java.util.List;
import java.util.Map;

public class PackageJson {

    private String name;
    private String version;
    private String description;

    private Map<String, String> repository;
    private Map<String, String> dependencies;
    private Map<String, String> devDependencies;
    private Map<String, String> engines;
    private Map<String, String> scripts;

    private List<String> keywords;

    private String author;
    private String license;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getRepository() {
        return repository;
    }

    public void setRepository(Map<String, String> repository) {
        this.repository = repository;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, String> getDevDependencies() {
        return devDependencies;
    }

    public void setDevDependencies(Map<String, String> devDependencies) {
        this.devDependencies = devDependencies;
    }

    public Map<String, String> getEngines() {
        return engines;
    }

    public void setEngines(Map<String, String> engines) {
        this.engines = engines;
    }

    public Map<String, String> getScripts() {
        return scripts;
    }

    public void setScripts(Map<String, String> scripts) {
        this.scripts = scripts;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

}
