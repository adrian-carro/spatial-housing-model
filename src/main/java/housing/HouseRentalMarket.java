package housing;

import org.apache.commons.math3.random.MersenneTwister;

/**************************************************************************************************
 * Class to represent the rental market
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class HouseRentalMarket extends HousingMarket {

    //------------------//
    //----- Fields -----//
    //------------------//

	private Region      region;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

	public HouseRentalMarket(Config config, MersenneTwister rand, Region region) {
        super(config, rand, region);
		this.region = region;
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

    @Override
	public void completeTransaction(HouseBidderRecord purchase, HouseOfferRecord sale) {
        region.regionalRentalMarketStats.recordTransaction(sale);
		sale.getHouse().rentalRecord = null;
		RentalAgreement rentalAgreement = purchase.getBidder().completeHouseRental(sale);
		sale.getHouse().owner.completeHouseLet(sale, rentalAgreement);
		region.regionalRentalMarketStats.recordSale(purchase, sale);
	}

	@Override
	public HouseOfferRecord offer(House house, double price, boolean BTLOffer) {
		HouseOfferRecord hsr = super.offer(house, price, false);
		house.putForRent(hsr);
		return(hsr);
	}
	
	@Override
	public void removeOffer(HouseOfferRecord hsr) {
		super.removeOffer(hsr);
		hsr.getHouse().resetRentalRecord();
	}
}
