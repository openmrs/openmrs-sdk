package org.openmrs.maven.plugins.utility;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class XmlHelperTest {

    @Test
    public void testApplyChanges(){
        Element modificationsRoot = new DefaultElement("project");
        Element targetRoot = new DefaultElement("project");

        Document target = DocumentFactory.getInstance().createDocument(targetRoot);
        Document modifications = DocumentFactory.getInstance().createDocument(modificationsRoot);

        target.getRootElement().addElement("dependencies");

        Element dependency = modifications.getRootElement()
                .addElement("dependencies")
                .addElement("dependency").addAttribute("sdk-copy", "true");

        dependency.addElement("groupId").setText("org.openmrs.module");
        dependency.addElement("artifactId").setText("metadtamapiing");
        dependency.addElement("version").setText("2345");

        assertThat(target.getRootElement().element("dependencies").element("dependency"), is(nullValue()));

        XmlHelper.applyModifications(modifications.getRootElement(), target.getRootElement());

        assertThat(target.getRootElement().element("dependencies").element("dependency"), is(notNullValue()));
        assertThat(target.getRootElement().element("dependencies").element("dependency").element("groupId").getText(), is("org.openmrs.module"));
    }

    @Test
    public void testApplyChanges2(){
        Element modificationsRoot = new DefaultElement("project");
        Element targetRoot = new DefaultElement("project");

        Document target = DocumentFactory.getInstance().createDocument(targetRoot);
        Document modifications = DocumentFactory.getInstance().createDocument(modificationsRoot);

        Element dependency = modifications.getRootElement()
                .addElement("dependencies")
                .addElement("dependency").addAttribute("sdk-copy", "true");

        dependency.addElement("groupId").setText("org.openmrs.module");
        dependency.addElement("artifactId").setText("metadtamapiing");
        dependency.addElement("version").setText("2345");

        assertThat(target.getRootElement().element("dependencies"), is(nullValue()));

        XmlHelper.applyModifications(modifications.getRootElement(), target.getRootElement());

        assertThat(target.getRootElement().element("dependencies").element("dependency"), is(notNullValue()));
    }
}
