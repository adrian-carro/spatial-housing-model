package collectors;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import housing.Config;
import housing.Model;

/***
 * For recording output to file
 * @author daniel, Adrian Carro
 *
 */
public class Recorder {

    private Config config = Model.config;    // Passes the Model's configuration parameters object to a private field

    private boolean newSim = true;
    private String outputFolderCopy;

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

    private PrintWriter outfile;
    private PrintWriter paramfile;

    public Recorder(String outputFolder) {
        outputFolderCopy = outputFolder;
    }
    
    public void start() throws FileNotFoundException, UnsupportedEncodingException {
        // --- open files for core indicators
        ooLTI = new PrintWriter(outputFolderCopy + "coreIndicator-ooLTI.csv", "UTF-8");
        btlLTV = new PrintWriter(outputFolderCopy + "coreIndicator-btlLTV.csv", "UTF-8");
        creditGrowth = new PrintWriter(outputFolderCopy + "coreIndicator-creditGrowth.csv", "UTF-8");
        debtToIncome = new PrintWriter(outputFolderCopy + "coreIndicator-debtToIncome.csv", "UTF-8");
        ooDebtToIncome = new PrintWriter(outputFolderCopy + "coreIndicator-ooDebtToIncome.csv", "UTF-8");
        mortgageApprovals = new PrintWriter(outputFolderCopy + "coreIndicator-mortgageApprovals.csv", "UTF-8");
        housingTransactions = new PrintWriter(outputFolderCopy + "coreIndicator-housingTransactions.csv", "UTF-8");
        advancesToFTBs = new PrintWriter(outputFolderCopy + "coreIndicator-advancesToFTB.csv", "UTF-8");
        advancesToBTL = new PrintWriter(outputFolderCopy + "coreIndicator-advancesToBTL.csv", "UTF-8");
        advancesToHomeMovers = new PrintWriter(outputFolderCopy + "coreIndicator-advancesToMovers.csv", "UTF-8");
        priceToIncome = new PrintWriter(outputFolderCopy + "coreIndicator-priceToIncome.csv", "UTF-8");
        rentalYield = new PrintWriter(outputFolderCopy + "coreIndicator-rentalYield.csv", "UTF-8");
        housePriceGrowth = new PrintWriter(outputFolderCopy + "coreIndicator-housePriceGrowth.csv", "UTF-8");
        interestRateSpread = new PrintWriter(outputFolderCopy + "coreIndicator-interestRateSpread.csv", "UTF-8");
        
        newSim = true;
    }

    public void step() {
        if(newSim) {
//            String simID = Integer.toHexString(UUID.randomUUID().hashCode());
            try {
                outfile = new PrintWriter(outputFolderCopy + "output-"+Model.nSimulation+".csv", "UTF-8");
                outfile.println(
                        "Model time, NRegisteredMortgages, nBTL(gene), nEmpty, nHomeless, nHouseholds, nRenting, AverageBidPrice, "+
                        "AverageDaysOnMarket, AverageOfferPrice, BTLSalesProportion, FTBSalesProportion, HPA, HPI, nBuyers, "+
                        "nSellers, nSales, nNewBuild, Rental AverageBidPrice, Rental AverageDaysOnMarket, Rental AverageOfferPrice, Rental HPA, Rental HPI, "+
                        "Rental nBuyers, Rental nSellers, Rental nSales, averageNewRentalGrossYield, nBTL(active), ProportionOfHousingStockBTL");
                paramfile = new PrintWriter(outputFolderCopy + "parameters-"+Model.nSimulation+".csv", "UTF-8");
                paramfile.println("BTL P_INVESTOR, CentralBank ICR Limit");
                paramfile.println(
                        config.getPInvestor()+", "+
                        Model.centralBank.interestCoverRatioLimit
                );
                paramfile.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // write parameters
            paramfile.close();
            newSim = false;
        } else {
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
        
        outfile.println(
                Model.getTime()+", "+
                Model.creditSupply.getNRegisteredMortgages()+", "+
                Model.householdStats.getnBTL()+", "+
                Model.householdStats.getnEmpty()+", "+
                Model.householdStats.getnHomeless()+", "+
                Model.demographics.getTotalPopulation()+", "+
                Model.householdStats.getnRenting()+", "+
                Model.housingMarketStats.getAvBidPrice()+", "+
                Model.housingMarketStats.getAvDaysOnMarket()+", "+
                Model.housingMarketStats.getAvOfferPrice()+", "+
                Model.housingMarketStats.getBTLSalesProportion()+", "+
                Model.housingMarketStats.getFTBSalesProportion()+", "+
                Model.housingMarketStats.getAnnualHPA()+", "+
                Model.housingMarketStats.getHPI()+", "+
                Model.housingMarketStats.getnBuyers()+", "+
                Model.housingMarketStats.getnSellers()+", "+
                Model.housingMarketStats.getnSales()+", "+
                Model.housingMarketStats.getnNewBuild()+", "+
                Model.rentalMarketStats.getAvBidPrice()+", "+
                Model.rentalMarketStats.getAvDaysOnMarket()+", "+
                Model.rentalMarketStats.getAvOfferPrice()+", "+
                Model.rentalMarketStats.getAnnualHPA()+", "+
                Model.rentalMarketStats.getHPI()+", "+
                Model.rentalMarketStats.getnBuyers()+", "+
                Model.rentalMarketStats.getnSellers()+", "+
                Model.rentalMarketStats.getnSales()+", "+
                Model.rentalMarketStats.getExpAvFlowYield()+", "+
                Model.householdStats.getnActiveBTL()+", "+
                Model.householdStats.getBTLProportion());
    }
    
    public void finish() {
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
        if(outfile != null) outfile.close();
    }
        
    public void endOfSim() {
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
        outfile.close();
        newSim = true;
    }

}
