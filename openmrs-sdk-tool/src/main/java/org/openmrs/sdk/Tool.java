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
	
	public static void main(String args[]) throws DocumentException {
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
				error("Path to module is missing.");
			} else {
				new Tool().addModule(modulePath);
			}
		}
	}
	
	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Tool.jar", options);
	}
	
	private static void info(String s) {
		System.out.println("INFO: " + s);
	}
	
	private static void error(String s) {
		System.out.println("ERROR: " + s);
	}
	
	private static void error(String s, Exception e) {
		System.out.println("ERROR: " + s + "\n\n" + ExceptionUtils.getStackTrace(e));
	}
	
	public int addModule(String modulePath) {
		File file = new File(modulePath, "pom.xml");
		if (!file.exists()) {
			error("Wrong path to module: " + file.getAbsolutePath());
			return -1;
		}
		try {
			SAXReader reader = new SAXReader();
			Document modulePom;
			Element root;
			
			modulePom = reader.read(file);
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
			addModuleToProjectConfiguration(modulePath, groupId, artifactId, version);
		}
		catch (Exception e) {
			error(modulePath + " cannot be parsed.", e);
			return -1;
		}
		
		return 0;
	}
	
	public void addModuleToProjectConfiguration(String modulePath, String groupId, String artifactId, String version) {
		Document projectPom = readXml("pom.xml");
		
		updateArtifactItem(projectPom, groupId, artifactId, version);

		Element modules = selectModules(projectPom);
		if (modules == null) {
			modules = projectPom.getRootElement().addElement("modules");
		}
		
		File file = new File(modulePath);
		if (!modules.asXML().contains(file.getPath())) {
			modules.addElement("module").addText(file.getPath());
		} else {
			info("Module is already added to modules");
		}
		
		writeXml("pom.xml", projectPom);
	}
	
	Element selectModules(Document projectPom) {
		XPath modulesPath = DocumentHelper.createXPath("/" + toLocalElement("project") + toLocalElement("modules"));
		return (Element) modulesPath.selectSingleNode(projectPom);
	}
	
	Document readXml(String filePath) {
		SAXReader reader = new SAXReader();
		Document projectPom = null;
		try {
			projectPom = reader.read(filePath);
		}
		catch (DocumentException e) {
			error("Xml configuration cannot be parsed.", e);
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
		newArtifactItem.addElement("type").addText("omod");
		newArtifactItem.addElement("destFileName").addText(artifactId + "-" + version + ".omod");
		
		return newArtifactItem;
	}
	
	private String toLocalElement(String name) {
		return "/*[local-name()='" + name + "']";
	}
	
	void writeXml(String filePath, Document document) {
		info("Saving file.");
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream(filePath), format);
			writer.write(document);
			writer.close();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			error("Configuration is missing.");
		}
		catch (IOException e) {
			error("Cannot write to project configuration.");
		}
		
	}
	
}
