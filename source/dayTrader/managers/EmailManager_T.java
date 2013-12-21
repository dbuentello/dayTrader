package managers;

import interfaces.Manager_IF;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;


import util.XMLTags_T;
import dayTrader.DayTrader_T;

public class EmailManager_T implements Manager_IF {

    private boolean sendEmails = true;
    private ArrayList<String> emails = null;
    
    
    @Override
    public void initialize() {

    }

    /**
     * Send an e-mail to the addresses specified in the configuration file. 
     * 
     * @param subject - The subject of the email
     * @param body - The body of the email
     * @param attachments - List of files to attach to the email. Optional.
     */
    public void sendEmail(String subject, String body, ArrayList<String> attachments) {
        
 
    
    }

    
    @Override
    public void sleep() {
        // TODO Auto-generated method stub

    }

    @Override
    public void terminate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void wakeup() {
        // TODO Auto-generated method stub

    }

    
    private class Authenticator  {
       
 
        
    }
}
