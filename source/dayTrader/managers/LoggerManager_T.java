package managers;

import interfaces.Manager_IF;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LoggerManager_T implements Manager_IF {

    
    private Logger logger = Logger.getLogger(LoggerManager_T.class.getName());
    
    
    public Level loglLevel() {
        return logger.getLevel();
    }
    
    @Override
    public void initialize() {

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
    
    
    public synchronized void logText(String text, Level level) {
        
            //get information about the method that called logText so we can see where our log statement is coming from
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();        
            String callingClass = stackTrace[3].getClassName();
            String callingMethod = stackTrace[3].getMethodName();
            int lineNumber = stackTrace[3].getLineNumber();
            
            Date date = new Date(System.currentTimeMillis());
            String timestamp = new SimpleDateFormat("[MM-dd-yyyy HH:mm:ss.SSS] ").format(date);
            
            String logString = timestamp + callingClass + "." + callingMethod + "():" + lineNumber + " - " + text;
            
            logger.log(level, logString);
            //TODO: delete for debug purposes only (cause logging isn't working quite right)
            System.out.println(logString);
        
        
    }
    
    public synchronized void logFault(String text, Throwable fault) {
        logger.log(Level.FATAL, text, fault);
    }

    /**
     * Get the current log level set for our logger
     * 
     * @return current log level
     */
    public synchronized Level logLevel() {
        return logger.getLevel();
    }


}
