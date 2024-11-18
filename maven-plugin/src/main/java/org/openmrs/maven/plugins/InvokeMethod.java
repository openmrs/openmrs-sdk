package org.openmrs.maven.plugins;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.utility.MavenEnvironment;
import org.openmrs.maven.plugins.utility.Wizard;

import java.lang.reflect.Method;

/**
 * The purpose is this Mojo is to support testing of helper utilities that are used by the various goals.  This will:
 *   instantiate a new object of type className
 *   if this class has a setter property for a MavenExecution, this will be set
 *   it will then invoke the given testMethod on the testClass
 */
@Mojo(name = InvokeMethod.NAME)
@Data
public class InvokeMethod extends AbstractTask {

    public static final String NAME = "invoke-method";

    @Parameter(property = "className")
    String className;

    @Parameter(property = "methodName")
    String methodName;

    public void executeTask() throws MojoExecutionException {

        if (StringUtils.isBlank(className) || StringUtils.isBlank(methodName)) {
            throw new MojoExecutionException("You must supply both a className and methodName parameter");
        }

        Wizard wizard = getMavenEnvironment().getWizard();

        wizard.showMessage("Invoking: " + className + ":" + methodName);

        Class<?> clazz;
        try {
            clazz = getClass().getClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Unable to load class", e);
        }
        wizard.showMessage("Class " + clazz.getName() + " loaded successfully");

        Object instance;
        try {
            instance = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to instantiate class with " + getClass(), e);
        }
        wizard.showMessage("New instance of " + clazz.getName() + " instantiated");

        Method setterMethod = null;
        try {
            setterMethod = clazz.getMethod("set" + MavenEnvironment.class.getSimpleName(), MavenEnvironment.class);
        }
        catch (NoSuchMethodException ignored) {
        }

        if (setterMethod != null) {
            try {
                setterMethod.invoke(instance, getMavenEnvironment());
                wizard.showMessage("Instance populated with maven environment");
        }
        catch (Exception e) {
                throw new MojoExecutionException("Unable to set MavenTestExecutor on class", e);
        }
        }

        Method method;
        try {
            method = clazz.getMethod(methodName);
        }
        catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Unable to find method: " + methodName, e);
        }
        wizard.showMessage("Got test method: " + method.getName());

        try {
            method.invoke(instance);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error executing method: " + methodName, e);
        }
        wizard.showMessage("Method: " + method.getName() + " invoked successfully");
    }
}
