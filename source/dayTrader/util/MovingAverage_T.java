package util;


import java.util.LinkedList;
import java.util.Queue;
import java.util.Iterator;


public class MovingAverage_T {
    private final Queue<Double> window = new LinkedList<Double>();
    private final int period;
    private double sum;
    
    // the new rolling averages
    private final Queue<Double> rollingAvg = new LinkedList<Double>();
    
    // the minimum needed to initialize
    private int INIT_MIN = 5;		// cannot be less than 2 or greater than period
 
    public MovingAverage_T(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }
 
    public void newNum(double num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
        
        // add to the rolling average
        rollingAvg.add(getAvg());
        if (rollingAvg.size() > period) {
        	rollingAvg.remove();
        }
    }
 
    // return the latest value
    public double getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sum / window.size();
    }
 
    /**
     * Get the trend (least squares slope) from the actual window values
     * @return
     */
    public double getTrend() {
    	if (window.size() < INIT_MIN) return 0;
    	
    	double x=0, y=0, x2=0, xy=0;
    	double trend=0;
    	
    	Iterator<Double> it = window.iterator();
    	
    	int n=0;
    	double first=0;
    	while (it.hasNext()) {
    		n++;
    		double v = it.next();
    		
    		// least squares
    		x += n;
    		y += v;
    		x2 += (n*n);
    		xy += (n*v);
    		
    		// simple trend
    		//if (n==1) first = v;
    		//trend = v - first;
    	}
    	
    	trend = ((n*xy)-(x*y))/((n*x2)-(x*x));
    	
    	return trend;	
    }
    
    /**
     * use the calculated rolling averages to calculate a smoothed trend
     *  
     * @return
     */
    public double getRollingTrend() {
    	if (rollingAvg.size() < INIT_MIN) return 0;
    	
    	double x=0, y=0, x2=0, xy=0;
    	double trend=0;
    	
    	Iterator<Double> it = rollingAvg.iterator();
    	
    	int n=0;
    	double first=0;
    	while (it.hasNext()) {
    		n++;
    		double v = it.next();
    		
    		// least squares
    		x += n;
    		y += v;
    		x2 += (n*n);
    		xy += (n*v);
    		
    		// simple trend
    		if (n==1) first = v;
    		trend = v - first;
    	}
    	
    	trend = ((n*xy)-(x*y))/((n*x2)-(x*x));
    	
    	return trend;	
    }
    
    
    // return the percent change from end to start of rolling average (alt)
    public double getRateOfChange()
    {
    	if (rollingAvg.size() < INIT_MIN) return 0;

    	Double[] change = (Double[])rollingAvg.toArray(new Double[rollingAvg.size()]);
    	
    	return (change[change.length-1]-change[0])/change[0];
    }
}

/***** Test code

  	double[] testData = {1,2,3,4,5,5,4,3,2,1};
      int[] windowSizes = {3,5};
      for (int windSize : windowSizes) {
          MovingAverage_T ma = new MovingAverage_T(windSize);
          for (double x : testData) {
              ma.newNum(x);
              System.out.println("Next number = " + x + ", SMA = " + ma.getAvg() + " Trend="+ma.getTrend() + " RTrend="+ma.getRollingTrend());
           }
           System.out.println();
      }
}
***/
