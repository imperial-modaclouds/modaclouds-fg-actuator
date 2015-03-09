package imperial.modaclouds.fg.modelUpdater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class LQNUpdate 
{
	public static void main( String[] args )
	{
		String fileName = "/homes/ww2210/workspace2/OfBizMDLoad/lqns/pcm2lqn-2015-01-23-111738.xml";
		String processorName = "vm2";
		String[] classes = {"main","login"};
		String[] values = {"22","33"};
		ClassMapping map = new ClassMapping("/data/Dropbox/maven/lqnUpdater/classMap.properties");
		ResourceMapping map2 = new ResourceMapping("/data/Dropbox/maven/lqnUpdater/resourceMap.properties");

		//updateFile(fileName,processorName,classes,values);
	}
	
	public static void updateFile(String fileName, String[] processorNames, String[] classes, String[] values, String classFileName, String resourceFileName, String N, String Z) {
		System.out.println("Start to update LQN model.");
		
		ClassMapping map = new ClassMapping(classFileName);
		ResourceMapping map2 = new ResourceMapping(resourceFileName);
		
		for (int i = 0; i < processorNames.length; i++) {
			updateSingleProcessor(fileName,processorNames[i],classes,values);
		}
		
		updateNZ(fileName, N, Z);
		
		System.out.println("LQN model updated.");
	}
	
	private static void updateNZ(String fileName, String N, String Z) {

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(fileName);
			
			NodeList processors = doc.getElementsByTagName("processor");
			for (int i = 0; i < processors.getLength(); i++) {
				NamedNodeMap attMaps = processors.item(i).getAttributes();
				Node name = attMaps.getNamedItem("name");
				
				if (name.getTextContent().contains("UsageScenario")) {
					Element processor = (Element) processors.item(i);
					Element task = (Element) processor.getElementsByTagName("task").item(0);
					System.out.println("Previous population: "+task.getAttributes().getNamedItem("multiplicity").getNodeValue());
					task.getAttributes().getNamedItem("multiplicity").setNodeValue(N);
					System.out.println("Updated population: "+task.getAttributes().getNamedItem("multiplicity").getNodeValue());
				
					System.out.println("Previous think time: "+task.getAttributes().getNamedItem("think-time").getNodeValue());
					task.getAttributes().getNamedItem("think-time").setNodeValue(N);
					System.out.println("Updated think time: "+task.getAttributes().getNamedItem("think-time").getNodeValue());

				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void updateSingleProcessor(String fileName, String processorName, String[] classes, String[] values) {
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(fileName);

			NodeList processors = doc.getElementsByTagName("processor");
			for (int i = 0; i < processors.getLength(); i++) {
				NamedNodeMap attMaps = processors.item(i).getAttributes();
				Node name = attMaps.getNamedItem("name");

				String mappedprocessorName = ResourceMapping.resourceMap.get(name.getTextContent());

				if (mappedprocessorName != null && mappedprocessorName.equals(processorName)) {
					Element processor = (Element) processors.item(i);
					Element task = (Element) processor.getElementsByTagName("task").item(0);
					NodeList entries = task.getElementsByTagName("entry");
					for (int j = 0; j < entries.getLength(); j++) {
						NamedNodeMap attEntryMaps = entries.item(j).getAttributes();
						for (int k = 0; k < classes.length; k++) {
							String LQNclassName = attEntryMaps.getNamedItem("name").getTextContent();							
							String[] splits = LQNclassName.split("_");

							String mapClassName = ClassMapping.classMap.get(splits[1]);
							if (mapClassName != null && mapClassName.equals(classes[k])) {
								Element entry = (Element) entries.item(j);
								Node activity = entry.getElementsByTagName("activity").item(0);
								System.out.println("Job class: "+ classes[k] + " Previous demand: "+activity.getAttributes().getNamedItem("host-demand-mean").getNodeValue());
								activity.getAttributes().getNamedItem("host-demand-mean").setNodeValue(values[k]);
								System.out.println("Job class: "+ classes[k] + " Updated demand: "+activity.getAttributes().getNamedItem("host-demand-mean").getNodeValue());
							}

						}
					}
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(fileName));
			transformer.transform(source, result);


		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} 

	}
}
