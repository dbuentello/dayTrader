package accounts;

public class Portfolio_T {

	public String m_symbol;
	public int    m_position;	// number of shares
	public double m_marketPrice;
	public double m_marketValue;
	public double m_avgCost;
	public double m_PNL;
	public double m_unrealizedPNL;
	
	public Portfolio_T() { }
	
	public Portfolio_T(String symId, int pos, double price, double val,
							double cost, double PNL, double unPNL)	{
		
		m_symbol = symId;
		m_position = pos;
		m_marketPrice = price;
		m_marketValue = val;
		m_avgCost = cost;
		m_PNL = PNL;
		m_unrealizedPNL = unPNL;
	}
	
}
