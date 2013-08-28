package com.ib.client;

import java.util.Vector;

public class Order {
  /* {src_lang=Java}*/


  public static final int CUSTOMER = 0;

  public static final int FIRM = 1;

  public static final char OPT_UNKNOWN = '?';

  public static final char OPT_BROKER_DEALER = 'b';

  public static final char OPT_CUSTOMER = 'c';

  public static final char OPT_FIRM = 'f';

  public static final char OPT_ISEMM = 'm';

  public static final char OPT_FARMM = 'n';

  public static final char OPT_SPECIALIST = 'y';

  public static final int AUCTION_MATCH = 1;

  public static final int AUCTION_IMPROVEMENT = 2;

  public static final int AUCTION_TRANSPARENT = 3;

  public static final String EMPTY_STR = "";

  public int m_orderId;

  public int m_clientId;

  public int m_permId;

  public String m_action;

  public int m_totalQuantity;

  public String m_orderType;

  public double m_lmtPrice;

  public double m_auxPrice;

  public String m_tif;

  public String m_ocaGroup;

  public int m_ocaType;

  public String m_orderRef;

  public boolean m_transmit;

  public int m_parentId;

  public boolean m_blockOrder;

  public boolean m_sweepToFill;

  public int m_displaySize;

  public int m_triggerMethod;

  public boolean m_outsideRth;

  public boolean m_hidden;

  public String m_goodAfterTime;

  public String m_goodTillDate;

  public boolean m_overridePercentageConstraints;

  public String m_rule80A;

  public boolean m_allOrNone;

  public int m_minQty;

  public double m_percentOffset;

  public double m_trailStopPrice;

  public double m_trailingPercent;

  public String m_faGroup;

  public String m_faProfile;

  public String m_faMethod;

  public String m_faPercentage;

  public String m_openClose;

  public int m_origin;

  public int m_shortSaleSlot;

  public String m_designatedLocation;

  public int m_exemptCode;

  public double m_discretionaryAmt;

  public boolean m_eTradeOnly;

  public boolean m_firmQuoteOnly;

  public double m_nbboPriceCap;

  public boolean m_optOutSmartRouting;

  public int m_auctionStrategy;

  public double m_startingPrice;

  public double m_stockRefPrice;

  public double m_delta;

  public double m_stockRangeLower;

  public double m_stockRangeUpper;

  public double m_volatility;

  public int m_volatilityType;

  public int m_continuousUpdate;

  public int m_referencePriceType;

  public String m_deltaNeutralOrderType;

  public double m_deltaNeutralAuxPrice;

  public int m_deltaNeutralConId;

  public String m_deltaNeutralSettlingFirm;

  public String m_deltaNeutralClearingAccount;

  public String m_deltaNeutralClearingIntent;

  public double m_basisPoints;

  public int m_basisPointsType;

  public int m_scaleInitLevelSize;

  public int m_scaleSubsLevelSize;

  public double m_scalePriceIncrement;

  public double m_scalePriceAdjustValue;

  public int m_scalePriceAdjustInterval;

  public double m_scaleProfitOffset;

  public boolean m_scaleAutoReset;

  public int m_scaleInitPosition;

  public int m_scaleInitFillQty;

  public boolean m_scaleRandomPercent;

  public String m_hedgeType;

  public String m_hedgeParam;

  public String m_account;

  public String m_settlingFirm;

  public String m_clearingAccount;

  public String m_clearingIntent;

  public String m_algoStrategy;

  public Vector m_algoParams;

  public boolean m_whatIf;

  public boolean m_notHeld;

  public Vector m_smartComboRoutingParams;

  public Vector m_orderComboLegs = new Vector<OrderComboLeg>();

  public Order() {
        m_lmtPrice = Double.MAX_VALUE;
        m_auxPrice = Double.MAX_VALUE;
    	m_outsideRth = false;
        m_openClose	= "O";
        m_origin = CUSTOMER;
        m_transmit = true;
        m_designatedLocation = EMPTY_STR;
        m_exemptCode = -1;
        m_minQty = Integer.MAX_VALUE;
        m_percentOffset = Double.MAX_VALUE;
        m_nbboPriceCap = Double.MAX_VALUE;
        m_optOutSmartRouting = false;
        m_startingPrice = Double.MAX_VALUE;
        m_stockRefPrice = Double.MAX_VALUE;
        m_delta = Double.MAX_VALUE;
        m_stockRangeLower = Double.MAX_VALUE;
        m_stockRangeUpper = Double.MAX_VALUE;
        m_volatility = Double.MAX_VALUE;
        m_volatilityType = Integer.MAX_VALUE;
        m_deltaNeutralOrderType = EMPTY_STR;
        m_deltaNeutralAuxPrice = Double.MAX_VALUE;
        m_deltaNeutralConId = 0;
        m_deltaNeutralSettlingFirm = EMPTY_STR;
        m_deltaNeutralClearingAccount = EMPTY_STR;
        m_deltaNeutralClearingIntent = EMPTY_STR;
        m_referencePriceType = Integer.MAX_VALUE;
        m_trailStopPrice = Double.MAX_VALUE;
        m_trailingPercent = Double.MAX_VALUE;
        m_basisPoints = Double.MAX_VALUE;
        m_basisPointsType = Integer.MAX_VALUE;
        m_scaleInitLevelSize = Integer.MAX_VALUE;
        m_scaleSubsLevelSize = Integer.MAX_VALUE;
        m_scalePriceIncrement = Double.MAX_VALUE;
        m_scalePriceAdjustValue = Double.MAX_VALUE;
        m_scalePriceAdjustInterval = Integer.MAX_VALUE;
        m_scaleProfitOffset = Double.MAX_VALUE;
        m_scaleAutoReset = false;
        m_scaleInitPosition = Integer.MAX_VALUE;
        m_scaleInitFillQty = Integer.MAX_VALUE;
        m_scaleRandomPercent = false;
        m_whatIf = false;
        m_notHeld = false;
  }

  public boolean equals(Object p_other) {
        if ( this == p_other )
            return true;
        if ( p_other == null )
            return false;
        Order l_theOther = (Order)p_other;
        if ( m_permId == l_theOther.m_permId ) {
            return true;
        }
        if (m_orderId != l_theOther.m_orderId ||
        	m_clientId != l_theOther.m_clientId ||
        	m_totalQuantity != l_theOther.m_totalQuantity ||
        	m_lmtPrice != l_theOther.m_lmtPrice ||
        	m_auxPrice != l_theOther.m_auxPrice ||
        	m_ocaType != l_theOther.m_ocaType ||
        	m_transmit != l_theOther.m_transmit ||
        	m_parentId != l_theOther.m_parentId ||
        	m_blockOrder != l_theOther.m_blockOrder ||
        	m_sweepToFill != l_theOther.m_sweepToFill ||
        	m_displaySize != l_theOther.m_displaySize ||
        	m_triggerMethod != l_theOther.m_triggerMethod ||
        	m_outsideRth != l_theOther.m_outsideRth ||
        	m_hidden != l_theOther.m_hidden ||
        	m_overridePercentageConstraints != l_theOther.m_overridePercentageConstraints ||
        	m_allOrNone != l_theOther.m_allOrNone ||
        	m_minQty != l_theOther.m_minQty ||
        	m_percentOffset != l_theOther.m_percentOffset ||
        	m_trailStopPrice != l_theOther.m_trailStopPrice ||
        	m_trailingPercent != l_theOther.m_trailingPercent ||
        	m_origin != l_theOther.m_origin ||
        	m_shortSaleSlot != l_theOther.m_shortSaleSlot ||
        	m_discretionaryAmt != l_theOther.m_discretionaryAmt ||
        	m_eTradeOnly != l_theOther.m_eTradeOnly ||
        	m_firmQuoteOnly != l_theOther.m_firmQuoteOnly ||
        	m_nbboPriceCap != l_theOther.m_nbboPriceCap ||
        	m_optOutSmartRouting != l_theOther.m_optOutSmartRouting ||
        	m_auctionStrategy != l_theOther.m_auctionStrategy ||
        	m_startingPrice != l_theOther.m_startingPrice ||
        	m_stockRefPrice != l_theOther.m_stockRefPrice ||
        	m_delta != l_theOther.m_delta ||
        	m_stockRangeLower != l_theOther.m_stockRangeLower ||
        	m_stockRangeUpper != l_theOther.m_stockRangeUpper ||
        	m_volatility != l_theOther.m_volatility ||
        	m_volatilityType != l_theOther.m_volatilityType ||
        	m_continuousUpdate != l_theOther.m_continuousUpdate ||
        	m_referencePriceType != l_theOther.m_referencePriceType ||
        	m_deltaNeutralAuxPrice != l_theOther.m_deltaNeutralAuxPrice ||
        	m_deltaNeutralConId != l_theOther.m_deltaNeutralConId ||
        	m_basisPoints != l_theOther.m_basisPoints ||
        	m_basisPointsType != l_theOther.m_basisPointsType ||
        	m_scaleInitLevelSize != l_theOther.m_scaleInitLevelSize ||
        	m_scaleSubsLevelSize != l_theOther.m_scaleSubsLevelSize ||
        	m_scalePriceIncrement != l_theOther.m_scalePriceIncrement ||
        	m_scalePriceAdjustValue != l_theOther.m_scalePriceAdjustValue ||
        	m_scalePriceAdjustInterval != l_theOther.m_scalePriceAdjustInterval ||
        	m_scaleProfitOffset != l_theOther.m_scaleProfitOffset ||
        	m_scaleAutoReset != l_theOther.m_scaleAutoReset ||
        	m_scaleInitPosition != l_theOther.m_scaleInitPosition ||
        	m_scaleInitFillQty != l_theOther.m_scaleInitFillQty ||
        	m_scaleRandomPercent != l_theOther.m_scaleRandomPercent ||
        	m_whatIf != l_theOther.m_whatIf ||
        	m_notHeld != l_theOther.m_notHeld ||
        	m_exemptCode != l_theOther.m_exemptCode) {
        	return false;
        }
        if (Util.StringCompare(m_action, l_theOther.m_action) != 0 ||
        	Util.StringCompare(m_orderType, l_theOther.m_orderType) != 0 ||
        	Util.StringCompare(m_tif, l_theOther.m_tif) != 0 ||
        	Util.StringCompare(m_ocaGroup, l_theOther.m_ocaGroup) != 0 ||
        	Util.StringCompare(m_orderRef,l_theOther.m_orderRef) != 0 ||
        	Util.StringCompare(m_goodAfterTime, l_theOther.m_goodAfterTime) != 0 ||
        	Util.StringCompare(m_goodTillDate, l_theOther.m_goodTillDate) != 0 ||
        	Util.StringCompare(m_rule80A, l_theOther.m_rule80A) != 0 ||
        	Util.StringCompare(m_faGroup, l_theOther.m_faGroup) != 0 ||
        	Util.StringCompare(m_faProfile, l_theOther.m_faProfile) != 0 ||
        	Util.StringCompare(m_faMethod, l_theOther.m_faMethod) != 0 ||
        	Util.StringCompare(m_faPercentage, l_theOther.m_faPercentage) != 0 ||
        	Util.StringCompare(m_openClose, l_theOther.m_openClose) != 0 ||
        	Util.StringCompare(m_designatedLocation, l_theOther.m_designatedLocation) != 0 ||
        	Util.StringCompare(m_deltaNeutralOrderType, l_theOther.m_deltaNeutralOrderType) != 0 ||
        	Util.StringCompare(m_deltaNeutralSettlingFirm, l_theOther.m_deltaNeutralSettlingFirm) != 0 ||
        	Util.StringCompare(m_deltaNeutralClearingAccount, l_theOther.m_deltaNeutralClearingAccount) != 0 ||
        	Util.StringCompare(m_deltaNeutralClearingIntent, l_theOther.m_deltaNeutralClearingIntent) != 0 ||
        	Util.StringCompare(m_hedgeType, l_theOther.m_hedgeType) != 0 ||
        	Util.StringCompare(m_hedgeParam, l_theOther.m_hedgeParam) != 0 ||
        	Util.StringCompare(m_account, l_theOther.m_account) != 0 ||
        	Util.StringCompare(m_settlingFirm, l_theOther.m_settlingFirm) != 0 ||
        	Util.StringCompare(m_clearingAccount, l_theOther.m_clearingAccount) != 0 ||
        	Util.StringCompare(m_clearingIntent, l_theOther.m_clearingIntent) != 0 ||
        	Util.StringCompare(m_algoStrategy, l_theOther.m_algoStrategy) != 0) {
        	return false;
        }
        if (!Util.VectorEqualsUnordered(m_algoParams, l_theOther.m_algoParams)) {
        	return false;
        }
        if (!Util.VectorEqualsUnordered(m_smartComboRoutingParams, l_theOther.m_smartComboRoutingParams)) {
        	return false;
        }
    	// compare order combo legs
        if (!Util.VectorEqualsUnordered(m_orderComboLegs, l_theOther.m_orderComboLegs)) {
        	return false;
        }
        
        return true;
  }

}