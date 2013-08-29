package org.openmrs.sdk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import org.apache.commons.cli.*;

public class Tool {

	public static void main(String args[]) throws DocumentException {
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption("h", false, "displays help");
		options.addOption("a", "addModule", true,
				"adds module to pom.xml in openmrs-project");
		Option o = options.getOption("a");
		o.setArgName("path");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption('h')) {
				usage(options);
			}
			if (cmd.hasOption('a')) {
				String filePath = cmd.getOptionValue('a');
				if (filePath == null)
					System.out.println("File Path missing.");
				else
					addModule(filePath);
			}
		} catch (ParseException e) {
			usage(options);
		}

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("tool.jar", options);
	}

	private static void addModule(String fileName) {
		SAXReader reader = new SAXReader();
		Document modulePom = null;
		Element root = null;
		String groupID = null, artID = null, version = null;
		try {
			modulePom = reader.read(fileName);
			root = modulePom.getRootElement();

			for (Iterator i = root.elementIterator(); i.hasNext();) {
				Element pElement = (Element) i.next();
				if (pElement.getName().equals("groupId")) {
					System.out.println("GroupID: " + pElement.getText());
					groupID = pElement.getText();
				}
				if (pElement.getName().equals("artifactId")) {
					System.out.println("ArtifactID: " + pElement.getText());
					artID = pElement.getText();
				}
				if (pElement.getName().equals("version")) {
					System.out.println("Version: " + pElement.getText());
					version = pElement.getText();
				}
			}
		} catch (DocumentException e1) {
			System.out.println("File: " + fileName + " does not exist.");

		} catch (NullPointerException e3) {
			System.out.print("");
		}

		// Time to modify next openmrs-project pom.xml
		Document projectPom = null;
		try {
			projectPom = reader.read("openmrs-project/pom.xml");
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			System.out.println("OpenMRS-Project pom.xml is missing.");
		}
		Map<String, String> namespaceUris = new HashMap<String, String>();
		namespaceUris.put("pom", "http://maven.apache.org/POM/4.0.0");
		XPath xpath = DocumentHelper
				.createXPath("//pom:project/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-dependency-plugin']//pom:configuration//pom:artifactItems");
		XPath xpath2 = DocumentHelper.createXPath("//pom:project/pom:dependencies");
		xpath.setNamespaceURIs(namespaceUris);
		xpath2.setNamespaceURIs(namespaceUris);
		Element e = (Element) xpath.selectSingleNode(projectPom);

		if (!e.asXML().contains(artID)) {
			System.out.println("Adding module to pom.xml");
            /*
            * Creates artifact element with all needed sub elements
            * */
			Element artifact = (Element) xpath.selectSingleNode(projectPom);
            Element artifactItem = DocumentHelper.createElement("artifactItem");
            Element groupId = artifactItem.addElement("groupId");
            Element artifactId = artifactItem.addElement("artifactId");
            Element ver = artifactItem.addElement("version");
            Element type = artifactItem.addElement("type");
            Element fName = artifactItem.addElement("destFileName");

            groupId.setText(groupID);
            artifactId.setText(artID);
            ver.setText(version);
            type.setText("omod");
            fName.setText(artID + "-" + version + ".omod");

            /*
            * Creates dependency element with all needed sub elements
            * Was going to use a copy of the artifact element, however
            * I was unable to remove the sub elements.
            * Therefore had to create a separate element.
            * @TODO
            * Possible solution:
            * Create artifact element with elements (groupId, artifactId and version)
            * then create a copy, afterwards add still needed elements to the artifact
            * Element whilst keeping the dependency copy with the basics.
            * */
			Element dependencies = (Element) xpath2.selectSingleNode(projectPom);
            Element dependency = DocumentHelper.createElement("dependency");
            Element groupid = dependency.addElement("groupId");
            Element artifactid = dependency.addElement("artifactId");
            Element vers = dependency.addElement("version");

            groupid.setText(groupID);
            artifactid.setText(artID);
            vers.setText(version);


			artifact.add(artifactItem);
            dependencies.add(dependency);

			System.out.println("Saving file.");
			// write output
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("utf-8");
			XMLWriter writer;
			try {
				writer = new XMLWriter(new FileOutputStream("openmrs-project/pom.xml"), format);
				writer.write(projectPom);
				writer.close();
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				System.out.println("OpenMRS-Project pom.xml is missing.");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("Cannot write to OpenMRS-Project pom.xml");
			}

		} else
			System.out.println("Module is in present in pom.xml");

	}

}

