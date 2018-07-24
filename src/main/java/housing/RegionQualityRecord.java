package housing;

/**************************************************************************************************
 * Class to encapsulate information on a house that is for sale. It can be thought of as the record
 * a estate agent would keep about each of the properties managed
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class RegionQualityRecord extends HousingMarketRecord {

    //------------------//
    //----- Fields -----//
    //------------------//

    private Region      region;
    private int         quality;
    private double      commutingCost;
    private boolean     saleOrRental;
    private double      FSale;
    private double      FRent;
    
    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Construct a new record containing the (exponential moving average) price for a given pair region-quality band
     *
     * @param region The region that is represented in the record
     * @param quality The quality band that is represented in the record
     * @param price The current exponential moving average of house sale prices for that region and that quality band
     * @param commutingCost The commuting cost between a region and a destination
     * @param saleOrRental True for sales market, false for rental market.
     */
    RegionQualityRecord(Config config, Region region, int quality, double price, double commutingCost, boolean saleOrRental) {
        super(price);
        this.region = region;
        this.quality = quality;
        this.commutingCost = commutingCost;
        this.saleOrRental = saleOrRental;
        if(saleOrRental) {
        		FSale = Math.pow(quality, config.A_IN_F)/price;
        } else {
        		FRent = Math.pow(quality, config.A_IN_F)/(config.HOLD_PERIOD*config.constants.MONTHS_IN_YEAR*price 
        				+ config.HOLD_PERIOD*config.constants.MONTHS_IN_YEAR*commutingCost + config.B_IN_F); 
    //TODO: probably add some psychological cost
        }        
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Getter/setter methods -----//

    /**
     * Quality band of this record
     */
    @Override
    public int getQuality() { return quality; }

    /**
     * Region of this property
     */
    public Region getRegion() { return region;}
    
    /**
     * commuting cost to the relative region
     */
    public double getCommutingCost(){return commutingCost;}
    
    /**
     *  For sale or rent
     */
    public boolean getSaleOrRent() { return saleOrRental;}
    
    
    public double getFSale() { return FSale;}
    public double getFRent() { return FRent;}
//    /**
//     * Dummy method as this needs to be implemented
//     */
//    @Override
//    public double getYield() {
//        System.out.println("Strange: The program shouldn't have entered here!");
//        return 0.0;
//    }

    /**
     * Set the (exponential moving average) price for this region and quality band
     *
     * @param newPrice The new (exponential moving average) price for this region and quality band
     * @param auth Authority to change the price
     */
    public void setPrice(double newPrice, HousingMarket.Authority auth) {
        super.setPrice(newPrice, auth);
    }
}
