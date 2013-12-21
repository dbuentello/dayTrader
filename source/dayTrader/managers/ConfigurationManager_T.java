/**
 * 
 */
package managers;

import interfaces.Manager_IF;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.XMLTags_T;

/**
 * @author nathan
 *
 */
public class ConfigurationManager_T implements Manager_IF {

    private HashMap<String, String> configParams = new HashMap<String, String>();
    private HashMap<String, ArrayList<String>> configParamsList = new HashMap<String, ArrayList<String>>();
    
    public ConfigurationManager_T(String filePath) {
        
        File configFile = new File(filePath);
        if (configFile.exists() == false) {
            configFile = new File(File.class.getResource("/Default_configuration_file.xml").getFile());
        }
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document configDoc = dBuilder.parse(configFile);
            configDoc.getDocumentElement().normalize();
    
            NodeList configNodes = configDoc.getDocumentElement().getChildNodes();
            
            for (int i = 0; i < configNodes.getLength(); i++) {
                
                Node node = configNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element configParam = (Element) node;
                    if (configParam.getTagName().equals(XMLTags_T.CFG_EMAIL_RECIPIENTS)) {
                        
                        configParamsList.put(XMLTags_T.CFG_EMAIL_RECIPIENTS, new ArrayList<String>());
                        NodeList emails = configParam.getChildNodes();
                        for (int j = 0; j < emails.getLength(); j++) {
                            if (emails.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                configParamsList.get(XMLTags_T.CFG_EMAIL_RECIPIENTS).add(emails.item(j).getTextContent());
                            }
                        }
                        
                    } else {
                        configParams.put(configParam.getTagName(), configParam.getTextContent());
                    }
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
    
    
    public String getConfigParam(String paramName) {
        return configParams.get(paramName);
    }
    
    public ArrayList<String> getConfigParamList(String paramName) {
        return configParamsList.get(paramName);
    }
    
    /* (non-Javadoc)
     * @see interfaces.Manager_IF#initialize()
     */
    @Override
    public void initialize() {

    }

    /* (non-Javadoc)
     * @see interfaces.Manager_IF#sleep()
     */
    @Override
    public void sleep() {

    }

    /* (non-Javadoc)
     * @see interfaces.Manager_IF#terminate()
     */
    @Override
    public void terminate() {

    }

    /* (non-Javadoc)
     * @see interfaces.Manager_IF#wakeup()
     */
    @Override
    public void wakeup() {

    }

}
