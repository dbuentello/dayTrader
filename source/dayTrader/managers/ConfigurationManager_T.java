/**
 * 
 */
package managers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import interfaces.Manager_IF;

/**
 * @author nathan
 *
 */
public class ConfigurationManager_T implements Manager_IF {

    private HashMap<String, String> configParams = new HashMap<String, String>();
    
    
    public ConfigurationManager_T(String filePath) {
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        

        File configFile = new File(filePath);

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document configDoc = dBuilder.parse(configFile);
            configDoc.getDocumentElement().normalize();
    
            NodeList configNodes = configDoc.getDocumentElement().getChildNodes();
            
            for (int i = 0; i < configNodes.getLength(); i++) {
                
                Node node = configNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element configParam = (Element) node;
                    configParams.put(configParam.getTagName(), configParam.getTextContent());
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
