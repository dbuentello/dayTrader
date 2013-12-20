/**
 * 
 */
package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author nathan
 *
 */
public class Utilities_T {

    /**
     * Convert a String value to a Double. This method was taken diretly from the JavaDocs for the 
     * Double class. To ensure a call to Double.valueOf(string) does not throw an exception, the string
     * value should parse through these filters first.
     * 
     * @param s - string representing a double value
     * @return double
     */
    public static Double stringToDouble(String s) {
        
        Double returnVal = 0d;
        
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally 
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
                ("[\\x00-\\x20]*[+-]?(NaN|Infinity|((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|(\\.("+Digits+")("+Exp+")?)|(((0[xX]"
                    + HexDigits + "(\\.)?)|(0[xX]" + HexDigits + "?(\\.)" + HexDigits + "))[pP][+-]?" + Digits + "))[fFdD]?))[\\x00-\\x20]*");

        if (s.equalsIgnoreCase("")) {
            returnVal = 0d;
        }
        else if (Pattern.matches(fpRegex, s)) {
            returnVal = Double.valueOf(s);
        }
        
        return returnVal;
    }

    /**
     * 
     * @param date as string in yyyy-MM-dd format
     * @return Date  the string as a date object
     */
    public static Date StringToDate(String date)
    {
    	Calendar c = Calendar.getInstance();
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try { 
			Date d = df.parse(date);
			c.setTime(d);
		} catch (ParseException e1)  { /*do nothing*/ System.out.println("StringToDate parse error"); }

		Date d = c.getTime();
		return d;    	
    }
 
    /**
     * return next date as cdate (less 1 minute).  Passing in "2013-07-20 00:00:00"
     * will return "2013-07-20 23:59:00" - helpful to simulate SQL DATE
     * by using hql .between(today,tomorrow)
     */
    public static Date tomorrow(Date today) {
    	
    	Calendar c = Calendar.getInstance();
    	c.setTime(today);
    	c.add(Calendar.DATE, 1); c.add(Calendar.MINUTE, -1);
    	Date nextDay = c.getTime();
    	
    	return nextDay;
    }
  
    
    /**
     *  round price to 3 digits
     * @param double
     * @return rounded n
     */
    public static double round(double n)
    {
  	    
    	  Double nd = ((n * 1000.0) + 0.5);
    	  int ni = nd.intValue();
    	  n = (double)ni/1000;
    	  
    	  return n;
    }
}
