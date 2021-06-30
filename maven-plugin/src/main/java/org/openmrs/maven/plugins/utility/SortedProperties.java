package org.openmrs.maven.plugins.utility;

import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Simple extension of Java Properties to support naturally sorted keys
 */
public class SortedProperties extends Properties {

    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> keys = super.keys();
        Vector<Object> v = new Vector<>();
        while (keys.hasMoreElements()) {
            v.add(keys.nextElement());
        }
        Collections.sort(v);
        return v.elements();
    }
}
