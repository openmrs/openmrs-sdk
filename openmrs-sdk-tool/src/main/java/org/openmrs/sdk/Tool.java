package org.openmrs.sdk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import org.apache.commons.cli.*;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class Tool {
	
	public static void main(String args[]) throws DocumentException {
		// create Options object
		Options options = new Options();
		
		// add t option
		options.addOption("h", "help", false, "displays help");
		options.addOption("a", "addModule", true, "adds module to pom.xml in openmrs-project");
		Option o = options.getOption("a");
		o.setArgName("path");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption('h') | cmd.hasOption("help")) {
				usage(options);
			}
			if (cmd.hasOption('a') | cmd.hasOption("addModule")) {
				String filePath = cmd.getOptionValue('a');
				if (filePath == null)
					info("File Path missing.");
				else
					addModule(filePath);
			}
		}
		catch (ParseException e) {
			usage(options);
		}
		
	}
	
	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("tool.jar", options);
	}
	
	private static void info(String s) {
		System.out.println("INFO: " + s);
	}
	
	private static void error(String s) {
		System.out.println("ERROR: " + s);
	}
	
	public static void addModule(String fileName) {
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
            // once all info has been read, add it as artifact.
            addArtifactItem(groupID,artID,version);
		}

		catch (DocumentException e1) {
			error("File: " + fileName + " does not exist.");
		}
	}
	
	public static void addArtifactItem( String groupID, String artID, String version) {
		// Time to modify next openmrs-project pom.xml
        SAXReader reader = new SAXReader();
		Document projectPom = null;
		try {
			projectPom = reader.read("openmrs-project/pom.xml");
		}
		catch (DocumentException e1) {
			error("OpenMRS-Project pom.xml is missing.");
		}
		Map<String, String> namespaceUris = new HashMap<String, String>();
		namespaceUris.put("pom", "http://maven.apache.org/POM/4.0.0");
		XPath artifactItemsPath = DocumentHelper
		        .createXPath("//pom:project/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-dependency-plugin']//pom:configuration//pom:artifactItems");
		XPath modulesPath = DocumentHelper.createXPath("//pom:project");
		artifactItemsPath.setNamespaceURIs(namespaceUris);
		modulesPath.setNamespaceURIs(namespaceUris);
		Element e = (Element) artifactItemsPath.selectSingleNode(projectPom);
		
		if (!e.asXML().contains(artID)) {
			System.out.println("Adding module to pom.xml");
			Namespace ns = new Namespace("", "http://maven.apache.org/POM/4.0.0");
			/*
			* Creates artifact element with all needed sub elements
			* */
			Element artifact = (Element) artifactItemsPath.selectSingleNode(projectPom);
			Element artifactItem = DocumentHelper.createElement(new QName("artifactItem", ns));
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
			* Creates module item.
			* Will be compiled then added as artifact.
			* */
			Element modules = (Element) modulesPath.selectSingleNode(projectPom);
			Element module = DocumentHelper.createElement(new QName("module", ns));

			module.setText("../" + artID);
			
			artifact.add(artifactItem);
			modules.add(module);

            // Write to file
            writePom(projectPom);
		} else
			info("Module is in present in pom.xml");
		
	}
	
	public static void writePom(Document document) {
		info("Saving file.");
		// write output
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream("openmrs-project/pom.xml"), format);
			writer.write(document);
			writer.close();
		}
		catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		catch (FileNotFoundException e1) {
			error("OpenMRS-Project pom.xml is missing.");
		}
		catch (IOException e1) {
			error("Cannot write to OpenMRS-Project pom.xml");
		}
		
	}
	
}
