package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.LinkedHashMap;
import java.util.Map;

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
        StringBuilder builder = new StringBuilder();
        for(Map.Entry<String, Exception> exception: exceptions.entrySet()){
            builder.append("\n").append(exception.getKey()).append("\n").append("\t").append(ExceptionUtils.getFullStackTrace(exception.getValue()));
        }
        return builder.toString();
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
