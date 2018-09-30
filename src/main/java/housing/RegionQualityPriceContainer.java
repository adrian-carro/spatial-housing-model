package housing;

/**************************************************************************************************
 * Container class to encapsulate a region, quality and price information so that a method from a
 * given class can return this container to a call from a different class
 *
 * @author Adrian Carro
 *
 *************************************************************************************************/
public class RegionQualityPriceContainer {

    //------------------//
    //----- Fields -----//
    //------------------//

    private Region      region;
    private int         quality;
    private double      expAvPrice;
    private double      desiredPrice;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    RegionQualityPriceContainer(Region region, int quality, double expAvPrice, double desiredPrice) {
        this.region = region;
        this.quality = quality;
        this.expAvPrice = expAvPrice;
        this.desiredPrice = desiredPrice;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Getter/setter methods -----//

    public Region getRegion() { return region; }

    public int getQuality() { return quality; }

    public double getExpAvPrice() { return expAvPrice; }

    public double getDesiredPrice() { return desiredPrice; }
}
