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
			addArtifactItemAndModule(modulePath, groupId, artifactId, version);
		}
		catch (Exception e) {
			error(modulePath + " cannot be parsed.", e);
			return -1;
		}
		
		return 0;
	}
	
	public void addArtifactItemAndModule(String modulePath, String groupId, String artifactId, String version) {
		Document projectPom = readXml("pom.xml");
		
		artifactId = artifactId + "-omod";
		
		Element artifactItem = selectArtifactItem(projectPom, artifactId);
		
		artifactItem.element("groupId").setText(groupId);
		artifactItem.element("artifactId").setText(artifactId + "-omod");
		artifactItem.element("version").setText(version);
		artifactItem.element("type").setText("omod");
		artifactItem.element("destFileName").setText(artifactId + "-" + version + ".omod");
		
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
		
		writePom(projectPom);
	}
	
	Element selectModules(Document projectPom) {
		XPath modules = DocumentHelper.createXPath("/" + toLocalElement("project") + toLocalElement("modules"));
		return (Element) modules.selectSingleNode(projectPom);
	}
	
	Document readXml(String file) {
		SAXReader reader = new SAXReader();
		Document projectPom = null;
		try {
			projectPom = reader.read(file);
		}
		catch (DocumentException e) {
			error("Xml configuration cannot be parsed.", e);
		}
		return projectPom;
	}
	
	Element selectArtifactItem(Document projectPom, String artifactId) {
		XPath artifactItemsPath = DocumentHelper.createXPath("/" + toLocalElement("project") + toLocalElement("build")
                + toLocalElement("plugins") + toLocalElement("plugin") + toLocalElement("executions")
                + toLocalElement("execution") + toLocalElement("configuration") + toLocalElement("artifactItems"));
		Element artifactItems = (Element) artifactItemsPath.selectSingleNode(projectPom);
		System.out.println(artifactItems.asXML());
		
		List<Element> existingArtifactItems = (List<Element>) artifactItems.elements();
		for (Element artifactItem : existingArtifactItems) {
			List<Element> items = (List<Element>) artifactItem.elements();
			
			for (Element item : items) {
				if (item.getName().equals("artifactId") && item.getText().equals(artifactId)) {
					
					return artifactItem;
					
				}
			}
		}
		Element newArtifactItem = artifactItems.addElement("artifactItem");
		
		newArtifactItem.addElement("groupId").addText("");
		newArtifactItem.addElement("artifactId").addText("");
		newArtifactItem.addElement("version").addText("");
		newArtifactItem.addElement("type").addText("");
		newArtifactItem.addElement("destFileName").addText("");
		
		return newArtifactItem;
	}
	
	private String toLocalElement(String name) {
		return "/*[local-name()='" + name + "']";
	}
	
	public void writePom(Document document) {
		info("Saving file.");
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream("pom.xml"), format);
			writer.write(document);
			writer.close();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			error("Project configuration is missing.");
		}
		catch (IOException e) {
			error("Cannot write to project configuration.");
		}
		
	}
	
}
