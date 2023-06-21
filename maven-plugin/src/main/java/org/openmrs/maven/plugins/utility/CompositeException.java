package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class CompositeException extends Exception {

    private final Map<String, Exception> exceptions = new LinkedHashMap<>();

    public CompositeException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return exceptions.entrySet().stream()
                .map(exception -> "\n" + exception.getKey() + "\n\t" + ExceptionUtils.getFullStackTrace(exception.getValue()))
                .collect(Collectors.joining());
    }

    public void add(String path, Exception e) {
        exceptions.put(path, e);
    }

    public void checkAndThrow() throws MojoExecutionException{
        if(!exceptions.isEmpty()){
            throw new MojoExecutionException(getMessage(), this);
        }
    }
}
