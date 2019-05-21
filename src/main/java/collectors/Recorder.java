package collectors;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import housing.Geography;
import housing.Model;
import housing.Region;

/**************************************************************************************************
 * Class to write output to files
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class Recorder {

    //------------------//
    //----- Fields -----//
    //------------------//

    private String outputFolder;
    private Geography geography;

    private PrintWriter outfile;
    private PrintWriter qualityBandPriceFile;
    private PrintWriter [] regionalOutfiles;

    private PrintWriter ooLTI;
    private PrintWriter btlLTV;
    private PrintWriter creditGrowth;
    private PrintWriter debtToIncome;
    private PrintWriter ooDebtToIncome;
    private PrintWriter mortgageApprovals;
    private PrintWriter housingTransactions;
    private PrintWriter advancesToFTBs;
    private PrintWriter advancesToBTL;
    private PrintWriter advancesToHomeMovers;
    private PrintWriter priceToIncome;
    private PrintWriter rentalYield;
    private PrintWriter housePriceGrowth;
    private PrintWriter interestRateSpread;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    public Recorder(String outputFolder, Geography geography) {
        this.outputFolder = outputFolder;
        this.geography = geography;
        regionalOutfiles = new PrintWriter[geography.getRegions().size()];
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public void openMultiRunFiles(boolean recordCoreIndicators) {
        // If recording of core indicators is active...
        if(recordCoreIndicators) {
            // ...try opening necessary files
            try {
                ooLTI = new PrintWriter(outputFolder + "coreIndicator-ooLTI.csv",
                        "UTF-8");
                btlLTV = new PrintWriter(outputFolder + "coreIndicator-btlLTV.csv",
                        "UTF-8");
                creditGrowth = new PrintWriter(outputFolder + "coreIndicator-creditGrowth.csv",
                        "UTF-8");
                debtToIncome = new PrintWriter(outputFolder + "coreIndicator-debtToIncome.csv",
                        "UTF-8");
                ooDebtToIncome = new PrintWriter(outputFolder + "coreIndicator-ooDebtToIncome.csv",
                        "UTF-8");
                mortgageApprovals = new PrintWriter(outputFolder + "coreIndicator-mortgageApprovals.csv",
                        "UTF-8");
                housingTransactions = new PrintWriter(outputFolder + "coreIndicator-housingTransactions.csv",
                        "UTF-8");
                advancesToFTBs = new PrintWriter(outputFolder + "coreIndicator-advancesToFTB.csv",
                        "UTF-8");
                advancesToBTL = new PrintWriter(outputFolder + "coreIndicator-advancesToBTL.csv",
                        "UTF-8");
                advancesToHomeMovers = new PrintWriter(outputFolder + "coreIndicator-advancesToMovers.csv",
                        "UTF-8");
                priceToIncome = new PrintWriter(outputFolder + "coreIndicator-priceToIncome.csv",
                        "UTF-8");
                rentalYield = new PrintWriter(outputFolder + "coreIndicator-rentalYield.csv",
                        "UTF-8");
                housePriceGrowth = new PrintWriter(outputFolder + "coreIndicator-housePriceGrowth.csv",
                        "UTF-8");
                interestRateSpread = new PrintWriter(outputFolder + "coreIndicator-interestRateSpread.csv",
                        "UTF-8");
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void openSingleRunFiles(int nRun, boolean recordQualityBandPrice, int nQualityBands) {
        // Try opening output files (national and for each region) and write first row header with column names
        try {
            outfile = new PrintWriter(outputFolder + "Output-run" + nRun + ".csv", "UTF-8");
            outfile.println("Model time, "
                    // Number of households of each type
                    + "nNonBTLHomeless, nBTLHomeless, nHomeless, nRenting, nNonOwner, "
                    + "nNonBTLOwnerOccupier, nBTLOwnerOccupier, nOwnerOccupier, nActiveBTL, nBTL, nNonBTLBankrupt, "
                    + "nBTLBankrupt, TotalPopulation, "
                    // Numbers of houses of each type
                    + "HousingStock, nNewBuild, nUnsoldNewBuild, nEmptyHouses, BTLStockFraction, "
                    // House sale market data
                    + "Sale HPI, Sale AnnualHPA, Sale AvBidPrice, Sale AvOfferPrice, Sale AvSalePrice, "
                    + "Sale ExAvSalePrice, Sale AvMonthsOnMarket, Sale ExpAvMonthsOnMarket, Sale nBuyers, "
                    + "Sale nBTLBuyers, Sale nSellers, Sale nNewSellers, Sale nBTLSellers, Sale nSales, "
                    + "Sale nNonBTLBidsAboveExpAvSalePrice, Sale nBTLBidsAboveExpAvSalePrice, Sale nSalesToBTL, "
                    + "Sale nSalesToFTB, "
                    // Rental market data
                    + "Rental HPI, Rental AnnualHPA, Rental AvBidPrice, Rental AvOfferPrice, Rental AvSalePrice, "
                    + "Rental AvMonthsOnMarket, Rental ExpAvMonthsOnMarket, Rental nBuyers, Rental nSellers, "
                    + "Rental nSales, Rental ExpAvFlowYield, "
                    // Credit data
                    + "nRegisteredMortgages, interestRate, "
                    // Commuting data
                    + "nCommuters, sumCommutingFees, sumCommutingCost");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < geography.getRegions().size(); i++) {
            try {
                regionalOutfiles[i] = new PrintWriter(outputFolder + "Output-region" + i + "-run" + nRun + ".csv",
                        "UTF-8");
                regionalOutfiles[i].println("Model time, "
                        // Number of households of each type
                        + "nNonBTLHomeless, nBTLHomeless, nHomeless, nRenting, nNonOwner, "
                        + "nNonBTLOwnerOccupier, nBTLOwnerOccupier, nOwnerOccupier, nActiveBTL, nBTL, nNonBTLBankrupt, "
                        + "nBTLBankrupt, TotalPopulation, "
                        // Numbers of houses of each type
                        + "HousingStock, nNewBuild, nUnsoldNewBuild, nEmptyHouses, BTLStockFraction, "
                        // House sale market data
                        + "Sale HPI, Sale AnnualHPA, Sale AvBidPrice, Sale AvOfferPrice, Sale AvSalePrice, "
                        + "Sale ExAvSalePrice, Sale AvMonthsOnMarket, Sale ExpAvMonthsOnMarket, Sale nBuyers, "
                        + "Sale nBTLBuyers, Sale nSellers, Sale nNewSellers, Sale nBTLSellers, Sale nSales, "
                        + "Sale nNonBTLBidsAboveExpAvSalePrice, Sale nBTLBidsAboveExpAvSalePrice, Sale nSalesToBTL, "
                        + "Sale nSalesToFTB, "
                        // Rental market data
                        + "Rental HPI, Rental AnnualHPA, Rental AvBidPrice, Rental AvOfferPrice, Rental AvSalePrice, "
                        + "Rental AvMonthsOnMarket, Rental ExpAvMonthsOnMarket, Rental nBuyers, Rental nSellers, "
                        + "Rental nSales, Rental ExpAvFlowYield, "
                        // Commuting data
                        + "nCommuters, sumCommutingFees, sumCommutingCost");
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // If recording of quality band prices is active...
        if(recordQualityBandPrice) {
            // ...try opening output file and write first row header with column names
            try {
                qualityBandPriceFile = new PrintWriter(outputFolder + "QualityBandPrice-run" + nRun + ".csv", "UTF-8");
                StringBuilder str = new StringBuilder();
                str.append(String.format("Time, Q%d", 0));
                for (int i = 1; i < nQualityBands; i++) {
                    str.append(String.format(", Q%d", i));
                }
                qualityBandPriceFile.println(str);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeTimeStampResults(boolean recordCoreIndicators, int time, boolean recordQualityBandPrice) {
        if (recordCoreIndicators) {
            // If not at the first point in time...
            if (time > 0) {
                // ...write value separation for core indicators (except for time 0)
                ooLTI.print(", ");
                btlLTV.print(", ");
                creditGrowth.print(", ");
                debtToIncome.print(", ");
                ooDebtToIncome.print(", ");
                mortgageApprovals.print(", ");
                housingTransactions.print(", ");
                advancesToFTBs.print(", ");
                advancesToBTL.print(", ");
                advancesToHomeMovers.print(", ");
                priceToIncome.print(", ");
                rentalYield.print(", ");
                housePriceGrowth.print(", ");
                interestRateSpread.print(", ");
            }
            // Write core indicators results
            ooLTI.print(Model.coreIndicators.getOwnerOccupierLTIMeanAboveMedian());
            btlLTV.print(Model.coreIndicators.getBuyToLetLTVMean());
            creditGrowth.print(Model.coreIndicators.getHouseholdCreditGrowth());
            debtToIncome.print(Model.coreIndicators.getDebtToIncome());
            ooDebtToIncome.print(Model.coreIndicators.getOODebtToIncome());
            mortgageApprovals.print(Model.coreIndicators.getMortgageApprovals());
            housingTransactions.print(Model.coreIndicators.getHousingTransactions());
            advancesToFTBs.print(Model.coreIndicators.getAdvancesToFTBs());
            advancesToBTL.print(Model.coreIndicators.getAdvancesToBTL());
            advancesToHomeMovers.print(Model.coreIndicators.getAdvancesToHomeMovers());
            priceToIncome.print(Model.coreIndicators.getPriceToIncome());
            rentalYield.print(Model.coreIndicators.getAvStockYield());
            housePriceGrowth.print(Model.coreIndicators.getQoQHousePriceGrowth());
            interestRateSpread.print(Model.coreIndicators.getInterestRateSpread());
        }

        // Write general output results to output file
        outfile.println(time + ", " +
                // Number of households of each type
                Model.householdStats.getnNonBTLHomeless() + ", " +
                Model.householdStats.getnBTLHomeless() + ", " +
                Model.householdStats.getnHomeless() + ", " +
                Model.householdStats.getnRenting() + ", " +
                Model.householdStats.getnNonOwner() + ", " +
                Model.householdStats.getnNonBTLOwnerOccupier() + ", " +
                Model.householdStats.getnBTLOwnerOccupier() + ", " +
                Model.householdStats.getnOwnerOccupier() + ", " +
                Model.householdStats.getnActiveBTL() + ", " +
                Model.householdStats.getnBTL() + ", " +
                Model.householdStats.getnNonBTLBankruptcies() + ", " +
                Model.householdStats.getnBTLBankruptcies() + ", " +
                Model.demographics.getTotalPopulation() + ", " +
                // Numbers of houses of each type
                Model.construction.getHousingStock() + ", " +
                Model.construction.getnNewBuild() + ", " +
                Model.housingMarketStats.getnUnsoldNewBuild() + ", " +
                Model.householdStats.getnEmptyHouses() + ", " +
                Model.householdStats.getBTLStockFraction() + ", " +
                // House sale market data
                Model.housingMarketStats.getHPI() + ", " +
                Model.housingMarketStats.getAnnualHPA() + ", " +
                Model.housingMarketStats.getAvBidPrice() + ", " +
                Model.housingMarketStats.getAvOfferPrice() + ", " +
                Model.housingMarketStats.getAvSalePrice() + ", " +
                Model.housingMarketStats.getExpAvSalePrice() + ", " +
                Model.housingMarketStats.getAvMonthsOnMarket() + ", " +
                Model.housingMarketStats.getExpAvMonthsOnMarket() + ", " +
                Model.housingMarketStats.getnBuyers() + ", " +
                Model.housingMarketStats.getnBTLBuyers() + ", " +
                Model.housingMarketStats.getnSellers() + ", " +
                Model.housingMarketStats.getnNewSellers() + ", " +
                Model.housingMarketStats.getnBTLSellers() + ", " +
                Model.housingMarketStats.getnSales() + ", " +
                Model.householdStats.getnNonBTLBidsAboveExpAvSalePrice() + ", " +
                Model.householdStats.getnBTLBidsAboveExpAvSalePrice() + ", " +
                Model.housingMarketStats.getnSalesToBTL() + ", " +
                Model.housingMarketStats.getnSalesToFTB() + ", " +
                // Rental market data
                Model.rentalMarketStats.getHPI() + ", " +
                Model.rentalMarketStats.getAnnualHPA() + ", " +
                Model.rentalMarketStats.getAvBidPrice() + ", " +
                Model.rentalMarketStats.getAvOfferPrice() + ", " +
                Model.rentalMarketStats.getAvSalePrice() + ", " +
                Model.rentalMarketStats.getAvMonthsOnMarket() + ", " +
                Model.rentalMarketStats.getExpAvMonthsOnMarket() + ", " +
                Model.rentalMarketStats.getnBuyers() + ", " +
                Model.rentalMarketStats.getnSellers() + ", " +
                Model.rentalMarketStats.getnSales() + ", " +
                Model.rentalMarketStats.getExpAvFlowYield() + ", " +
                // Credit data
                Model.creditSupply.getnRegisteredMortgages() + ", " +
                Model.creditSupply.getInterestRate() + ", " +
                // Commuting data
                Model.householdStats.getnCommuters() + ", " +
                Model.householdStats.getSumCommutingFees() + ", " +
                Model.householdStats.getSumCommutingCost());

        // Write general output results for each region
        int i = 0;
        for (Region region: geography.getRegions()) {
            regionalOutfiles[i].println(time + ", " +
                    // Number of households of each type
                    region.regionalHouseholdStats.getnNonBTLHomeless() + ", " +
                    region.regionalHouseholdStats.getnBTLHomeless() + ", " +
                    region.regionalHouseholdStats.getnHomeless() + ", " +
                    region.regionalHouseholdStats.getnRenting() + ", " +
                    region.regionalHouseholdStats.getnNonOwner() + ", " +
                    region.regionalHouseholdStats.getnNonBTLOwnerOccupier() + ", " +
                    region.regionalHouseholdStats.getnBTLOwnerOccupier() + ", " +
                    region.regionalHouseholdStats.getnOwnerOccupier() + ", " +
                    region.regionalHouseholdStats.getnActiveBTL() + ", " +
                    region.regionalHouseholdStats.getnBTL() + ", " +
                    region.regionalHouseholdStats.getnNonBTLBankruptcies() + ", " +
                    region.regionalHouseholdStats.getnBTLBankruptcies() + ", " +
                    region.households.size() + ", " +
                    // Numbers of houses of each type
                    region.getHousingStock() + ", " +
                    Model.construction.getnNewBuildForRegion(region) + ", " +
                    region.regionalHousingMarketStats.getnUnsoldNewBuild() + ", " +
                    region.regionalHouseholdStats.getnEmptyHouses() + ", " +
                    region.regionalHouseholdStats.getBTLStockFraction() + ", " +
                    // House sale market data
                    region.regionalHousingMarketStats.getHPI() + ", " +
                    region.regionalHousingMarketStats.getAnnualHPA() + ", " +
                    region.regionalHousingMarketStats.getAvBidPrice() + ", " +
                    region.regionalHousingMarketStats.getAvOfferPrice() + ", " +
                    region.regionalHousingMarketStats.getAvSalePrice() + ", " +
                    region.regionalHousingMarketStats.getExpAvSalePrice() + ", " +
                    region.regionalHousingMarketStats.getAvMonthsOnMarket() + ", " +
                    region.regionalHousingMarketStats.getExpAvMonthsOnMarket() + ", " +
                    region.regionalHousingMarketStats.getnBuyers() + ", " +
                    region.regionalHousingMarketStats.getnBTLBuyers() + ", " +
                    region.regionalHousingMarketStats.getnSellers() + ", " +
                    region.regionalHousingMarketStats.getnNewSellers() + ", " +
                    region.regionalHousingMarketStats.getnBTLSellers() + ", " +
                    region.regionalHousingMarketStats.getnSales() + ", " +
                    region.regionalHouseholdStats.getnNonBTLBidsAboveExpAvSalePrice() + ", " +
                    region.regionalHouseholdStats.getnBTLBidsAboveExpAvSalePrice() + ", " +
                    region.regionalHousingMarketStats.getnSalesToBTL() + ", " +
                    region.regionalHousingMarketStats.getnSalesToFTB() + ", " +
                    // Rental market data
                    region.regionalRentalMarketStats.getHPI() + ", " +
                    region.regionalRentalMarketStats.getAnnualHPA() + ", " +
                    region.regionalRentalMarketStats.getAvBidPrice() + ", " +
                    region.regionalRentalMarketStats.getAvOfferPrice() + ", " +
                    region.regionalRentalMarketStats.getAvSalePrice() + ", " +
                    region.regionalRentalMarketStats.getAvMonthsOnMarket() + ", " +
                    region.regionalRentalMarketStats.getExpAvMonthsOnMarket() + ", " +
                    region.regionalRentalMarketStats.getnBuyers() + ", " +
                    region.regionalRentalMarketStats.getnSellers() + ", " +
                    region.regionalRentalMarketStats.getnSales() + ", " +
                    region.regionalRentalMarketStats.getExpAvFlowYield() + ", " +
                    // Commuting data
                    region.regionalHouseholdStats.getnCommuters() + ", " +
                    region.regionalHouseholdStats.getSumCommutingFees() + ", " +
                    region.regionalHouseholdStats.getSumCommutingCost());
            i++;
        }

        // Write quality band prices to file
        if (recordQualityBandPrice) {
            String str = Arrays.toString(Model.housingMarketStats.getAvSalePricePerQuality());
            str = str.substring(1, str.length() - 1);
            qualityBandPriceFile.println(time + ", " + str);
        }
    }

    public void finishRun(boolean recordCoreIndicators, boolean recordQualityBandPrice) {
        if (recordCoreIndicators) {
            ooLTI.println("");
            btlLTV.println("");
            creditGrowth.println("");
            debtToIncome.println("");
            ooDebtToIncome.println("");
            mortgageApprovals.println("");
            housingTransactions.println("");
            advancesToFTBs.println("");
            advancesToBTL.println("");
            advancesToHomeMovers.println("");
            priceToIncome.println("");
            rentalYield.println("");
            housePriceGrowth.println("");
            interestRateSpread.println("");
        }
        outfile.close();
        for (int i = 0; i < geography.getRegions().size(); i++) {
            regionalOutfiles[i].close();
        }
        if (recordQualityBandPrice) {
            qualityBandPriceFile.close();
        }
    }

    public void finish(boolean recordCoreIndicators) {
        if (recordCoreIndicators) {
            ooLTI.close();
            btlLTV.close();
            creditGrowth.close();
            debtToIncome.close();
            ooDebtToIncome.close();
            mortgageApprovals.close();
            housingTransactions.close();
            advancesToFTBs.close();
            advancesToBTL.close();
            advancesToHomeMovers.close();
            priceToIncome.close();
            rentalYield.close();
            housePriceGrowth.close();
            interestRateSpread.close();
        }
    }
}
