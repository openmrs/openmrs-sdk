package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;

public class XmlHelper {

    public void modifyXml(File targetfile, String modificationsPath) throws MojoExecutionException {

        StringWriter writer = new StringWriter();

        try (
                InputStream modificationsStream = getClass().getClassLoader().getResourceAsStream(modificationsPath)
        ){
            Document targetDocument = getDocument(targetfile);
            Document modificationsDocument = getDocument(modificationsStream);

            Element targetRoot = targetDocument.getRootElement();
            Element modificationsRoot = modificationsDocument.getRootElement();

            if(!targetRoot.getName().equals(modificationsRoot.getName())){
                throw new IllegalArgumentException(
                        "Target file and modifications file have differing root elements -  target:"+targetRoot.getName()+", root: "+modificationsRoot.getName()
                );
            }

            applyModifications(modificationsRoot, targetRoot);

            XMLWriter xmlWriter = new XMLWriter( writer );
            xmlWriter.write( targetDocument );

            FileUtils.fileWrite(targetfile, writer.toString());

        } catch (IOException | DocumentException e) {
            throw new MojoExecutionException("Failed to create owa submodule", e);
        }
    }

    protected void applyModifications(Element modificationsRoot, Element targetRoot) {
        for(Object object : modificationsRoot.elements()){
            if(object instanceof Element){
                Element modificationElement = (Element) object;
                String copy = modificationElement.attributeValue("sdk-copy");
                if("true".equals(copy)){
                    addModification(targetRoot, modificationElement);
                } else {
                    Element targetElement = targetRoot.element(modificationElement.getName());
                    if(targetElement == null) {
                        targetElement = targetRoot.addElement(modificationElement.getName());
                    }
                    applyModifications(modificationElement, targetElement);
                }
            }
        }
    }

    protected void addChildren(Element modificationsRoot, Element targetRoot){
        for(Object object : modificationsRoot.elements()){
            if(object instanceof Element){
                addModification(targetRoot, (Element) object);
            }
        }
    }

    protected void addModification(Element targetRoot, Element modificationElement) {
        Element targetElement = targetRoot.addElement(modificationElement.getName());
        targetElement.setText(modificationElement.getText());
        //add children values
        addChildren(modificationElement, targetElement);
    }

    protected Document getDocument(File file) throws IOException, DocumentException {
        return getDocument(new FileInputStream(file));
    }

    protected Document getDocument(InputStream stream) throws IOException, DocumentException {
        Reader targetReader = ReaderFactory.newXmlReader(stream);
        Document doc = new SAXReader().read(targetReader);
        IOUtils.closeQuietly(stream);
        return doc;
    }
}
