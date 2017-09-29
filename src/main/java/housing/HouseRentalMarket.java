package housing;

/**************************************************************************************************
 * Class to represent the rental market
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class HouseRentalMarket extends HousingMarket {
	private static final long serialVersionUID = -3039057421808432696L;

    //------------------//
    //----- Fields -----//
    //------------------//

	private Region      region;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

	public HouseRentalMarket(Region region) {
        super(region);
		this.region = region;
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

    @Override
	public void completeTransaction(HouseBuyerRecord purchase, HouseSaleRecord sale) {
        region.regionalRentalMarketStats.recordTransaction(sale);
		sale.house.rentalRecord = null;
		purchase.buyer.completeHouseRental(sale);
		sale.house.owner.completeHouseLet(sale);
		region.regionalRentalMarketStats.recordSale(purchase, sale);
	}

	@Override
	public HouseSaleRecord offer(House house, double price) {
		if(house.isOnMarket()) {
			System.out.println("Got offer on rental market of house already on sale market");			
		}
		HouseSaleRecord hsr = super.offer(house, price);
		house.putForRent(hsr);
		return(hsr);
	}
	
	@Override
	public void removeOffer(HouseSaleRecord hsr) {
		super.removeOffer(hsr);
		hsr.house.resetRentalRecord();
	}
}
