package housing;

import java.io.Serializable;
import utilities.PriorityQueue2D;

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
    private double      F;
    
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
        		F = Math.pow(quality, config.A_IN_F)/price;
        } else {
        		F = Math.pow(quality, config.A_IN_F)/(config.constants.MONTHS_IN_YEAR*price 
        				+ config.constants.MONTHS_IN_YEAR*commutingCost); 
    //TODO: probably add some psychological cost
        }        
    }

    //----------------------//
    //----- Subclasses -----//
    //----------------------//
    
    /**
     * Class that implements the comparators needed for inserting RegionQualityRecord objects into PriorityQueue2D. In
     * particular, this class implements the comparators for a price-F priority queue.
     */
    public static class PFComparator implements PriorityQueue2D.XYComparator<RegionQualityRecord>, Serializable {
        private static final long serialVersionUID = 6225466622291609603L;

        /**
         * @return -1 or 1 if arg0 is, respectively, cheaper than or more expensive than arg1 solving the arg0 == arg1
         * case by reverse comparing their F and comparing their Id's if they also have the same F
         */
        @Override
        public int XYCompare(RegionQualityRecord arg0, RegionQualityRecord arg1) {
            double diff = arg0.getPrice() - arg1.getPrice();
            if (diff == 0.0) {
                diff = ((RegionQualityRecord) arg1).getF() - ((RegionQualityRecord) arg0).getF(); // Note the reverse ordering here
                if (diff == 0.0) {
                    diff = arg0.getId() - arg1.getId();
                }
            }
            return (int) Math.signum(diff);
        }

        /**
         * @return -1, 0 or 1 if arg0 has, respectively, less P than, equal P as, or greater P than
         * arg1
         */
        @Override
        public int XCompare(RegionQualityRecord arg0, RegionQualityRecord arg1) {
            return Integer.signum((int) (((RegionQualityRecord) arg0).getPrice() - ((RegionQualityRecord) arg1).getPrice()));
        }

        /**
         * @return -1, 0 or 1 if arg0 has, respectively, less F than, equal F as, or greater F than
         * arg1
         */
        @Override
        public int YCompare(RegionQualityRecord arg0, RegionQualityRecord arg1) {
            return Integer.signum((int) (((RegionQualityRecord) arg0).getF() - ((RegionQualityRecord) arg1).getF()));
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
       
    public double getF() { return F;}
    
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
