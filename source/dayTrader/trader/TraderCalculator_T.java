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

import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.TimeManager_T;
import managers.MarketDataManager_T;
import marketdata.MarketData_T;
import marketdata.Symbol_T;

// for simulation only
import trader.Holding_T;


public class TraderCalculator_T {
  /* {src_lang=Java}*/

    /** References to the Managers we need */
    private DatabaseManager_T databaseManager;
    private MarketDataManager_T marketDataManager;
    private TimeManager_T timeManager;
    
    
    /**
     * 
     */
    public TraderCalculator_T() {
	    databaseManager   = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    timeManager       = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
	    //logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
    }

    /**
     * update our Holdings db with buy position of the the stocks we want to buy today
 	 * buy price, #of share, total $ amt
	 * calculate how much of each stock to buy
	 * volume is declared as INT so only full shares are bought
	 * TOD0: also create the sell criteria (sell price, %incr/decr, etc).  This will be an XML field.
	 * input is symbol list of biggest losers
     */
    public synchronized void updateBuyPositions(List<Symbol_T> losers)
    {
    	// how much $$ do we have to work with?
    	// use equal dollar amount for each holding (rounded to full shares)
    	double totalCapital = getCapital();
    	double buyTotal = totalCapital/Trader_T.MAX_BUY_POSITIONS;

        Iterator<Symbol_T> it = losers.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
      
            //double buyPrice = 100.0 * (xx++ * .05);					// getEODPrice(Symbol)
            double buyPrice = marketDataManager.getEODPrice(symbol);
            int buyVolume = (int)(buyTotal/buyPrice);
            double adjustedBuyTotal = buyVolume * buyPrice;
    	
            Session session = databaseManager.getSessionFactory().openSession();

            // updates must be within a transaction
            Transaction tx = null;
        
            try {
            	tx = session.beginTransaction();

            	//SALxx-- where is buy price?  using avgFillPrice for now
            	// use lastFillPrice for total for now
            	String hql = "UPDATE trader.Holding_T " +
            			//"SET avgFillPrice = :buyPrice, remaining = :buyVolume, lastFillPrice = :buyTotal " +
            			"SET avgFillPrice = :buyPrice, remaining = :buyVolume " +
            			"WHERE symbolId = :sym AND buyDate >= :date";
            	Query query = session.createQuery(hql);
            	query.setDouble("buyPrice", buyPrice);
            	query.setInteger("buyVolume", buyVolume);    
            	//query.setDouble("buyTotal", adjustedBuyTotal);
            	query.setParameter("sym", symbol.getId());
            	query.setDate("date", timeManager.Today());

            	int n = query.executeUpdate();
                 
            	tx.commit();
            } catch (HibernateException e) {
            	//TODO: for now just print to stdout, we'll change this to a log file later
            	e.printStackTrace();
            	if (tx != null) tx.rollback();
            } finally {
            	session.close();
            }
            
        }
        
    }

    /**
     * simulate what brokerManager.liquidateHoldings will do, but without
     * executing the orders.  Nathan, you can copy/use this logic.
     * 
     * Sell all remaining holdings at the end of the day and record in our
     * Holdings DB
     * 
     * The logic is based on the fact that when a holding is sold, the sell date
     * will be entered into the DB.  Any holding without a sell date needs to be sold
     */
    public void simulateLiquidateHoldings()
    {
    	//"SELECT symbolId, buy_price FROM $tableName where sell_date is NULL";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.isNull("sellDate"));


        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        if (holdingData.size() == 0)
        {
        	System.out.println("There are no remaining holdings at the end of the day");
        	return;
        }
                
       System.out.println("There are "+holdingData.size()+" remaining Holdings");
            
       Iterator<Holding_T> it = holdingData.iterator();
       while (it.hasNext()) {
           Holding_T holding = it.next();

           Symbol_T symbol = holding.getSymbol();
           double buyPrice = holding.getAvgFillPrice();		//SAL the price we bought at

           double price  = marketDataManager.getEODPrice(symbol);	// the price now
           Date sellDate = timeManager.TimeNow();
           
           holding.setSellDate(sellDate);
           holding.setExecSellPriceLow(price);	// SAL - why is there a high and low?
           
           System.out.print(" "+symbol.getSymbol());

           // updateSellPositions(Symbol, price, lastTradeDate)

           // $stmt = "UPDATE $tableName SET sell_price = $price, sell_date= \"$date\" WHERE symbol = \"$symbol\" AND sell_date is NULL";
           //holding.update(); SAL why doesnt this worK? is this Holding object different than if retrieved from db? it shouldnt be
           
           // this does....
           Session session2 = databaseManager.getSessionFactory().openSession();

           // updates must be within a transaction
           Transaction tx = null;
       
           try {
           	tx = session2.beginTransaction();

           	//SALxx-- where is buy price?  using avgFillPrice for now
           	// use lastFillPrice for total for now
           	String hql = "UPDATE trader.Holding_T " +
           			"SET execSellPriceLow = :price, sellDate = :date " +
           			"WHERE symbolId = :sym AND sellDate is null";
           	Query query = session2.createQuery(hql);
           	query.setDouble("price", price);
           	query.setDate("date", timeManager.TimeNow());
           	query.setParameter("sym", symbol.getId());


           	int n = query.executeUpdate();
                
           	tx.commit();
           } catch (HibernateException e) {
           	//TODO: for now just print to stdout, we'll change this to a log file later
           	e.printStackTrace();
           	if (tx != null) tx.rollback();
           } finally {
           	session2.close();
           }           
           
       }

    }
    
    public void calcSellPrice() {
    }
    
    public void calcNumSharesToBuy() {
    }
    
    public void calcDollarBuyAmount() {
    }

    /**
     * Get the $ amount of available capital
     * 
     * @return (double) the amount of capital we can invest
     */
    private double getCapital()
    {
    	//SELECT price from DailyNet ORDER BY date DESC LIMIT 1";
    	
        return 10000.00;   
  
    }
}