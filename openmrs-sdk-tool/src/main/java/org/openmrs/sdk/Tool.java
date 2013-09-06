package org.openmrs.sdk;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class Tool {
	
	public static void main(String args[]) throws IOException {
		Options options = new Options();
		
		options.addOption("h", "help", false, "displays help");
		options.addOption("a", "addModule", true, "adds module to project");
		Option o = options.getOption("a");
		o.setArgName("path");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		}
		catch (ParseException e) {
			usage(options);
			return;
		}
		
		if (cmd.hasOption('h') | cmd.hasOption("help")) {
			usage(options);
			return;
		}
		
		if (cmd.hasOption('a') | cmd.hasOption("addModule")) {
			String modulePath = cmd.getOptionValue('a');
			if (modulePath == null) {
				throw new IOException("Path to module is missing");
			} else {
				new Tool().addModule(modulePath);
			}
		}
	}
	
	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("tool.jar", options);
	}
	
	private static void info(String s) {
		System.out.println(s);
	}
	
	public void addModule(String modulePath) throws IOException {
		File file = new File(modulePath, "pom.xml");
		if (!file.exists()) {
			throw new IOException("Wrong path to module: " + file.getAbsolutePath());
		}

		SAXReader reader = new SAXReader();
		Document modulePom;
		Element root;

		info("Reading module configuration");

		try {
			modulePom = reader.read(file);
		} catch (DocumentException e) {
			throw new IOException(file + " cannot be parsed", e);
		}

		root = modulePom.getRootElement();
		String groupId = null, artifactId = null, version = null;

		for (Iterator i = root.elementIterator(); i.hasNext();) {
			Element element = (Element) i.next();
			if (element.getName().equals("groupId")) {
				groupId = element.getText();
			}
			if (element.getName().equals("artifactId")) {
				artifactId = element.getText();
			}
			if (element.getName().equals("version")) {
				version = element.getText();
			}
		}

		info("Updating project configuration");

		addModuleToProjectConfiguration(modulePath, groupId, artifactId, version);
	}
	
	public void addModuleToProjectConfiguration(String modulePath, String groupId, String artifactId, String version) throws IOException {
		Document projectPom = readXml("pom.xml");
		
		updateArtifactItem(projectPom, groupId, artifactId, version);

		File file = new File(modulePath);
		if (!file.getPath().equals("../")) {
			Element modules = selectModules(projectPom);
			if (modules == null) {
				modules = projectPom.getRootElement().addElement("modules");
			}

			if (!modules.asXML().contains(file.getPath())) {
				modules.addElement("module").addText(file.getPath());
			}
		}

		writeXml("pom.xml", projectPom);

		info("Configuration updated");
	}
	
	Element selectModules(Document projectPom) {
		XPath modulesPath = DocumentHelper.createXPath("/" + toLocalElement("project") + toLocalElement("modules"));
		return (Element) modulesPath.selectSingleNode(projectPom);
	}
	
	Document readXml(String filePath) throws IOException {
		SAXReader reader = new SAXReader();
		Document projectPom;
		try {
			projectPom = reader.read(filePath);
		}
		catch (DocumentException e) {
			throw new IOException(filePath + " cannot be parsed", e);
		}
		return projectPom;
	}
	
	Element updateArtifactItem(Document projectPom, String groupId, String artifactId, String version) {
		XPath artifactItemsPath = DocumentHelper.createXPath("/" + toLocalElement("project") + toLocalElement("build")
				+ toLocalElement("plugins") + toLocalElement("plugin") + toLocalElement("executions")
				+ toLocalElement("execution") + toLocalElement("configuration") + toLocalElement("artifactItems"));
		Element artifactItems = (Element) artifactItemsPath.selectSingleNode(projectPom);

		List<Element> existingArtifactItems = (List<Element>) artifactItems.elements();
		for (Element artifactItem : existingArtifactItems) {
			List<Element> artifactItemElements = (List<Element>) artifactItem.elements();

			for (Element artifactItemElement : artifactItemElements) {
				if (artifactItemElement.getName().equals("artifactId") && artifactItemElement.getText().equals(artifactId + "-omod")) {
					artifactItems.remove(artifactItem);
					break;
				}
			}
		}
		Element newArtifactItem = artifactItems.addElement("artifactItem");

		newArtifactItem.addElement("groupId").addText(groupId);
		newArtifactItem.addElement("artifactId").addText(artifactId + "-omod");
		newArtifactItem.addElement("version").addText(version);
		newArtifactItem.addElement("type").addText("jar");
		newArtifactItem.addElement("destFileName").addText(artifactId + "-" + version + ".omod");
		
		return newArtifactItem;
	}
	
	private String toLocalElement(String name) {
		return "/*[local-name()='" + name + "']";
	}
	
	void writeXml(String filePath, Document document) throws IOException {
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream(filePath), format);
			writer.write(document);
			writer.close();
		}
		catch (Exception e) {
			throw new IOException(filePath + " cannot be written", e);
		}
		
	}
	
}
