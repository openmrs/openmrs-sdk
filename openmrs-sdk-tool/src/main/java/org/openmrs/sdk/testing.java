package org.openmrs.sdk;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import java.util.List;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 13-09-02
 * Time: 10:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class testing {

    public static void main (String []args){
        addModule("/Users/admin/openmrs/gsoc/openmrs-sdk/openmrs-sdk-tool/test.xml");

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
                if (pElement.getName().equals("properties")) {
                    System.out.println("Found: "+pElement.getName());

                    Element e = DocumentHelper.createElement("modules");
                    Element module = e.addElement("module");
                    module.setText("testing");
                    System.out.println(pElement.getName());
                    ((Element) i.next()).getParent().add(e);
                    pElement.getParent().add(e);




                }

            }
           writePom(modulePom);

        }

        catch (DocumentException e1) {
           System.out.print("file missing");
        }
    }

    public static void writePom(Document document) {
        System.out.println("Saving file.");
        // write output
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("utf-8");
        XMLWriter writer;
        try {
            writer = new XMLWriter(new FileOutputStream("/Users/admin/openmrs/gsoc/openmrs-sdk/openmrs-sdk-tool/test.xml"), format);
            writer.write(document);
            writer.close();
        }
        catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        catch (FileNotFoundException e1) {
            System.out.println("OpenMRS-Project pom.xml is missing.");
        }
        catch (IOException e1) {
            System.out.println("Cannot write to OpenMRS-Project pom.xml");
        }

    }


}
