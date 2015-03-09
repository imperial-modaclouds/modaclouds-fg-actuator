package imperial.modaclouds.fg.modelUpdater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

public class PCMUpdate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] processorNames= {"vm2"};
		String PCMRateFileName = "/homes/ww2210/workspace2/OfBizMDLoad/default.resourceenvironment";
		String PCMDemandFileName = "/homes/ww2210/workspace2/OfBizMDLoad/default.repository";
		String LQNFileName = "/homes/ww2210/workspace2/OfBizMDLoad/lqns/pcm2lqn-2015-01-23-111738.xml";
		String[] classes = {"main","login"};
		ClassMapping map = new ClassMapping("/data/Dropbox/maven/lqnUpdater/classMap.properties");
		ResourceMapping map2 = new ResourceMapping("/data/Dropbox/maven/lqnUpdater/resourceMap.properties");

		//ArrayList<String[]> pairs = parseLQNFile(LQNFileName, "FrontEnd_CPU_Processor", classes);
		//updateFile(PCMDemandFileName, pairs, "1000");
		
		//updatePCMModels(PCMRateFileName, PCMDemandFileName, LQNFileName, processorNames, classes);
	}

	public static void updatePCMModels(String PCMRateFileName, String PCMDemandFileName, String PCMUsageModelFileName, String LQNFileName, String[] processorNames, String[] classes, String N, String Z, String classFileName, String resourceFileName) {
		System.out.println("Start to update PCM model.");
		
		ClassMapping map = new ClassMapping(classFileName);
		ResourceMapping map2 = new ResourceMapping(resourceFileName);
		
		for (int i = 0; i < processorNames.length; i++) {
			ArrayList<String[]> pairs = parseLQNFile(LQNFileName, processorNames[i], classes);

			Map<String,String> rateResource = obtainProcessingRate(PCMRateFileName);

			updateFile(PCMDemandFileName, pairs, rateResource);
		}
		
		updateNZ(PCMUsageModelFileName, N, Z);
		
		System.out.println("PCM model updated.");
	}
	
	private static void updateNZ(String fileName, String N, String Z) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(fileName);
			
			Node population = doc.getElementsByTagName("workload_UsageScenario").item(0);
			System.out.println("Previous population: "+population.getAttributes().getNamedItem("population").getNodeValue());
			population.getAttributes().getNamedItem("population").setNodeValue(N);
			System.out.println("Updated population: "+population.getAttributes().getNamedItem("population").getNodeValue());

			Node think_time = doc.getElementsByTagName("thinkTime_ClosedWorkload").item(0);
			System.out.println("Previous think time: "+think_time.getAttributes().getNamedItem("specification").getNodeValue());
			think_time.getAttributes().getNamedItem("specification").setNodeValue(N);
			System.out.println("Updated think time: "+think_time.getAttributes().getNamedItem("specification").getNodeValue());

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<String,String> obtainProcessingRate(String FileName) {

		String rate = null;
		String resource = null;
		Map<String,String> rateResource = new HashMap<String,String>();

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(FileName);

			NodeList processors = doc.getElementsByTagName("activeResourceSpecifications_ResourceContainer");
			for (int i = 0; i < processors.getLength(); i++) {
				NamedNodeMap attMaps = processors.item(i).getAttributes();

				Element processor = (Element) processors.item(i);
				Node processRate = processor.getElementsByTagName("processingRate_ProcessingResourceSpecification").item(0);
				rate = processRate.getAttributes().getNamedItem("specification").getNodeValue();

				Node processorID = processor.getElementsByTagName("activeResourceType_ActiveResourceSpecification").item(0);
				resource = processorID.getAttributes().getNamedItem("href").getNodeValue();

				rateResource.put(resource, rate);

			}		

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rateResource;
	}

	private static void updateFile(String PCMFileName, ArrayList<String[]> pairs, Map<String,String> rateResource) {

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(PCMFileName);

			NodeList processors = doc.getElementsByTagName("steps_Behaviour");

			for (int i = 0; i < processors.getLength(); i++) {
				NamedNodeMap attMaps = processors.item(i).getAttributes();
				Node type = attMaps.getNamedItem("xsi:type");

				if (type.getTextContent().equals("seff:InternalAction")) {
					String id = attMaps.getNamedItem("id").getTextContent();
					for (int j = 0; j < pairs.size(); j ++) {
						String[] pair = pairs.get(j);
						if (pair[0].equals(id)) {
							Element processor = (Element) processors.item(i);
							Node processorIDNode = processor.getElementsByTagName("requiredResource_ParametricResourceDemand").item(0);
							String processorID = processorIDNode.getAttributes().getNamedItem("href").getTextContent();
							String rate = rateResource.get(processorID);

							if (rate != null) {
								Node demand = processor.getElementsByTagName("specification_ParametericResourceDemand").item(0);
								
								double demand_value = Double.valueOf(pair[1])*Double.valueOf(rate);
								demand.getAttributes().getNamedItem("specification").setNodeValue(String.valueOf(demand_value));
								System.out.println("Updated demand: "+demand.getAttributes().getNamedItem("specification").getNodeValue());
							}
						}
					}
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(PCMFileName));
			transformer.transform(source, result);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

	}

	private static ArrayList<String[]> parseLQNFile(String LQNFileName, String processorName, String[] classes) {
		ArrayList<String[]> pairs = new ArrayList<String[]>();

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(LQNFileName);

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

								String value = activity.getAttributes().getNamedItem("host-demand-mean").getNodeValue();
								String id = activity.getAttributes().getNamedItem("name").getNodeValue();

								String[] parts = id.split("_");
								id = parts[2];
								if (id.equals("")) {
									id = "_"+parts[3];
								}
								String[] pair = new String[2];
								pair[0] = id;
								pair[1] = value;
								pairs.add(pair);

							}
						}
					}
				}
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return pairs;

	}

}
