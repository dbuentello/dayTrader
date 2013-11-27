package trader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Level;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import dayTrader.DayTrader_T;
import util.dtLogger_T;

import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.TimeManager_T;
import marketdata.MarketData_T;

import trader.Holding_T;
import trader.DailyNet_T;

import util.Utilities_T;


public class TraderCalculator_T {
  /* {src_lang=Java}*/

    /** References to the Managers we need */
    private DatabaseManager_T databaseManager;
    private TimeManager_T timeManager;
    private dtLogger_T Log;  // shorthand
    
    
    /**
     * 
     */
    public TraderCalculator_T() {
	    databaseManager   = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    timeManager       = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    //logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
	    Log = DayTrader_T.dtLog;
    }

 

    /**
     * Get the $ amount of available capital
     * 
     * @return (double) the amount of capital we can invest
     */
    public double getCapital()
    {
    	// TODO!!!
    	//DailyNet_T dailyNet = getLatestDailyNet();
    	//if (dailyNet.getPrice() == null)
    	//    return 0.0;						// TODO
    	
        return 10000.00;   
  
    }
    
    /**
     * for the most recent updates to Holdings, the net profit/loss field
	 * will be empty.  Update the sell total, and calculate the net. Add this to our daily net table
	 * 
	 * ??must be done before todays losers are determined, and the Holdings Table updated for the new losers
     */
    public void calculateNet()
    {
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
    	//SELECT id, buy_volume, buy_total, sell_price FROM $tableName WHERE DATE(buy_date) = \"$buy_date\" AND net IS NULL
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
//SALxx        	.add(Restrictions.ge("buyDate", buyDate))
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
        	.add(Restrictions.isNull("net"));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        //session.close();
        
        if (holdingData.size() != Trader_T.MAX_BUY_POSITIONS)
        {
        	Log.println("WARNING! something is fishy in Holdings... "+holdingData.size()+" rows retrieved.  Should be "+Trader_T.MAX_BUY_POSITIONS);
        }
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	
        	double buyPrice  = holding.getBuyPrice();
        	double sellPrice = holding.getSellPrice();
        	long   volume    = holding.getVolume();
        	
        	if (sellPrice == 0.00)
        	{
        		Log.println("[ERROR] CalculateNet() no sell price for "+holding.getSymbolId());
        	}
        	else
        	{
           	    
        		//LOG
                String netGL = "EVEN";
                if (sellPrice > buyPrice) { netGL = "GAIN"; }
                if (sellPrice < buyPrice) { netGL = "LOSS"; }

                double delta = sellPrice - buyPrice;
                delta = Utilities_T.round(delta);

                Log.println("*** SOLD " +holding.getSymbolId()+ " at a " +netGL+" of $"+delta+" ("+sellPrice+"/"+buyPrice+" "+
                				holding.getRemaining()+ " remaining) at "+holding.getSellDate().toString()+" ***"); 

                
        		double buyTotal  = buyPrice * volume;
        		double sellTotal = sellPrice * volume;
        		double net = sellTotal - buyTotal;
        		net = Utilities_T.round(net);
        		
        		// update the Holdings net for this stock in the Holdings Table
        		
                holding.setNet(net);
        		//holding.update();						// SAL why doesnt this worK?  no errors
        												// but this does?
        		holding.updateNet(holding.getId(), net);
	
        		
        		cumNet += net;
        		cumVol += volume;
        	}
        	
        }  // next Holding

        // update daily table with net for today

        // get latest capital record from DB
        
        double startCapital;
        
        // TODO:
if (1==0) {
        DailyNet_T dailyNet = getLatestDailyNet();
        dailyNet.setNet(cumNet);
        dailyNet.setVolume(cumVol);
        dailyNet.update();			// nope
        dailyNet.updateNet(); 		//yup
        
        startCapital = dailyNet.getPrice();		// TODO put double here
} else {
	
        //"SELECT id, price from DailyNet ORDER BY date DESC LIMIT 1";

        criteria = session.createCriteria(DailyNet_T.class)
        	.addOrder(Order.desc("date"))
        	.setMaxResults(1);

        @SuppressWarnings("unchecked")
        List<DailyNet_T> dailyNet = criteria.list();
        
            
        if (dailyNet.size() != 1)
        {
         	Log.println("FATAL: calculateNet() Cant get starting capital value from DailyNet!");
         	// throw exception
         	return;			//SALxx TODO
        }
        
        long id = dailyNet.get(0).getId();
        startCapital = dailyNet.get(0).getPrice();

        // update our net for today
        // UPDATE DailyNet SET $netName=$cum_net, totalVolume=$cum_vol WHERE id=$id;";
        DailyNet_T dn = dailyNet.get(0);
        dn.setNet(cumNet);
        dn.setVolume(cumVol);
        dn.update();					// nope
        								// yup
        dn.updateNet(cumNet, cumVol, dn.getId());
}        

        // tell us about it
        Date today = timeManager.getCurrentTradeDate();
        Log.println("*** Net for "+today.toString()+" is $"+cumNet+" on "+cumVol+" shares ***\n");


        // add new record for starting point for tomorrow
        Date date = timeManager.getNextTradeDate();

        // we can do it two ways - cumulative (pev+cumNet) or start fresh every day
        // TEST--- always start at 10000
        startCapital = 10000;
        // TEST---

        //INSERT INTO DailyNet (date, price) VALUES (\"$date\", $start_capital+$cum_net)";
        DailyNet_T newRecord = new DailyNet_T();
        newRecord.setDate(date);
        newRecord.setPrice(startCapital);
        newRecord.insertOrUpdate();
        
        session.close();
        
    }


    /**
     * get the latest (last) record from the DailyNet Table
     * we want to get the starting capital at the start of the day,
     * and the record id for the end of the day update
     * 
     * @return DaiyNet_T
     */
    private DailyNet_T getLatestDailyNet()
    {
    	//"SELECT id, price from DailyNet ORDER BY date DESC LIMIT 1";

       	Session session = DatabaseManager_T.getSessionFactory().openSession();
       	
       	Criteria criteria = session.createCriteria(DailyNet_T.class)
       			.addOrder(Order.desc("date"))
       			.setMaxResults(1);

       	@SuppressWarnings("unchecked")
       	List<DailyNet_T> dailyNet = criteria.list();
        
       	if (dailyNet.size() != 1)
       	{
       		Log.println("FATAL: getLatestDailyNet() Cant get starting capital record from DailyNet!");
       		// throw exception
       		DailyNet_T badRet = new DailyNet_T();
       		return badRet;			//SALxx TODO now the id is 0, and price is null
       	}
       	
       	return dailyNet.get(0);
    }

    /**
     * Create End of Day Holdings Report
     */
    public void CreateReport()
    {
    	String reportName = "dt_"+timeManager.getCurrentTradeDate().toString();
    	dtLogger_T report = new dtLogger_T();
    	report.open("/home/steve/Reports/"+reportName+".rpt");

    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	//SELECT * FROM Holdings WHERE DATE(buy_date) = \"$buy_date\"
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        if (holdingData.size() != Trader_T.MAX_BUY_POSITIONS)
        {
        	Log.println("WARNING! something is fishy in Holdings... "+holdingData.size()+" rows retrieved.  Should be "+Trader_T.MAX_BUY_POSITIONS);
        }
        
        //                                  actual    desired
        report.println("Symbol\t\tBuy/Sell\t\t$$\tact$$\tvolume\tleft\tEOD$$\task/bid\t\tdvol\tOrderId");
        
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	
        	Holding_T holding = it.next();
        	
        	double buyPrice  = holding.getBuyPrice();
        	double sellPrice = holding.getSellPrice();
        	long   volume    = holding.getVolume();
        	
        	double buyTotal  = buyPrice * volume;
        	double sellTotal = sellPrice * volume;
        	double net = sellTotal - buyTotal;
        	net = Utilities_T.round(net);
 	
        	// TODO: get EOD info
        	MarketData_T buyData  = databaseManager.getMarketData(holding.getSymbolId(), buyDate);
        	Date sellDate = timeManager.getCurrentTradeDate();
        	MarketData_T sellData = databaseManager.getMarketData(holding.getSymbolId(), sellDate);
        	
        	report.print(holding.getSymbol().getSymbol()+ "["+holding.getSymbolId()+"]");
        	report.print("\t"+holding.getBuyDate()+"\t$"+buyPrice+"\t$"+holding.getActualBuyPrice()+"\t"+holding.getVolume()+"\t");
        	report.println("\t$"+buyData.getLastPrice()+"\t$"+buyData.getAskPrice()+"/$"+buyData.getBidPrice()+"\t"+buyData.getVolume().longValue());
        	report.print("\t\t"+holding.getSellDate()+"\t$" +sellPrice+"\t$"+holding.getAvgFillPrice()+"\t"+holding.getVolume()+"\t"+holding.getRemaining());
        	report.println("\t$"+sellData.getLastPrice()+"\t$"+sellData.getAskPrice()+"/$"+sellData.getBidPrice()+"\t"+sellData.getVolume().longValue());
        	report.println("\t\t\t\t[net: $"+net+"]");
        	
        	cumNet += net;
        	cumVol += volume;
        	
        }  // next Holding
        
        double netLessCommision = cumNet - (cumVol * 0.01);
        report.println("\nTotal Net: $"+netLessCommision+" on "+cumVol+" shares ($"+cumNet+" less commission)");
    
        report.close();
    }
    
}