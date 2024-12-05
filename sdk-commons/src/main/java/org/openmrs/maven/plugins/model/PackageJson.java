package org.openmrs.maven.plugins.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
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
}