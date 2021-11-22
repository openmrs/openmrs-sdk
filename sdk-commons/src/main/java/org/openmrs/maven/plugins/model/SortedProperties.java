package org.openmrs.maven.plugins.model;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simple extension of Java Properties to support naturally sorted keys
 */
public class SortedProperties extends Properties {

    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> keys = super.keys();
    
        SortedSet<Object> sortedKeys = new TreeSet<>();
        
        while (keys.hasMoreElements()) {
            sortedKeys.add(keys.nextElement());
        }
        
        return Collections.enumeration(sortedKeys);
    }
}
