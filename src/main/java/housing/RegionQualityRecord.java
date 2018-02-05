package housing;

/**************************************************************************************************
 * Class to encapsulate information on a house that is for sale. It can be though of as the record
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

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Construct a new record containing the (exponential moving average) price for a given pair region-quality band
     *
     * @param region The region that is represented in the record
     * @param quality The quality band that is represented in the record
     * @param price The current exponential moving average of house sale prices for that region and that quality band
     */
    RegionQualityRecord(Region region, int quality, double price) {
        super(price);
        this.region = region;
        this.quality = quality;
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
