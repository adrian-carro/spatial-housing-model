package housing;

import java.util.Iterator;

import utilities.PriorityQueue2D;

/*******************************************************
 * Class that represents market for houses for-sale.
 * 
 * @author daniel
 *
 *****************************************************/
public class HouseSaleMarket extends HousingMarket {
	private static final long serialVersionUID = -2878118108039744432L;

	private Config                                  config = Model.config; // Passes the Model's configuration parameters object to a private field
	private Region                                  region;
    private PriorityQueue2D<HousingMarketRecord>    offersPY;

	public HouseSaleMarket(Region region) {
	    super(region);
	    this.region = region;
		offersPY = new PriorityQueue2D<>(new HousingMarketRecord.PYComparator());
	}
	
	@Override
	public void init() {
		super.init();
		if(offersPY != null) offersPY.clear();
	}
		
	/**
	 * This method deals with doing all the stuff necessary whenever a house gets sold.
	 */
	public void completeTransaction(HouseBuyerRecord purchase, HouseSaleRecord sale) {
	    // TODO: Revise if it makes sense to have recordTransaction as a separate method from recordSale
		region.regionalHousingMarketStats.recordTransaction(sale);
		sale.house.saleRecord = null;
		Household buyer = purchase.buyer;
		if(buyer == sale.house.owner) return;
		sale.house.owner.completeHouseSale(sale);
		buyer.completeHousePurchase(sale);
        region.regionalHousingMarketStats.recordSale(purchase, sale);
		sale.house.owner = buyer;
	}

	@Override
	public HouseSaleRecord offer(House house, double price) {
		HouseSaleRecord hsr = super.offer(house, price);
		offersPY.add(hsr);
		house.putForSale(hsr);
		return(hsr);
	}
	
	@Override
	public void removeOffer(HouseSaleRecord hsr) {
		super.removeOffer(hsr);
		offersPY.remove(hsr);
		hsr.house.resetSaleRecord();
	}
	
	@Override
	public void updateOffer(HouseSaleRecord hsr, double newPrice) {
		offersPY.remove(hsr);
		super.updateOffer(hsr, newPrice);
		offersPY.add(hsr);
	}
	
	@Override
	protected HouseSaleRecord getBestOffer(HouseBuyerRecord bid) {
		if(bid.getClass() == BTLBuyerRecord.class) { // BTL buyer (yield driven)
			HouseSaleRecord bestOffer = (HouseSaleRecord)offersPY.peek(bid);
			if(bestOffer != null) {
					double minDownpayment = bestOffer.getPrice()*(1.0
                            - region.regionalRentalMarketStats.getExpAvFlowYield()/
                            (Model.bank.interestCoverageRatio()*config.CENTRAL_BANK_BTL_STRESSED_INTEREST));
					if(bid.buyer.getBankBalance() >= minDownpayment) {
						return(bestOffer);
					}
			}
			return(null);
		} else { // must be OO buyer (quality driven)
			return super.getBestOffer(bid);
		}
	}
	
	public Iterator<HousingMarketRecord> offersIterator() {
		final PriorityQueue2D<HousingMarketRecord>.Iter underlyingIterator
				= (PriorityQueue2D<HousingMarketRecord>.Iter)super.getOffersIterator();
		return(new Iterator<HousingMarketRecord>() {
			@Override
			public boolean hasNext() {
				return underlyingIterator.hasNext();
			}
			@Override
			public HousingMarketRecord next() {
				return underlyingIterator.next();
			}
			@Override
			public void remove() {
				underlyingIterator.remove();
				if(underlyingIterator.last != null) HouseSaleMarket.this.offersPY.remove(underlyingIterator.last);
			}
		});
	}

	/*******************************************
	 * Make a bid on the market as a Buy-to-let investor
	 *  (i.e. make an offer on a (yet to be decided) house).
	 * 
	 * @param buyer The household that is making the bid.
	 * @param maxPrice The maximum price that the household is willing to pay.
	 ******************************************/
	public void BTLbid(Household buyer, double maxPrice) {
		bids.add(new BTLBuyerRecord(buyer, maxPrice)); }
}
