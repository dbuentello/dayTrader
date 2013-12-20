package managers;

import interfaces.Manager_IF;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import util.XMLTags_T;
import dayTrader.DayTrader_T;

public class EmailManager_T implements Manager_IF {

    private boolean sendEmails = true;
    private ArrayList<String> emails = null;
    
    
    @Override
    public void initialize() {
        ConfigurationManager_T configMgr = (ConfigurationManager_T) (DayTrader_T.getManager(ConfigurationManager_T.class));
        emails = configMgr.getConfigParamList(XMLTags_T.CFG_EMAIL_RECIPIENTS);
        sendEmails = Boolean.parseBoolean(configMgr.getConfigParam(XMLTags_T.CFG_SEND_EMAILS));
    }

    /**
     * Send an e-mail to the addresses specified in the configuration file. 
     * 
     * @param subject - The subject of the email
     * @param body - The body of the email
     * @param attachments - List of files to attach to the email. Optional.
     */
    public void sendEmail(String subject, String body, ArrayList<String> attachments) {
        
    if(sendEmails) {
        // Sender's email ID needs to be mentioned
        String from = "daytrader@ltrade.com";
        
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", "localhost");

        Authenticator authenticator = new Authenticator();        
        Session session = Session.getInstance(properties, authenticator);

        try{
           MimeMessage message = new MimeMessage(session);
           message.setFrom(new InternetAddress(from));
           
           for (String email : emails) {
               message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
           }

           message.setSubject(subject);
           message.setText(body);

           if(attachments != null) {
               MimeBodyPart messageBodyPart = new MimeBodyPart();
               Multipart multipart = new MimeMultipart();
               
               for (String attachment : attachments) {
                   DataSource source = new FileDataSource(attachment);
                   messageBodyPart.setDataHandler(new DataHandler(source));
                   messageBodyPart.setFileName(new File(attachment).getName());
                   multipart.addBodyPart(messageBodyPart);
               }
               message.setContent(multipart);
           }
           
           // Send message
           Transport.send(message);
           
        }catch (MessagingException mex) {
           mex.printStackTrace();
        }
    
    }
    
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

    
    private class Authenticator extends javax.mail.Authenticator {
        private PasswordAuthentication authentication;
         
        public Authenticator() {
            String username = "mail_admin";
            String password = "r00t";
            authentication = new PasswordAuthentication(username, password);
        }
             
        protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
        }
        
    }
}
