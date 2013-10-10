package managers;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import interfaces.Manager_IF;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LoggerManager_T implements Manager_IF {

    
    private static Logger logger = Logger.getLogger(LoggerManager_T.class.getName());
    
    @Override
    public void initialize() {

    }

    @Override
    public void run() {
        //Do nothing
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
    
    
    public static void logText(String text, Level level) {
        
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
    
    public static void logFault(String text, Throwable fault) {
        logger.log(Level.FATAL, text, fault);
    }
    

}
