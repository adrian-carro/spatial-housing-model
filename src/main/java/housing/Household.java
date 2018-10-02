package housing;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.random.MersenneTwister;

/**************************************************************************************************
 * This represents a household who receives an income, consumes, saves and can buy, sell, let, and
 * invest in houses.
 *
 * @author daniel, davidrpugh, Adrian Carro
 *
 *************************************************************************************************/

public class Household implements IHouseOwner, Serializable {

    //------------------//
    //----- Fields -----//
    //------------------//

    private static int          id_pool;

    public int                  id; // Only used for identifying households within the class MicroDataRecorder
    public HouseholdBehaviour   behaviour; // Behavioural plugin

    double                      incomePercentile; // Fixed for the whole lifetime of the household

    private Geography                       geography;
    private Region                          jobRegion;
    private House                           home;
    private Map<House, PaymentAgreement>    housePayments = new TreeMap<>(); // Houses owned and their payment agreements
    private Config	                        config; // Private field to receive the Model's configuration parameters object
    private MersenneTwister                 rand; // Private field to receive the Model's random number generator
    private double                          age; // Age of the household representative person
    private double                          bankBalance;
    private double                          annualGrossEmploymentIncome;
    private double                          monthlyGrossEmploymentIncome;
    private double                          monthlyGrossRentalIncome; // Keeps track of monthly rental income, as only tenants keep a reference to the rental contract, not landlords
    private boolean                         isFirstTimeBuyer;
    private boolean                         isBankrupt;
    
    //------------------------//
    //----- Constructors -----//
    //------------------------//

    /**
     * Initialises behaviour (determine whether the household will be a BTL investor). Households start off in social
     * housing and with their "desired bank balance" in the bank
     */
    public Household(Config config, MersenneTwister rand, double householdAgeAtBirth, Geography geography,
                     Region jobRegion) {
        this.config = config;
        this.rand = rand;
        this.geography = geography;
        this.jobRegion = jobRegion;
        home = null;
        isFirstTimeBuyer = true;
        isBankrupt = false;
        id = ++id_pool;
        age = householdAgeAtBirth;
        incomePercentile = this.rand.nextDouble();
        behaviour = new HouseholdBehaviour(this.config, this.rand, incomePercentile);
        // Find initial values for the annual and monthly gross employment income
        annualGrossEmploymentIncome = data.EmploymentIncome.getAnnualGrossEmploymentIncome(age, incomePercentile);
        monthlyGrossEmploymentIncome = annualGrossEmploymentIncome/config.constants.MONTHS_IN_YEAR;
        bankBalance = behaviour.getDesiredBankBalance(getAnnualGrossTotalIncome()); // Desired bank balance is used as initial value for actual bank balance
        monthlyGrossRentalIncome = 0.0;
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- General methods -----//

    /**
     * Main simulation step for each household. They age, receive employment and other forms of income, make their rent
     * or mortgage payments, perform an essential consumption, make non-essential consumption decisions, manage their
     * owned properties, and make their housing decisions depending on their current housing state:
     * - Buy or rent if in social housing
     * - Sell house if owner-occupier
     * - Buy/sell/rent out properties if BTL investor
     */
    public void step() {
        isBankrupt = false; // Delete bankruptcies from previous time step
        age += 1.0/config.constants.MONTHS_IN_YEAR;
        // Update annual and monthly gross employment income
        annualGrossEmploymentIncome = data.EmploymentIncome.getAnnualGrossEmploymentIncome(age, incomePercentile);
        monthlyGrossEmploymentIncome = annualGrossEmploymentIncome/config.constants.MONTHS_IN_YEAR;
        // Add monthly disposable income (net total income minus essential consumption and housing expenses) to bank balance
        bankBalance += getMonthlyDisposableIncome();
        // Consume based on monthly disposable income (after essential consumption and house payments have been subtracted)
        bankBalance -= behaviour.getDesiredConsumption(getBankBalance(), getAnnualGrossTotalIncome()); // Old implementation: if(isFirstTimeBuyer() || !isInSocialHousing()) bankBalance -= behaviour.getDesiredConsumption(getBankBalance(), getAnnualGrossTotalIncome());
        // Deal with bankruptcies
        // TODO: Improve bankruptcy procedures (currently, simple cash injection), such as terminating contracts!
        if (bankBalance < 0.0) {
            bankBalance = 1.0;
            isBankrupt = true;
        }
        // TODO: Repair this! Important to keep the payoff function so as to remove the loan from the bank
        // Manage all owned properties
        for (House h: housePayments.keySet()) {
            if (h.owner == this) {
                manageHouse(h);
//            } else if (h.resident != this && housePayments.get(h).nPayments == 0) {
//                // Second, find mortgage object and pay off as much outstanding debt as possible given bank balance
//                MortgageAgreement mortgage = mortgageFor(sale.house);
//                bankBalance -= mortgage.payoff(bankBalance);
//                // Third, if there is no more outstanding debt, remove the house from the household's housePayments object
//                if (mortgage.nPayments == 0) {
//                    housePayments.remove(sale.house);
//                }
            } else if (h.resident != this) {
                System.out.println("House is in my payments, but I don't own it nor do I live in it");
            }
        }
        // Make housing decisions depending on current housing state
        if (isInSocialHousing()) {
            bidForAHome(); // When BTL households are born, they enter here the first time and until they manage to buy a home!
        } else if (isRenting()) {
            if (housePayments.get(home).nPayments == 0) { // End of rental period for this tenant
                endTenancy();
                bidForAHome();
            }
        } else if (behaviour.isPropertyInvestor()) { // Only BTL investors who already own a home enter here
            double price = behaviour.btlPurchaseBid(this, jobRegion);
            // TODO: Replace this jobRegion by the one the investor decides to invest in
            // TODO: Note this is counting all BTL investors as bids, regardless of decideToBuyInvestmentProperty
            jobRegion.regionalHouseholdStats.countBTLBidsAboveExpAvSalePrice(price);
            if (behaviour.decideToBuyInvestmentProperty(this, jobRegion)) {
                // TODO: ATTENTION ---> For now, investor households bid always in the region where they work!
                // TODO: A separate method for quickly disqualifying investors who can't afford investing? How to choose
                // TODO: between regions in unbiased manner?
                jobRegion.houseSaleMarket.BTLbid(this, price);
            }
        } else if (!isHomeowner()){
            System.out.println("Strange: this household is not a type I recognize");
        }
    }

    /**
     * Subtracts the essential, necessary consumption and housing expenses (mortgage and rental payments) from the net
     * total income (employment income, property income, financial returns minus taxes)
     */
    private double getMonthlyDisposableIncome() {
        // Start with net monthly income
        double monthlyDisposableIncome = getMonthlyNetTotalIncome();
        // Subtract essential, necessary consumption
        // TODO: ESSENTIAL_CONSUMPTION_FRACTION is not explained in the paper, all support is said to be consumed
        monthlyDisposableIncome -= config.ESSENTIAL_CONSUMPTION_FRACTION*config.GOVERNMENT_MONTHLY_INCOME_SUPPORT;
        // Subtract housing consumption
        for(PaymentAgreement payment: housePayments.values()) {
            monthlyDisposableIncome -= payment.makeMonthlyPayment();
        }
        return monthlyDisposableIncome;
    }

    /**
     * Subtracts the monthly aliquot part of all due taxes from the monthly gross total income. Note that only income
     * tax on employment income and national insurance contributions are implemented!
     */
    double getMonthlyNetTotalIncome() {
        return getMonthlyGrossTotalIncome()
                - (Model.government.incomeTaxDue(annualGrossEmploymentIncome)   // Employment income tax
                + Model.government.class1NICsDue(annualGrossEmploymentIncome))  // National insurance contributions
                /config.constants.MONTHS_IN_YEAR;
    }

    /**
     * Adds up all sources of (gross) income on a monthly basis: employment, property, returns on financial wealth
     */
    public double getMonthlyGrossTotalIncome() {
        return monthlyGrossEmploymentIncome + monthlyGrossRentalIncome + bankBalance*config.RETURN_ON_FINANCIAL_WEALTH;
    }

    double getAnnualGrossTotalIncome() { return getMonthlyGrossTotalIncome()*config.constants.MONTHS_IN_YEAR; }

    //----- Methods for house owners -----//

    /**
     * Decide what to do with a house h owned by the household:
     * - if the household lives in the house, decide whether to sell it or not
     * - if the house is up for sale, rethink its offer price, and possibly put it up for rent instead (only BTL investors)
     * - if the house is up for rent, rethink the rent demanded
     *
     * @param house A house owned by the household
     */
    private void manageHouse(House house) {
        HouseOfferRecord forSale, forRent;
        double newPrice;
        
        forSale = house.getSaleRecord();
        if(forSale != null) { // reprice house for sale
            newPrice = behaviour.rethinkHouseSalePrice(forSale);
            if(newPrice > mortgageFor(house).principal) {
                house.region.houseSaleMarket.updateOffer(forSale, newPrice);
            } else {
                house.region.houseSaleMarket.removeOffer(forSale);
                // TODO: Is first condition redundant?
                if(house != home && house.resident == null) {
                    house.region.houseRentalMarket.offer(house, buyToLetRent(house), false);
                }
            }
        } else if(decideToSellHouse(house)) { // put house on market?
            if(house.isOnRentalMarket()) house.region.houseRentalMarket.removeOffer(house.getRentalRecord());
            putHouseForSale(house);
        }
        
        forRent = house.getRentalRecord();
        if(forRent != null) { // reprice house for rent
            newPrice = behaviour.rethinkBuyToLetRent(forRent);
            house.region.houseRentalMarket.updateOffer(forRent, newPrice);
        }        
    }

    /******************************************************
     * Having decided to sell house h, decide its initial sale price and put it up in the market.
     *
     * @param h the house being sold
     ******************************************************/
    private void putHouseForSale(House h) {
        double principal;
        MortgageAgreement mortgage = mortgageFor(h);
        if(mortgage != null) {
            principal = mortgage.principal;
        } else {
            principal = 0.0;
        }
        if (h == home) {
            h.getRegion().houseSaleMarket.offer(h, behaviour.getInitialSalePrice(h.getRegion(), h.getQuality(), principal), false);
        } else {
            h.getRegion().houseSaleMarket.offer(h, behaviour.getInitialSalePrice(h.getRegion(), h.getQuality(), principal), true);
        }
    }

    /////////////////////////////////////////////////////////
    // Houseowner interface
    /////////////////////////////////////////////////////////

    /********************************************************
     * Do all the stuff necessary when this household
     * buys a house:
     * Give notice to landlord if renting,
     * Get loan from mortgage-lender,
     * Pay for house,
     * Put house on rental market if buy-to-let and no tenant.
     ********************************************************/
    void completeHousePurchase(HouseOfferRecord sale) {
        if(isRenting()) { // give immediate notice to landlord and move out
            if(sale.getHouse().resident != null) System.out.println("Strange: my new house has someone in it!");
            if(home == sale.getHouse()) {
                System.out.println("Strange: I've just bought a house I'm renting out");
            } else {
                endTenancy();
            }
        }
        MortgageAgreement mortgage = Model.bank.requestLoan(this, sale.getPrice(),
                behaviour.decideDownPayment(this,sale.getPrice()), home == null, sale.getHouse());
        if(mortgage == null) {
            // TODO: need to either provide a way for house sales to fall through or to ensure that pre-approvals are always satisfiable
            System.out.println("Can't afford to buy house: strange");
            System.out.println("Bank balance is "+bankBalance);
            System.out.println("Annual income is "+ monthlyGrossEmploymentIncome *config.constants.MONTHS_IN_YEAR);
            if(isRenting()) System.out.println("Is renting");
            if(isHomeowner()) System.out.println("Is homeowner");
            if(isInSocialHousing()) System.out.println("Is homeless");
            if(isFirstTimeBuyer()) System.out.println("Is firsttimebuyer");
            if(behaviour.isPropertyInvestor()) System.out.println("Is investor");
            System.out.println("House owner = "+ sale.getHouse().owner);
            System.out.println("me = "+this);
        } else {
            bankBalance -= mortgage.downPayment;
            housePayments.put(sale.getHouse(), mortgage);
            if (home == null) { // move in to house
                home = sale.getHouse();
                sale.getHouse().resident = this;
            } else if (sale.getHouse().resident == null) { // put empty buy-to-let house on rental market
                sale.getHouse().region.houseRentalMarket.offer(sale.getHouse(), buyToLetRent(sale.getHouse()),
                        false);
            }
            isFirstTimeBuyer = false;
        }
    }

    /********************************************************
     * Do all stuff necessary when this household sells a house
     ********************************************************/
    public void completeHouseSale(HouseOfferRecord sale) {
        // First, receive money from sale
        bankBalance += sale.getPrice();
        // Second, find mortgage object and pay off as much outstanding debt as possible given bank balance
        MortgageAgreement mortgage = mortgageFor(sale.getHouse());
        bankBalance -= mortgage.payoff(bankBalance);
        // Third, remove the house from the household's housePayments object (even if there is outstanding debt!)
        // TODO: An implicit assumption here is that any outstanding debt that cannot be paid with the sellers savings
        // TODO: (including the money from the current sale) is wiped out, i.e., the household is bailed out. That is
        // TODO: why the house is removed from the housePayments object. Need to explain this in the article!
        housePayments.remove(sale.getHouse());
        // Fourth, if the house is still being offered on the rental market, withdraw the offer
        if (sale.getHouse().isOnRentalMarket()) {
            sale.getHouse().region.houseRentalMarket.removeOffer(sale);
        }
        // Fifth, if the house is the household's home, then the household moves out and becomes temporarily homeless...
        if (sale.getHouse() == home) {
            home.resident = null;
            home = null;
        // ...otherwise, if the house has a resident, if must be a renter, who must get evicted, also the rental income
        // corresponding to this tenancy must be subtracted from the owner's monthly rental income
        } else if (sale.getHouse().resident != null) {
            monthlyGrossRentalIncome -= sale.getHouse().resident.housePayments.get(sale.getHouse()).monthlyPayment;
            sale.getHouse().resident.getEvicted();
        }
    }
    
    /********************************************************
     * A BTL investor receives this message when a tenant moves
     * out of one of its buy-to-let houses.
     * 
     * The household simply puts the house back on the rental
     * market.
     ********************************************************/
    @Override
    public void endOfLettingAgreement(House h, PaymentAgreement contract) {
        monthlyGrossRentalIncome -= contract.monthlyPayment;

        // put house back on rental market
        if(!housePayments.containsKey(h)) {
            System.out.println("Strange: I don't own this house in endOfLettingAgreement");
        }
//        if(h.resident != null) System.out.println("Strange: renting out a house that has a resident");        
//        if(h.resident != null && h.resident == h.owner) System.out.println("Strange: renting out a house that belongs to a homeowner");        
        if(h.isOnRentalMarket()) System.out.println("Strange: got endOfLettingAgreement on house on rental market");
        if(!h.isOnMarket()) h.region.houseRentalMarket.offer(h, buyToLetRent(h), false);
    }

    /**********************************************************
     * This household moves out of current rented accommodation
     * and becomes homeless (possibly temporarily). Move out,
     * inform landlord and delete rental agreement.
     **********************************************************/
    private void endTenancy() {
        home.owner.endOfLettingAgreement(home, housePayments.get(home));
        housePayments.remove(home);
        home.resident = null;
        home = null;
    }
    
    /*** Landlord has told this household to get out: leave without informing landlord */
    private void getEvicted() {
        if(home == null) {
            System.out.println("Strange: got evicted but I'm homeless");            
        }
        if(home.owner == this) {
            System.out.println("Strange: got evicted from a home I own");
        }
        housePayments.remove(home);
        home.resident = null;
        home = null;        
    }

    
    /********************************************************
     * Do all the stuff necessary when this household moves
     * in to rented accommodation (i.e. set up a regular
     * payment contract. At present we use a MortgageApproval).
     ********************************************************/
    void completeHouseRental(HouseOfferRecord sale) {
        // If trying to rent own house, no need for contract
        if (sale.getHouse().owner == this) {
            System.out.println("Strange: I'm renting a house I own");
            System.exit(0);
        // Otherwise, write a contract
        } else {
            RentalAgreement rent = new RentalAgreement();
            rent.monthlyPayment = sale.getPrice();
            rent.nPayments = config.TENANCY_LENGTH_AVERAGE
                    + rand.nextInt(2*config.TENANCY_LENGTH_EPSILON + 1) - config.TENANCY_LENGTH_EPSILON;
//            rent.principal = rent.monthlyPayment*rent.nPayments;
            housePayments.put(sale.getHouse(), rent);
        }
        if (home != null) {
            System.out.println("Strange: I'm renting a house but not homeless");
            System.exit(0);
        } else {
            home = sale.getHouse();
        }
        if (sale.getHouse().resident != null) {
            System.out.println("Strange: tenant moving into an occupied house");
            if(sale.getHouse().resident == this) System.out.println("...and I'm the resident!");
            if(sale.getHouse().owner == this) System.out.println("...and I'm the owner!");
            if(sale.getHouse().owner == sale.getHouse().resident) System.out.println("...and owner and resident are the same!");
            System.exit(0);
        } else {
            sale.getHouse().resident = this;
        }
    }

    /**
     * Decide whether to bid on the house sale market or the rental market and where. This is an "intensity of choice"
     * decision (sigma function) on the cost of owning in the optimal region for this household (there where it can
     * afford the highest possible quality taking into account commuting costs) compared to the cost of renting in the
     * cheapest region for that same quality for this household (there where the price of this quality is the cheapest
     * taking into account commuting costs), with COST_OF_RENTING being an intrinsic psychological cost of not owning.
     * Note the use of max to refer to maximum quality in a given region while optimal refers to the maximum quality
     * among all regions.
     */
    private void bidForAHome() {
        // Declare variables
        RegionQualityPriceContainer optimalOptionForBuying;
        RegionQualityPriceContainer optimalOptionForRenting;
        // Find optimal option for buying (region where the household could afford the highest quality band, taking into
        // account commuting costs, among all possible regions)
        optimalOptionForBuying = findOptimalPurchaseRegion();
        // If household is a potential buy-to-let investor, then always buy...
        if (behaviour.isPropertyInvestor()) {
            // ...if household cannot afford minimum quality anywhere (optimal option for buying is null), then it
            // chooses to bid in the region with cheapest minimum quality
            if (optimalOptionForBuying == null) {
                optimalOptionForBuying = findCheapestPurchaseRegion();
            }
            // ...bid in the house sale market for the capped desired price
            optimalOptionForBuying.getRegion().houseSaleMarket.bid(this,
                    optimalOptionForBuying.getDesiredPrice());
        // Otherwise, for normal households...
        } else {
            // ...if household cannot afford minimum quality anywhere (optimal option for buying is null), then it tries
            // to find the optimal rental region (where it can afford the highest quality)
            if (optimalOptionForBuying == null) {
                optimalOptionForRenting = findOptimalRentalRegion();
                // ...if household cannot afford to rent minimum quality anywhere (optimal option for renting is null),
                // then it chooses to bid for rental in the cheapest region for renting (where the minimum quality has
                // the minimum price)
                if (optimalOptionForRenting == null) {
                    optimalOptionForRenting = findCheapestRentalRegion();
                }
                // ...bid in the house rental market for the desired rent price
                optimalOptionForRenting.getRegion().houseRentalMarket.bid(this,
                        optimalOptionForRenting.getDesiredPrice());
            // ...otherwise, if the normal household can afford to buy somewhere...
            } else {
                // ...then find the region where the same quality has the cheapest rental cost (including commuting)
                optimalOptionForRenting = findCheapestRentalRegionForQuality(optimalOptionForBuying.getQuality());
                // ...and decide between the purchase and the rental options
                if (decideRentOrPurchase(optimalOptionForBuying, optimalOptionForRenting)) {
                    // ...if buying, bid in the house sale market for the capped desired price
                    optimalOptionForBuying.getRegion().houseSaleMarket.bid(this,
                            optimalOptionForBuying.getDesiredPrice());
                } else {
                    // ...if renting, bid in the house rental market for the desired rent price
                    optimalOptionForRenting.getRegion().houseRentalMarket.bid(this,
                            optimalOptionForRenting.getDesiredPrice());
                }
            }
        }
        // TODO: Need to call here to an equivalent to the old countNonBTLBidsAboveExpAvSalePrice(), not implemented yet
    }

    /**
     * Find optimal region for buying, that is, the region where the household can afford the highest quality band when
     * looking at exponential moving average sale prices
     *
     * @return RegionQualityPriceContainer with information on the chosen region, i.e., the maximum quality the
     * household could afford there, the exponential moving average sale price of that quality, and the household's
     * desired purchase price there (taking into account commuting costs)
     */
    private RegionQualityPriceContainer findOptimalPurchaseRegion() {
        // Declare and initialise variables for comparisons
        double desiredPurchasePrice = 0.0; // Dummy value, never used
        int optimalQuality = -1; // Dummy value, it forces entering the if condition in the first iteration
        double optimalExpAvSalePrice = 0.0; // Dummy value, never used
        Region optimalRegionForBuying = null;
        // Find optimal region for buying. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find household's desired purchase price (with regional expected HPA)
            // TODO: Discuss with Doyne how to subtract from here commuting costs (which multiplier to transform annual
            // TODO: commuting cost into full house price discount)
            desiredPurchasePrice = behaviour.getDesiredPurchasePrice(monthlyGrossEmploymentIncome, region);
            // ...capped to the maximum mortgage available to the household, including commuting costs in the
            // affordability check
            desiredPurchasePrice = Math.min(desiredPurchasePrice, Model.bank.getMaxMortgage(bankBalance,
                    annualGrossEmploymentIncome, (getMonthlyNetTotalIncome() - getMonthlyCommutingCost(region)),
                    isFirstTimeBuyer, true));
            // ...with this desired purchase price, find highest quality this household could afford to buy in this
            // region
            int maxQualityForBuying = region.regionalHousingMarketStats.getMaxQualityForPrice(desiredPurchasePrice);
            // ...check if this quality is non-negative (i.e., household can at least afford the minimum quality) and it
            // is higher than (or equal and cheaper) than the previous optimal (among studied regions)
            if (maxQualityForBuying >= 0 && ((maxQualityForBuying > optimalQuality)
                    || ((maxQualityForBuying == optimalQuality)
                    && (region.regionalHousingMarketStats.getExpAvSalePriceForQuality(maxQualityForBuying)
                    < optimalExpAvSalePrice)))) {
                optimalQuality = maxQualityForBuying;
                optimalExpAvSalePrice
                        = region.regionalHousingMarketStats.getExpAvSalePriceForQuality(maxQualityForBuying);
                optimalRegionForBuying = region;
            }
        }
        if (optimalQuality < 0) {
            return null;
        } else {
            return new RegionQualityPriceContainer(optimalRegionForBuying, optimalQuality, optimalExpAvSalePrice,
                    desiredPurchasePrice);
        }
    }

    /**
     * Find optimal region for renting, that is, the region where the household can afford the highest quality band when
     * looking at exponential moving average rental prices
     *
     * @return RegionQualityPriceContainer with information on the chosen region, i.e., the maximum quality the
     * household could afford to rent there, the exponential moving average rental price of that quality, and the
     * household's desired rent price there (taking into account commuting costs)
     */
    private RegionQualityPriceContainer findOptimalRentalRegion() {
        // Declare and initialise variables for comparisons
        double desiredRentPrice = 0.0; // Dummy value, never used
        int optimalQuality = -1; // Dummy value, it forces entering the if condition in the first iteration
        double optimalExpAvRentPrice = 0.0; // Dummy value, never used
        Region optimalRegionForRenting = null;
        // Find optimal region for renting. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find household's desired rental price (taking into account commuting cost)
            desiredRentPrice = behaviour.getDesiredRentPrice((monthlyGrossEmploymentIncome
                    - getMonthlyCommutingCost(region)));
            // ...with this desired rent price, find highest quality this household could afford to rent in this region
            int maxQualityForRenting = region.regionalRentalMarketStats.getMaxQualityForPrice(desiredRentPrice);
            // ...check if this quality is non-negative (i.e., household can at least afford the minimum quality) and it
            // is higher than (or equal and cheaper) than the previous optimal (among studied regions)
            if (maxQualityForRenting >= 0 && ((maxQualityForRenting > optimalQuality)
                    || ((maxQualityForRenting == optimalQuality)
                    && (region.regionalRentalMarketStats.getExpAvSalePriceForQuality(maxQualityForRenting)
                    < optimalExpAvRentPrice)))) {
                optimalQuality = maxQualityForRenting;
                optimalExpAvRentPrice
                        = region.regionalRentalMarketStats.getExpAvSalePriceForQuality(maxQualityForRenting);
                optimalRegionForRenting = region;
            }
        }
        if (optimalQuality < 0) {
            return null;
        } else {
            return new RegionQualityPriceContainer(optimalRegionForRenting, optimalQuality, optimalExpAvRentPrice,
                    desiredRentPrice);
        }
    }

    /**
     * Find cheapest region for buying, that is, the region with the cheapest lowest quality band when looking at
     * exponential moving average sale prices
     *
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    private RegionQualityPriceContainer findCheapestPurchaseRegion() {
        // Declare and initialise variables for comparisons
        double cheapestTotalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region cheapestRegionForBuying = null;
        // Find cheapest region for buying. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find total purchase cost including the household's commuting cost to this region
            // TODO: Once a decision is made with Doyne about how to add commuting costs to the total price of the
            // TODO: house, it should be implemented here
            double totalCost = region.regionalHousingMarketStats.getExpAvSalePriceForQuality(0);
            // ...check if this cost is lower than or equal to the previous cheapest cost (among studied regions)
            if (totalCost <= cheapestTotalCost) {
                cheapestTotalCost = totalCost;
                cheapestRegionForBuying = region;
            }
        }
        // Find household's desired purchase price (with regional expected HPA)...
        // TODO: Discuss with Doyne how to subtract from here commuting costs (which multiplier to transform annual
        // TODO: commuting cost into full house price discount)
        double desiredPurchasePrice = behaviour.getDesiredPurchasePrice(monthlyGrossEmploymentIncome,
                cheapestRegionForBuying);
        // ...capped to the maximum mortgage available to the household, including commuting costs in the
        // affordability check
        desiredPurchasePrice = Math.min(desiredPurchasePrice, Model.bank.getMaxMortgage(bankBalance,
                annualGrossEmploymentIncome,
                (getMonthlyNetTotalIncome() - getMonthlyCommutingCost(cheapestRegionForBuying)), isFirstTimeBuyer,
                true));
        return new RegionQualityPriceContainer(cheapestRegionForBuying, 0, cheapestTotalCost,
                desiredPurchasePrice);
    }

    /**
     * Find cheapest region for renting, that is, the region with the cheapest lowest quality band when looking at
     * exponential moving average rental prices
     *
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    private RegionQualityPriceContainer findCheapestRentalRegion() {
        // Declare and initialise variables for comparisons
        double cheapestTotalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region cheapestRegionForRenting = null;
        // Find cheapest region for buying. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find total rental cost including the household's commuting cost to this region
            double totalCost = region.regionalRentalMarketStats.getExpAvSalePriceForQuality(0)
                    + getMonthlyCommutingCost(region);
            // ...check if this cost is lower than or equal to the previous cheapest cost (among studied regions)
            if (totalCost <= cheapestTotalCost) {
                cheapestTotalCost = totalCost;
                cheapestRegionForRenting = region;
            }
        }
        return new RegionQualityPriceContainer(cheapestRegionForRenting, 0, cheapestTotalCost,
                (behaviour.getDesiredRentPrice(monthlyGrossEmploymentIncome)
                        - getMonthlyCommutingCost(cheapestRegionForRenting)));
    }

    /**
     * Find cheapest region to rent a house of a given quality taking into account commuting costs
     *
     * @param quality Quality band to check
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    private RegionQualityPriceContainer findCheapestRentalRegionForQuality(int quality) {
        // Declare and initialise variables for comparisons
        double optimalMonthlyRentalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region optimalRegionForRenting = null;
        // For each region...
        for (Region region : geography.getRegions()) {
            // ...find monthly rental cost of that quality band (exponential average price + commuting costs)
            double monthlyRentalCost = region.regionalRentalMarketStats.getExpAvSalePriceForQuality(quality)
                    + getMonthlyCommutingCost(region);
            if (monthlyRentalCost <= optimalMonthlyRentalCost) {
                optimalMonthlyRentalCost = monthlyRentalCost;
                optimalRegionForRenting = region;
            }
        }
        return new RegionQualityPriceContainer(optimalRegionForRenting, quality, optimalMonthlyRentalCost,
                (behaviour.getDesiredRentPrice(monthlyGrossEmploymentIncome)
                        - getMonthlyCommutingCost(optimalRegionForRenting)));
    }

    /**
     * Between a given optimal purchase choice (in a given region and for a given average price) and a given optimal
     * rental choice (in a given region and for a given average price), both of them including commuting costs,
     * decide whether to go for the purchase or the rental option. Note that, even though the household may decide to
     * not rent a house of the same quality as they would buy, but rather of a different quality, the cash value of the
     * difference in quality is assumed to be the difference in rental price between the two qualities, thus being
     * economically equivalent options.
     *
     *  @return True if household decides for the purchase option, false if household decides for the rental option
     */
    private boolean decideRentOrPurchase(RegionQualityPriceContainer optimalOptionForBuying,
                                         RegionQualityPriceContainer optimalOptionForRenting) {
        // Simulate a mortgage request to assess annual mortgage cost for a house in the optimal region and quality band
        // for this household (i.e., using exponential average sale price for that region and quality band)
        MortgageAgreement mortgageApproval = Model.bank.requestApproval(this, optimalOptionForBuying.getExpAvPrice(),
                behaviour.decideDownPayment(this, optimalOptionForBuying.getExpAvPrice()), true);
        // Compute annual buying cost (annual mortgage cost plus annual commuting cost)
        double optimalAnnualBuyingCost = (mortgageApproval.monthlyPayment
                + getMonthlyCommutingCost(optimalOptionForBuying.getRegion())) * config.constants.MONTHS_IN_YEAR
                - optimalOptionForBuying.getExpAvPrice()
                * behaviour.getLongTermHPAExpectation(optimalOptionForBuying.getRegion());
        double optimalAnnualRentalCost = config.constants.MONTHS_IN_YEAR * (optimalOptionForRenting.getExpAvPrice()
                + getMonthlyCommutingCost(optimalOptionForRenting.getRegion()));
        // Compare costs to build a probability of buying based on a sigma function
        double probabilityOfBuying = behaviour.sigma(config.SENSITIVITY_RENT_OR_PURCHASE * (optimalAnnualRentalCost
                * (1.0 + config.PSYCHOLOGICAL_COST_OF_RENTING) - optimalAnnualBuyingCost));
        // Return a boolean which is true with that probability
        return rand.nextDouble() < probabilityOfBuying;
    }
    
    /********************************************************
     * Decide whether to sell ones own house.
     ********************************************************/
    private boolean decideToSellHouse(House h) {
        if(h == home) {
            return(behaviour.decideToSellHome(h));
        } else {
            return(behaviour.decideToSellInvestmentProperty(h, this));
        }
    }

    /***
     * Do stuff necessary when BTL investor lets out a rental
     * property
     */
    @Override
    public void completeHouseLet(HouseOfferRecord sale) {
        if (sale.getHouse().isOnMarket()) {
            sale.getHouse().region.houseSaleMarket.removeOffer(sale.getHouse().getSaleRecord());
        }
        monthlyGrossRentalIncome += sale.getPrice();
    }

    private double buyToLetRent(House h) {
        return(behaviour.buyToLetRent(
                h.region.regionalRentalMarketStats.getExpAvSalePriceForQuality(h.getQuality()),
                h.region.regionalRentalMarketStats.getExpAvDaysOnMarket(), h));
    }

    /**
     * Find the monthly commuting cost for this household: monthly commuting time times value of time, plus monthly
     * commuting fee
     */
    private double getMonthlyCommutingCost(Region region) {
        // TODO: Add travel fee here: 2.0*(travelTime*getTimeValue + travelFee)*config.constants.WORKING_DAYS_IN_MONTH
        return 2.0 * geography.getCommutingTimeBetween(jobRegion, region) * getTimeValue()
                * config.constants.WORKING_DAYS_IN_MONTH;
    }

    /**
     * Find the value (in GBP) of an hour of time for this household
     */
    private double getTimeValue(){
        return monthlyGrossEmploymentIncome / config.constants.WORKING_DAYS_IN_MONTH
                * config.constants.WORKING_HOURS_IN_DAY;
    }

    /////////////////////////////////////////////////////////
    // Inheritance behaviour
    /////////////////////////////////////////////////////////

    /**
     * Implement inheritance: upon death, transfer all wealth to the previously selected household.
     *
     * Take all houses off the markets, evict any tenants, pay off mortgages, and give property and remaining
     * bank balance to the beneficiary.
     * @param beneficiary The household that will inherit the wealth
     */
    void transferAllWealthTo(Household beneficiary) {
        // Check if beneficiary is the same as the deceased household
        if (beneficiary == this) { // TODO: I don't think this check is really necessary
            System.out.println("Strange: I'm transferring all my wealth to myself");
            System.exit(0);
        }
        // Create an iterator over the house-paymentAgreement pairs at the deceased household's housePayments object
        Iterator<Entry<House, PaymentAgreement>> paymentIt = housePayments.entrySet().iterator();
        Entry<House, PaymentAgreement> entry;
        House h;
        PaymentAgreement payment;
        // Iterate over these house-paymentAgreement pairs
        while(paymentIt.hasNext()) {
            entry = paymentIt.next();
            h = entry.getKey();
            payment = entry.getValue();
            // If the house is the deceased household's home, set both the house resident and household's home to null
            if (h == home) {
                h.resident = null;
                home = null;
            }
            // If the deceased household owns the house, then...
            if (h.owner == this) {
                // ...first, withdraw the house from any market where it is currently being offered
                if (h.isOnRentalMarket()) h.region.houseRentalMarket.removeOffer(h.getRentalRecord());
                if (h.isOnMarket()) h.region.houseSaleMarket.removeOffer(h.getSaleRecord());
                // ...then, if there is a resident, then this resident must be a tenant, who must get evicted
                if (h.resident != null) {
                    h.resident.getEvicted(); // TODO: Explain in paper that renters always get evicted, not just if heir needs the house
                }
                // ...finally, transfer the property to the beneficiary household
                beneficiary.inheritHouse(h);
            // Otherwise, if the deceased household does not own the house, it must have been renting it: end the letting agreement
            } else {
                h.owner.endOfLettingAgreement(h, housePayments.get(h));
            }
            // If payment agreement is a mortgage, then try to pay off as much as possible from the deceased household's bank balance
            if (payment instanceof MortgageAgreement) {
                bankBalance -= ((MortgageAgreement) payment).payoff();
            }
            // Remove the house-paymentAgreement entry from the deceased household's housePayments object
            paymentIt.remove(); // TODO: I don't think this is necessary
        }
        // Finally, transfer all remaining liquid wealth to the beneficiary household
        beneficiary.bankBalance += Math.max(0.0, bankBalance);
    }
    
    /**
     * Inherit a house.
     *
     * Write off the mortgage for the house. Move into the house if renting or in social housing.
     * 
     * @param h House to inherit
     */
    private void inheritHouse(House h) {
        // Create a null (zero payments) mortgage
        MortgageAgreement nullMortgage = new MortgageAgreement(this,false);
        nullMortgage.nPayments = 0;
        nullMortgage.downPayment = 0.0;
        nullMortgage.monthlyInterestRate = 0.0;
        nullMortgage.monthlyPayment = 0.0;
        nullMortgage.principal = 0.0;
        nullMortgage.purchasePrice = 0.0;
        // Become the owner of the inherited house and include it in my housePayments list (with a null mortgage)
        // TODO: Make sure the paper correctly explains that no debt is inherited
        housePayments.put(h, nullMortgage);
        h.owner = this;
        // Check for residents in the inherited house
        if (h.resident != null) {
            System.out.println("Strange: inheriting a house with a resident");
            System.exit(0);
        }
        // If renting or homeless, move into the inherited house
        if (!isHomeowner()) {
            // If renting, first cancel my current tenancy
            if (isRenting()) {
                endTenancy();                
            }
            home = h;
            h.resident = this;
        // If owning a home and having the BTL gene...
        } else if (behaviour.isPropertyInvestor()) {
            // ...decide whether to sell the inherited house
            if (decideToSellHouse(h)) {
                putHouseForSale(h);
            // ...or rent it out
            } else {
                h.region.houseRentalMarket.offer(h, buyToLetRent(h), false);
            }
        // If being an owner-occupier, put inherited house for sale
        } else {
            putHouseForSale(h);
        }
    }

    //----- Helpers -----//

    public double getAge() { return age; }

    public boolean isHomeowner() {
        if(home == null) return(false);
        return(home.owner == this);
    }

    public boolean isRenting() {
        if(home == null) return(false);
        return(home.owner != this);
    }

    public boolean isInSocialHousing() { return home == null; }

    boolean isFirstTimeBuyer() { return isFirstTimeBuyer; }

    public boolean isBankrupt() { return isBankrupt; }

    public double getBankBalance() { return bankBalance; }

    public House getHome() { return home; }

    public Map<House, PaymentAgreement> getHousePayments() { return housePayments; }

    public double getAnnualGrossEmploymentIncome() { return annualGrossEmploymentIncome; }

    public double getMonthlyGrossEmploymentIncome() { return monthlyGrossEmploymentIncome; }
    
    /***
     * @return Number of properties this household currently has on the sale market
     */
    public int nPropertiesForSale() {
        int n=0;
        for(House h : housePayments.keySet()) {
            if(h.isOnMarket()) ++n;
        }
        return(n);
    }
  
    public int nInvestmentProperties() { return housePayments.size() - 1; }
    
    /***
     * @return Current mark-to-market (with exponentially averaged prices per quality) equity in this household's home.
     */
    double getHomeEquity() {
        if(!isHomeowner()) return(0.0);
        return home.region.regionalHousingMarketStats.getExpAvSalePriceForQuality(home.getQuality())
                - mortgageFor(home).principal;
    }
    
    public MortgageAgreement mortgageFor(House h) {
        PaymentAgreement payment = housePayments.get(h);
        if(payment instanceof MortgageAgreement) {
            return((MortgageAgreement)payment);
        }
        return(null);
    }

    public double monthlyPaymentOn(House h) {
        PaymentAgreement payment = housePayments.get(h);
        if(payment != null) {
            return(payment.monthlyPayment);
        }
        return(0.0);        
    }
}
