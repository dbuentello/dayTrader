package managers;

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
        logger.log(level, text);
    }
    
    public static void logFault(String text, Throwable fault) {
        logger.log(Level.FATAL, text, fault);
    }
    

}
