package housing;

import java.io.Serializable;

/**************************************************************************************************
 * Class to represent a house with all its intrinsic characteristics.
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class House implements Comparable<House>, Serializable {
    private static final long serialVersionUID = 4538336934216907799L;

    //------------------//
    //----- Fields -----//
    //------------------//

    private static int      id_pool = 0;

    public IHouseOwner  owner;
    public Household    resident;
    public Region       region;
    public int          id;

    HouseSaleRecord     saleRecord;
    HouseSaleRecord     rentalRecord;

    private int         quality;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Creates a house of quality quality in region region
     *
     * @param region Reference to the region where the house sits
     * @param quality Quality band characterizing the house
     */
	public House(Region region, int quality) {
		this.id = ++id_pool;
        this.owner = null;
        this.resident = null;
        this.region = region;
		this.quality = quality;
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

	public boolean isOnMarket() {
		return saleRecord != null;
	}

	public HouseSaleRecord getSaleRecord() {
		return saleRecord;
	}

	public HouseSaleRecord getRentalRecord() {
		return rentalRecord;
	}

	public boolean isOnRentalMarket() {
		return rentalRecord != null;
	}
    public void putForSale(HouseSaleRecord saleRecord) {
		this.saleRecord = saleRecord;
	}

	public void resetSaleRecord() {
		saleRecord = null;
	}
    public void putForRent(HouseSaleRecord rentalRecord) {
		this.rentalRecord = rentalRecord;
	}

	public void resetRentalRecord() {
		rentalRecord = null;
	}

	public int getQuality() {
		return quality;
	}

	@Override
	public int compareTo(House o) {
		return((int)Math.signum(id-o.id));
	}
	
}
