package housing;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

/**************************************************************************************************
 * Class to implement the behavioural decisions made by households
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class HouseholdBehaviour {

    //------------------//
    //----- Fields -----//
    //------------------//

    private static Config                   config = Model.config; // Passes the Model's configuration parameters object to a private static field
    private static MersenneTwister	        rand = Model.rand; // Passes the Model's random number generator to a private static field
    private static LogNormalDistribution    downpaymentDistFTB = new LogNormalDistribution(rand,
            config.DOWNPAYMENT_FTB_SCALE, config.DOWNPAYMENT_FTB_SHAPE); // Size distribution for downpayments of first-time-buyers
    private static LogNormalDistribution    downpaymentDistOO = new LogNormalDistribution(rand,
            config.DOWNPAYMENT_OO_SCALE, config.DOWNPAYMENT_OO_SHAPE); // Size distribution for downpayments of owner-occupiers
    private boolean                         BTLInvestor;
    private double                          BTLCapGainCoefficient; // Sensitivity of BTL investors to capital gain, 0.0 cares only about rental yield, 1.0 cares only about cap gain
    private double                          propensityToSave;
    private Geography                       geography;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

	/**
	 * Initialise behavioural variables for a new household: propensity to save, whether the household will have the BTL
     * investor "gene" (provided its income percentile is above a certain minimum), and whether the household will be a
     * fundamentalist or a trend follower investor (provided it has received the BTL investor gene)
	 *
	 * @param incomePercentile Fixed income percentile for the household (assumed constant over a lifetime)
     */
	HouseholdBehaviour(Geography geography, double incomePercentile) {
		this.geography = geography;
	    // Compute propensity to save, so that it is constant for a given household
        propensityToSave = rand.nextDouble();
        // Decide if household is a BTL investor and, if so, its tendency to seek capital gains or rental yields
		BTLCapGainCoefficient = 0.0;
        if(incomePercentile > config.MIN_INVESTOR_PERCENTILE &&
                rand.nextDouble() < config.getPInvestor()/config.MIN_INVESTOR_PERCENTILE) {
            BTLInvestor = true;
            if(rand.nextDouble() < config.P_FUNDAMENTALIST) {
                BTLCapGainCoefficient = config.FUNDAMENTALIST_CAP_GAIN_COEFF;
            } else {
                BTLCapGainCoefficient = config.TREND_CAP_GAIN_COEFF;
            }
        } else {
            BTLInvestor = false;
        }
	}

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- General behaviour -----//

	/**
	 * Compute the monthly non-essential or optional consumption by a household. It is calibrated so that the output
     * wealth distribution fits the ONS wealth data for Great Britain.
	 *
	 * @param bankBalance Household's liquid wealth
     * @param annualGrossTotalIncome Household's annual gross total income
	 */
    double getDesiredConsumption(double bankBalance, double annualGrossTotalIncome) {
        return config.CONSUMPTION_FRACTION*Math.max(bankBalance
                - data.Wealth.getDesiredBankBalance(annualGrossTotalIncome, propensityToSave), 0.0);
    }

    //----- Owner-Occupier behaviour -----//

	/**
     * Desired purchase price used to decide whether to buy a house and how much to bid for it
     *
	 * @param monthlyGrossEmploymentIncome Monthly gross employment income of the household
	 */
	double getDesiredPurchasePrice(double monthlyGrossEmploymentIncome, Region region) {
	    // TODO: This product is generally so small that it barely has any impact on the results, need to rethink if
        // TODO: it is necessary and if this small value makes any sense
        double HPAFactor = config.BUY_WEIGHT_HPA*getLongTermHPAExpectation(region);
        // TODO: The capping of this factor intends to avoid negative and too large desired prices, the 0.9 is a
        // TODO: purely artificial fudge parameter. This formula should be reviewed and changed!
        if (HPAFactor > 0.9) HPAFactor = 0.9;
        // TODO: Note that wealth is not used here, but only monthlyGrossEmploymentIncome
		return config.BUY_SCALE*config.constants.MONTHS_IN_YEAR*monthlyGrossEmploymentIncome
                *Math.exp(config.BUY_EPSILON*rand.nextGaussian())
                /(1.0 - HPAFactor);
	}

	/**
     * Initial sale price of a house to be listed
     *
	 * @param region Reference to the region where the house to be sold sits
	 * @param quality Quality of the house ot be sold
	 * @param principal Amount of principal left on any mortgage on this house
	 */
	double getInitialSalePrice(Region region, int quality, double principal) {
        double exponent = config.SALE_MARKUP
                + Math.log(region.regionalHousingMarketStats.getExpAvSalePriceForQuality(quality) + 1.0)
                - config.SALE_WEIGHT_MONTHS_ON_MARKET
                * Math.log(region.regionalHousingMarketStats.getExpAvMonthsOnMarketForQuality(quality) + 1.0)
                + config.SALE_EPSILON*rand.nextGaussian();
        return Math.max(Math.exp(exponent), principal);
	}

	/**
     * This method implements a household's decision to sell their owner-occupied property. On average, households sell
     * owner-occupied houses every 11 years, due to exogenous reasons not addressed in the model.
     *
	 * @return True if the owner-occupier decides to sell the house and false otherwise.
	 */
	boolean decideToSellHome() {
        // TODO: This if implies BTL agents never sell their homes, need to explain in paper!
        return !isPropertyInvestor() && (rand.nextDouble() < config.derivedParams.MONTHLY_P_SELL);
    }

	/**
	 * Decide amount to pay as initial downpayment
     *
	 * @param me the household
	 * @param housePrice the price of the house
     */
	double decideDownPayment(Household me, double housePrice) {
		if (me.getBankBalance() > housePrice*config.DOWNPAYMENT_BANK_BALANCE_FOR_CASH_SALE) {
			return housePrice;
		}
		double downpayment;
		if (me.isFirstTimeBuyer()) {
		    // Since the function of the HPI is to move the down payments distribution upwards or downwards to
            // accommodate current price levels, and the distribution is itself aggregate, we use the aggregate HPI
			downpayment = Model.housingMarketStats.getHPI()*downpaymentDistFTB.inverseCumulativeProbability(Math.max(0.0,
                    (me.incomePercentile - config.DOWNPAYMENT_MIN_INCOME)/(1 - config.DOWNPAYMENT_MIN_INCOME)));
		} else if (isPropertyInvestor()) {
			downpayment = housePrice*(Math.max(0.0,
					config.DOWNPAYMENT_BTL_MEAN + config.DOWNPAYMENT_BTL_EPSILON*rand.nextGaussian()));
		} else {
			downpayment = Model.housingMarketStats.getHPI()*downpaymentDistOO.inverseCumulativeProbability(Math.max(0.0,
                    (me.incomePercentile - config.DOWNPAYMENT_MIN_INCOME)/(1 - config.DOWNPAYMENT_MIN_INCOME)));
		}
		if (downpayment > me.getBankBalance()) downpayment = me.getBankBalance();
		return downpayment;
	}

	/**
	 * Decide how much to drop the list-price of a house if it has been on the market for (another) month and hasn't
	 * sold. Calibrated against Zoopla data at the Bank of England
	 *
	 * @param sale The HouseOfferRecord of the house that is on the market.
	 */
	double rethinkHouseSalePrice(HouseOfferRecord sale) {
		if (rand.nextDouble() < config.P_SALE_PRICE_REDUCE) {
			double logReduction = config.REDUCTION_MU + (rand.nextGaussian() * config.REDUCTION_SIGMA);
			return sale.getPrice() * (1.0 - Math.exp(logReduction) / 100.0);
		}
		return sale.getPrice();
	}

    //----- Spatial decisions -----//

    /**
     * Find optimal region for buying, that is, the region where the household can afford the highest quality band when
     * looking at exponential moving average sale prices
     *
     * @return RegionQualityPriceContainer with information on the chosen region, i.e., the maximum quality the
     * household could afford there, the exponential moving average sale price of that quality, and the household's
     * desired purchase price there (taking into account commuting fees at the mortgage affordability check)
     * TODO: Effect of total commuting cost on desired purchase price still to be implemented
     */
    RegionQualityPriceContainer findOptimalPurchaseRegion(Household h) {
        // Declare and initialise variables for comparisons
        double desiredPurchasePrice = 0.0; // Dummy value, never used
        int optimalQuality = -1; // Dummy value, it forces entering the if condition in the first iteration
        double optimalExpAvSalePrice = 0.0; // Dummy value, never used
        double optimalDesiredPurchasePrice = 0.0; // Dummy value, never used
        Region optimalRegionForBuying = null;
        // Find optimal region for buying. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find household's desired purchase price (with regional expected HPA)
            // TODO: Discuss with Doyne how to subtract from here total commuting costs (time + fees), that is, which
            // TODO: multiplier to use to transform annual commuting cost into full house price discount
            desiredPurchasePrice = getDesiredPurchasePrice(h.getMonthlyGrossEmploymentIncome(), region);
            // ...capped to the maximum mortgage available to the household, including commuting fees (effective
            // commuting cost) in the affordability check
            desiredPurchasePrice = Math.min(desiredPurchasePrice, Model.bank.getMaxMortgage(h.getBankBalance(),
                    h.getAnnualGrossEmploymentIncome(),
                    (h.getMonthlyNetEmploymentIncome() - h.getMonthlyCommutingFee(region)),
                    h.isFirstTimeBuyer(), true));
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
                optimalDesiredPurchasePrice = desiredPurchasePrice;
                optimalRegionForBuying = region;
            }
        }
        if (optimalQuality < 0) {
            return null;
        } else {
            return new RegionQualityPriceContainer(optimalRegionForBuying, optimalQuality, optimalExpAvSalePrice,
                    optimalDesiredPurchasePrice);
        }
    }

    /**
     * Find optimal region for renting, that is, the region where the household can afford the highest quality band when
     * looking at exponential moving average rental prices and taking into account total commuting costs, i.e., both
     * commuting times and commuting fees. Note that this assumes that the household seeks to be economically
     * compensated for the time spent commuting.
     *
     * @return RegionQualityPriceContainer with information on the chosen region, i.e., the maximum quality the
     * household could afford to rent there, the exponential moving average rental price of that quality, and the
     * household's desired rent price there (taking into account total commuting costs, time + fees)
     */
    RegionQualityPriceContainer findOptimalRentalRegion(Household h) {
        // Declare and initialise variables for comparisons
        double desiredRentPrice = 0.0; // Dummy value, never used
        int optimalQuality = -1; // Dummy value, it forces entering the if condition in the first iteration
        double optimalExpAvRentPrice = 0.0; // Dummy value, never used
        double optimalDesiredRentPrice = 0.0; // Dummy value, never used
        Region optimalRegionForRenting = null;
        // Find optimal region for renting. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find household's desired rental price (taking into account total commuting cost, time + fees)
            desiredRentPrice = getDesiredRentPrice((h.getMonthlyGrossEmploymentIncome()
                    - h.getMonthlyCommutingCost(region)));
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
                optimalDesiredRentPrice = desiredRentPrice;
                optimalRegionForRenting = region;
            }
        }
        if (optimalQuality < 0) {
            return null;
        } else {
            return new RegionQualityPriceContainer(optimalRegionForRenting, optimalQuality, optimalExpAvRentPrice,
                    optimalDesiredRentPrice);
        }
    }

    /**
     * Find cheapest region for buying, that is, the region with the cheapest lowest quality band when looking at
     * exponential moving average sale prices and taking into account only commuting fees: being the household unable to
     * afford even the lowest quality in any region, it is forced to accept the cheapest combination of housing prices
     * and commuting fees, not being able to seek any economic compensation for the time spent commuting.
     *
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    RegionQualityPriceContainer findCheapestPurchaseRegion(Household h) {
        // Declare and initialise variables for comparisons
        double cheapestTotalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region cheapestRegionForBuying = null;
        // Find cheapest region for buying. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find total purchase cost including the household's commuting cost to this region
            // TODO: Once a decision is made with Doyne about which multiplier to use to transform annual commuting fees
            // TODO: into full house price discount, these fees should be subtracted here
            double totalCost = region.regionalHousingMarketStats.getExpAvSalePriceForQuality(0);
            // ...check if this cost is lower than or equal to the previous cheapest cost (among studied regions)
            if (totalCost <= cheapestTotalCost) {
                cheapestTotalCost = totalCost;
                cheapestRegionForBuying = region;
            }
        }
        // Find household's desired purchase price (with regional expected HPA)...
        // TODO: Discuss with Doyne how to subtract from here total commuting costs (time + fees), that is, which
        // TODO: multiplier to use to transform annual commuting cost into full house price discount
        double desiredPurchasePrice = getDesiredPurchasePrice(h.getMonthlyGrossEmploymentIncome(),
                cheapestRegionForBuying);
        // ...capped to the maximum mortgage available to the household, including commuting fees (effective commuting
        // cost) in the affordability check
        desiredPurchasePrice = Math.min(desiredPurchasePrice, Model.bank.getMaxMortgage(h.getBankBalance(),
                h.getAnnualGrossEmploymentIncome(),
                (h.getMonthlyNetEmploymentIncome() - h.getMonthlyCommutingFee(cheapestRegionForBuying)),
                h.isFirstTimeBuyer(),
                true));
        return new RegionQualityPriceContainer(cheapestRegionForBuying, 0, cheapestTotalCost,
                desiredPurchasePrice);
    }

    /**
     * Find cheapest region for renting, that is, the region with the cheapest lowest quality band when looking at
     * exponential moving average rental prices and taking into account only commuting fees: being the household unable
     * to afford even the lowest quality in any region, it is forced to accept the cheapest combination of house rental
     * prices and commuting fees, not being able to seek any economic compensation for the time spent commuting.
     *
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    RegionQualityPriceContainer findCheapestRentalRegion(Household h) {
        // Declare and initialise variables for comparisons
        double cheapestTotalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region cheapestRegionForRenting = null;
        // Find cheapest region for renting. To this end, for each region...
        for (Region region : geography.getRegions()) {
            // ...find total rental cost including the household's commuting fees to this region
            double totalCost = region.regionalRentalMarketStats.getExpAvSalePriceForQuality(0)
                    + h.getMonthlyCommutingFee(region);
            // ...check if this cost is lower than or equal to the previous cheapest cost (among studied regions)
            if (totalCost <= cheapestTotalCost) {
                cheapestTotalCost = totalCost;
                cheapestRegionForRenting = region;
            }
        }
        // Return container with cheapest rental region and desired rental price taking into account only commuting
        // fees. Note that this might lead to negative bid prices if the desired bid price is smaller than the monthly
        // commuting fee
        return new RegionQualityPriceContainer(cheapestRegionForRenting, 0, cheapestTotalCost,
                (getDesiredRentPrice(h.getMonthlyGrossEmploymentIncome())
                        - h.getMonthlyCommutingFee(cheapestRegionForRenting)));
    }

    /**
     * Find cheapest region to rent a house of a given quality taking into account total commuting costs, i.e., both
     * commuting times and commuting fees. Note that this assumes that the household seeks to be economically
     * compensated for the time spent commuting.
     *
     * @param quality Quality band to check
     * @return RegionQualityPriceContainer with information on the chosen region
     */
    RegionQualityPriceContainer findCheapestRentalRegionForQuality(int quality, Household h) {
        // Declare and initialise variables for comparisons
        double optimalMonthlyRentalCost = Double.POSITIVE_INFINITY; // Dummy value, used only for 1st entry at if statement within for loop
        Region optimalRegionForRenting = null;
        // For each region...
        for (Region region : geography.getRegions()) {
            // ...find monthly rental cost of that quality band taking into account total commuting costs (time + fees)
            double monthlyRentalCost = region.regionalRentalMarketStats.getExpAvSalePriceForQuality(quality)
                    + h.getMonthlyCommutingCost(region);
            if (monthlyRentalCost <= optimalMonthlyRentalCost) {
                optimalMonthlyRentalCost = monthlyRentalCost;
                optimalRegionForRenting = region;
            }
        }
        // Return container with cheapest rental region and desired rental price taking into account total commuting
        // cost (time + fees). Note that this might lead to negative bid prices if the desired bid price is smaller than
        // the monthly commuting cost
        return new RegionQualityPriceContainer(optimalRegionForRenting, quality, optimalMonthlyRentalCost,
                (getDesiredRentPrice(h.getMonthlyGrossEmploymentIncome())
                        - h.getMonthlyCommutingCost(optimalRegionForRenting)));
    }

    /**
     * Between a given optimal purchase choice (in a given region and for a given average price) and a given optimal
     * rental choice (in a given region and for a given average price), both of them including total commuting costs
     * (time + fees, i.e., the household is assume to be able to seek an economic compensation for the time spent
     * commuting), decide whether to go for the purchase or the rental option. Note that, even though the household may
     * decide to not rent a house of the same quality as they would buy, but rather of a different quality, the cash
     * value of the difference in quality is assumed to be the same as the difference in rental price between the two
     * qualities, thus being economically equivalent options.
     *
     *  @return True if household decides for the purchase option, false if household decides for the rental option
     */
    boolean decideRentOrPurchase(RegionQualityPriceContainer optimalOptionForBuying,
                                 RegionQualityPriceContainer optimalOptionForRenting, Household h) {
        // Simulate a mortgage request to assess annual mortgage cost for a house in the optimal region and quality band
        // for this household (i.e., using exponential average sale price for that region and quality band)
        MortgageAgreement mortgageApproval = Model.bank.requestApproval(h, optimalOptionForBuying.getExpAvPrice(),
                decideDownPayment(h, optimalOptionForBuying.getExpAvPrice()), true);
        // Compute annual buying cost (annual mortgage cost plus annual total commuting cost, time + fees)
        double optimalAnnualBuyingCost = (mortgageApproval.monthlyPayment
                + h.getMonthlyCommutingCost(optimalOptionForBuying.getRegion())) * config.constants.MONTHS_IN_YEAR
                - optimalOptionForBuying.getExpAvPrice()
                * getLongTermHPAExpectation(optimalOptionForBuying.getRegion());
        // Compute annual renting cost (annual rent plus annual total commuting cost, time + fees)
        double optimalAnnualRentalCost = config.constants.MONTHS_IN_YEAR * (optimalOptionForRenting.getExpAvPrice()
                + h.getMonthlyCommutingCost(optimalOptionForRenting.getRegion()));
        // Compare costs to build a probability of buying based on a sigma function
        double probabilityOfBuying = sigma(config.SENSITIVITY_RENT_OR_PURCHASE * (optimalAnnualRentalCost
                * (1.0 + config.PSYCHOLOGICAL_COST_OF_RENTING) - optimalAnnualBuyingCost));
        // Return a boolean which is true with that probability
        return rand.nextDouble() < probabilityOfBuying;
    }

    ///////////////////////////////////////////////////////////
    ///////////////////////// REVISED /////////////////////////
    ///////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Renter behaviour
	///////////////////////////////////////////////////////////////////////////////////////////////

	/********************************************************
	 * Decide how much to bid on the rental market
	 * Source: Zoopla rental prices 2008-2009 (at Bank of England)
	 ********************************************************/
	// TODO: Check if monthly gross employment income is the correct one for this use, rather than net employment income
	double getDesiredRentPrice(double monthlyGrossEmploymentIncome) {
	    return monthlyGrossEmploymentIncome*config.BID_RENT_AS_FRACTION_OF_INCOME;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Property investor behaviour
	///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decide whether to sell or not an investment property. Investor households with only one investment property do
     * never sell it. A sale is never attempted when the house is occupied by a tenant. Households with at least two
     * investment properties will calculate the expected yield of the property in question based on two contributions:
     * rental yield and capital gain (with their corresponding weights which depend on the type of investor)
	 *
	 * @param h The house in question
	 * @param me The investor household
	 * @return True if investor me decides to sell investment property h
	 */
	boolean decideToSellInvestmentProperty(House h, Household me) {
		// Fast decisions...
        // ...always keep at least one investment property (i.e., at least two properties)
		if(me.getNProperties() < 3) return false;
        // ...don't sell while occupied by tenant
		if(!h.isOnRentalMarket()) return false;

        // Find the expected equity yield rate of this property as a weighted mix of both rental yield and capital gain
        // times the leverage
        // ...find the mortgage agreement for this property
        MortgageAgreement mortgage = me.mortgageFor(h);
        // ...find its current (fair market value) sale price
        double currentMarketPrice = h.region.regionalHousingMarketStats.getExpAvSalePriceForQuality(h.getQuality());
        // ...find equity, or assets minus liabilities
        double equity = Math.max(0.01, currentMarketPrice - mortgage.principal); // The 0.01 prevents possible divisions by zero later on
        // ...find the leverage on that mortgage (Assets divided by equity, or return on equity)
		double leverage = currentMarketPrice/equity;
        // ...find the expected rental yield of this property as its current rental price divided by its current (fair market value) sale price
		// TODO: ATTENTION ---> This rental yield is not accounting for expected occupancy... shouldn't it?
		double currentRentalYield = h.getRentalRecord().getPrice()*config.constants.MONTHS_IN_YEAR/currentMarketPrice;
        // ...find the mortgage rate (pounds paid a year per pound of equity)
		double mortgageRate = mortgage.nextPayment()*config.constants.MONTHS_IN_YEAR/equity;
        // ...finally, find expected equity yield, or yield on equity
        double expectedEquityYield = leverage*((1.0 - BTLCapGainCoefficient)*currentRentalYield
                    + BTLCapGainCoefficient*getLongTermHPAExpectation(h.region))
                    - mortgageRate;
		// Compute a probability to keep the property as a function of the effective yield
		double pKeep = Math.pow(sigma(config.BTL_CHOICE_INTENSITY*expectedEquityYield),
                1.0/config.constants.MONTHS_IN_YEAR);
		// Return true or false as a random draw from the computed probability
		return rand.nextDouble() < (1.0 - pKeep);
	}

    /**
     * Decide whether to buy or not a new investment property and where. Investor households with no investment
     * properties always attempt to buy one. If the household's bank balance is below its desired bank balance, then no
     * attempt to buy is made. If the resources available to the household (maximum mortgage) are below the average
     * price for the lowest quality houses in a given region, then the probability to try to buy in that region is zero.
     * Households compute a probability to invest in each region, proportional to the expected yield of a newly bought
     * property in that region. A random number is then drawn for deciding whether to buy or not and in which region. In
     * the case of household with no investment property, the random number is re-normalised to go from 0.0 to the sum
     * of all the probabilities to buy, such that the household always tries to buy, only the specific region needs to
     * be chosen. In the case of households with at least one investment property, the random number is re-normalised to
     * go from 0.0 to the number of regions, such that not buying (a number above the sum of all the probabilities to
     * buy) is a possible outcome.
     *
     * @param me The investor household
     * @return The region where the household decides to invest, null if the household decides not to invest
     */
    Region decideWhereToBuyInvestmentProperty(Household me) {
        // Fast decision: never buy (keep on saving) if bank balance is below the household's desired bank balance
        if (me.getBankBalance() < data.Wealth.getDesiredBankBalance(me.getAnnualGrossTotalIncome(),
                me.behaviour.getPropensityToSave())*config.BTL_CHOICE_MIN_BANK_BALANCE) { return null; }

        // Compute and store the probability to invest en each region, as well as the sum of these probabilities
        double[] probToBuyPerRegion = findProbabilityToInvestPerRegion(me);
        double sumProbToBuy = 0.0;
        for (double probToBuy : probToBuyPerRegion) {
            sumProbToBuy += probToBuy;
        }

        // Draw a double random number for decision making and initialise a counter
        double randDouble = rand.nextDouble();
        double counter = 0.0;
        // If the investor household currently owns no investment property, then it always decide to buy...
        if (me.getNProperties() < 2) {
            // ...thus the random number should be re-normalised to go from 0.0 to sumProbToBuy
            randDouble *= sumProbToBuy;
        // Otherwise, the investor household might buy or not...
        } else {
            // ...thus the random number should be re-normalised to go from 0.0 to the number of regions
            randDouble *= probToBuyPerRegion.length;
        }

        // Finally, iterate over the previously computed probabilities to find the chosen option...
        int i = 0;
        while ((i < probToBuyPerRegion.length) && (counter + probToBuyPerRegion[i] < randDouble)) {
            counter += probToBuyPerRegion[i];
            i++;
        }
        // ...in case the household must invest (it currently owns no investment property), but the probability to buy
        // is zero for all regions, then choose a region at random (otherwise it would always choose the first region!)
        if ((me.getNProperties() < 2) && sumProbToBuy == 0.0) {
            i = rand.nextInt(probToBuyPerRegion.length);
        }
        // ...and, if the chosen number corresponds to a region, then return that region
        if (i < probToBuyPerRegion.length) {
            return geography.getRegions().get(i);
        // ...return null otherwise
        } else {
            return null;
        }
    }

    /**
     * Compute the probability to invest in each region, proportional to the expected yield of a newly bought property
     * in that region. This expected yield is based on two contributions: rental yield and capital gain (with weights
     * that depend on the type of investor).
     *
     * @param me The investor household
     * @return An array with the probability to buy an investment property in each region
     */
    private double[] findProbabilityToInvestPerRegion(Household me) {
        // Find variables common to all regions (equity, leverage and mortgage rate) for a hypothetical house maximising
        // the leverage available to the household...
        // ...find maximum price (maximum mortgage + all liquid wealth) the household could pay
        double maxPrice = Model.bank.getMaxMortgage(me.getBankBalance(), me.getAnnualGrossEmploymentIncome(),
                me.getMonthlyNetEmploymentIncome(), me.isFirstTimeBuyer(), false);
        // ...find mortgage with maximum leverage by requesting maximum mortgage with minimum downpayment
        MortgageAgreement mortgage = Model.bank.requestApproval(me, maxPrice, 0.0, false);
        // ...find equity, or assets minus liabilities (which, initially, is simply the downpayment)
        double equity = Math.max(0.01, mortgage.downPayment); // The 0.01 prevents possible divisions by zero later on
        // ...find the leverage on that mortgage (Assets divided by equity, or return on equity)
        double leverage = mortgage.purchasePrice/equity;
        // ...find the mortgage rate (pounds paid a year per pound of equity)
        double mortgageRate = mortgage.nextPayment()*config.constants.MONTHS_IN_YEAR/equity;

        // Compute and store the probability to invest en each region, as well as the sum of these probabilities
        double[] probToBuyPerRegion = new double[geography.getRegions().size()];
        // To this end, iterate through the regions...
        int i = 0;
        for (Region region : geography.getRegions()) {
            // ...finding the expected rental yield as an (exponential) average over all house qualities
            double rentalYield = region.regionalRentalMarketStats.getExpAvFlowYield();
            // ...computing the expected equity yield, or yield on equity
            double expectedEquityYield = leverage*((1.0 - BTLCapGainCoefficient)*rentalYield
                    + BTLCapGainCoefficient*getLongTermHPAExpectation(region))
                    - mortgageRate;
            // ...and, finally, computing the probability to buy a new investment property in this region as a function
            // of its expected equity yield
            probToBuyPerRegion[i] = 1.0 - Math.pow((1.0 - sigma(config.BTL_CHOICE_INTENSITY*expectedEquityYield)),
                    1.0/config.constants.MONTHS_IN_YEAR);
            // ...with the caveat that, households assign zero probability to buy to regions where they cannot afford
            // the average price of even the lowest quality band
            if (maxPrice < region.regionalHousingMarketStats.getExpAvSalePriceForQuality(0)) {
                probToBuyPerRegion[i] = 0.0;
            }
            i++;
        }
        return probToBuyPerRegion;
    }

    double btlPurchaseBid(Household me, Region region) {
        // TODO: What is this 1.1 factor? Another fudge parameter? It prevents wealthy investors from offering more than
        // TODO: 10% above the average price of top quality houses. The effect of this is to prevent fast increases of
        // TODO: price as BTL investors buy all supply till prices are too high for everybody. Fairly unclear mechanism,
        // TODO: check for removal!
        return(Math.min(Model.bank.getMaxMortgage(me.getBankBalance(), me.getAnnualGrossEmploymentIncome(),
                me.getMonthlyNetEmploymentIncome(), me.isFirstTimeBuyer(), false),
                1.1*region.regionalHousingMarketStats.getExpAvSalePriceForQuality(config.N_QUALITY-1)));
    }

	/**
	 * How much rent does an investor decide to charge on a buy-to-let house? To make a decision, the household will
     * check current exponential average rent prices for houses of the same quality, and the current exponential average
     * time on market for houses of the same quality.
     *
	 * @param quality The quality of the house
	 */
	double buyToLetRent(int quality, Region region) {
		// TODO: What? Where does this equation come from?
		final double beta = config.RENT_MARKUP/Math.log(config.RENT_EQ_MONTHS_ON_MARKET); // Weight of months-on-market effect
		double exponent = config.RENT_MARKUP
                + Math.log(region.regionalRentalMarketStats.getExpAvSalePriceForQuality(quality) + 1.0)
                - beta*Math.log(region.regionalRentalMarketStats.getExpAvMonthsOnMarketForQuality(quality) + 1.0)
                + config.RENT_EPSILON * rand.nextGaussian();
		double result = Math.exp(exponent);
        // TODO: The following contains clamps rent prices to be at least 12*RENT_MAX_AMORTIZATION_PERIOD times below
        // TODO: sale prices, thus setting also a minimum rental yield
        double minAcceptable = region.regionalHousingMarketStats.getExpAvSalePriceForQuality(quality)
                /(config.RENT_MAX_AMORTIZATION_PERIOD*config.constants.MONTHS_IN_YEAR);
        if (result < minAcceptable) result = minAcceptable;
		return result;
	}

	/**
	 * Update the demanded rent for a property
	 *
	 * @param sale the HouseOfferRecord of the property for rent
	 * @return the new rent
     */
	double rethinkBuyToLetRent(HouseOfferRecord sale) { return (1.0 - config.RENT_REDUCTION)*sale.getPrice(); }

    /**
     * Logistic function, sometimes called sigma function, 1/1+e^(-x)
     *
     * @param x Parameter of the sigma or logistic function
     */
    private double sigma(double x) { return 1.0/(1.0 + Math.exp(-1.0*x)); }

	/**
     * @return expectation value of HPI in one year's time divided by today's HPI
     */
	private double getLongTermHPAExpectation(Region region) {
		// Dampening or multiplier factor, depending on its value being <1 or >1, for the current trend of HPA when
		// computing expectations as in HPI(t+DT) = HPI(t) + FACTOR*DT*dHPI/dt (double)
		return(region.regionalHousingMarketStats.getLongTermHPA()*config.HPA_EXPECTATION_FACTOR);
    }

    public double getBTLCapGainCoefficient() { return BTLCapGainCoefficient; }

    public boolean isPropertyInvestor() { return BTLInvestor; }

    double getPropensityToSave() { return propensityToSave; }
}
