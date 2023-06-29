package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

public final class XmlHelper {

	private XmlHelper() {
	}

	public static void modifyXml(File targetFile, String modificationsPath) throws MojoExecutionException {

		StringWriter writer = new StringWriter();

		try (InputStream modificationsStream = XmlHelper.class.getClassLoader().getResourceAsStream(modificationsPath)) {
			Document targetDocument = getDocument(targetFile);
			Document modificationsDocument = getDocument(modificationsStream);

			Element targetRoot = targetDocument.getRootElement();
			Element modificationsRoot = modificationsDocument.getRootElement();

			if (!targetRoot.getName().equals(modificationsRoot.getName())) {
				throw new MojoExecutionException(
						"Target file and modifications file have differing root elements -  target:" + targetRoot.getName()
								+ ", root: " + modificationsRoot.getName()
				);
			}

			applyModifications(modificationsRoot, targetRoot);

			XMLWriter xmlWriter = new XMLWriter(writer);
			xmlWriter.write(targetDocument);

			FileUtils.fileWrite(targetFile, writer.toString());

		}
		catch (IOException | DocumentException e) {
			throw new MojoExecutionException("Failed to create owa submodule", e);
		}
	}

	protected static void applyModifications(Element modificationsRoot, Element targetRoot) {
		for (Element modificationElement : modificationsRoot.elements()) {
			String copy = modificationElement.attributeValue("sdk-copy");
			if ("true".equals(copy)) {
				addModification(targetRoot, modificationElement);
			} else {
				Element targetElement = Optional.ofNullable(targetRoot.element(modificationElement.getName()))
						.orElseGet(() -> targetRoot.addElement(modificationElement.getName()));
				applyModifications(modificationElement, targetElement);
			}
		}
	}

	protected static void addChildren(Element modificationsRoot, Element targetRoot) {
		for (Element element : modificationsRoot.elements()) {
			addModification(targetRoot, element);
		}
	}

	protected static void addModification(Element targetRoot, Element modificationElement) {
		Element targetElement = targetRoot.addElement(modificationElement.getName());
		targetElement.setText(modificationElement.getText());
		//add children values
		addChildren(modificationElement, targetElement);
	}

	protected static Document getDocument(File file) throws IOException, DocumentException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return getDocument(fis);
		}
	}

	protected static Document getDocument(InputStream stream) throws IOException, DocumentException {
		return new SAXReader().read(stream);
	}
}
