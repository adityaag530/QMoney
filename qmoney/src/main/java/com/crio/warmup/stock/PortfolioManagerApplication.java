
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  // TILL MOD 3
  public static String getToken() {
    return "5068b12108c01c8e8455478f6b2d203693a936ee";
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    List<PortfolioTrade> trades = readTradesFromJson(args[0]);
    LocalDate date = LocalDate.parse(args[1]);
    List<String> quotes = new ArrayList<>();
    List<TotalReturnsDto> totalReturns = new ArrayList<>();
    for (PortfolioTrade trade : trades) {
      String url = prepareUrl(trade, date, getToken());
      RestTemplate restTemplate = new RestTemplate();
      TiingoCandle[] tiingoCandle = restTemplate.getForObject(url, TiingoCandle[].class);
      totalReturns.add(
          new TotalReturnsDto(trade.getSymbol(), tiingoCandle[tiingoCandle.length - 1].getClose()));
    }
    Collections.sort(totalReturns, new compareByClosingPrice());
    for (TotalReturnsDto tot : totalReturns) {
      quotes.add(tot.getSymbol());
    }
    return quotes;
  }

  public static List<PortfolioTrade> readTradesFromJson(String filename)
      throws IOException, URISyntaxException {

    File file = Paths
        .get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
    ObjectMapper objMap = new ObjectMapper();
    PortfolioTrade[] trades = objMap.readValue(file, PortfolioTrade[].class);

    List<PortfolioTrade> listOfTrades = new ArrayList<PortfolioTrade>();
    for (PortfolioTrade trade : trades) {
      listOfTrades.add(trade);
    }

    return listOfTrades;
  }


  public static List<String> mainReadFile(String[] strings) throws IOException, URISyntaxException {
    File file =
        Paths.get(Thread.currentThread().getContextClassLoader().getResource(strings[0]).toURI())
            .toFile();
    ObjectMapper om = new ObjectMapper();
    PortfolioTrade[] trades = om.readValue(file, PortfolioTrade[].class);
    List<String> listOfSymbols = new ArrayList<>();
    for (PortfolioTrade trade : trades) {
      listOfSymbols.add(trade.getSymbol());
    }
    return listOfSymbols;
  }

  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

    String url = "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
        + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;

    return url;
  }


  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }

  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    RestTemplate restTemplate = new RestTemplate();
    List<Candle> candles = Arrays
        .asList(restTemplate.getForObject(prepareUrl(trade, endDate, token), TiingoCandle[].class));
    return candles;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;
    // int years = endDate.getYear() - trade.getPurchaseDate().getYear();
    double years = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.25;
    double time = 1 / years;
    double annualizedReturn = Math.pow((1 + totalReturn), time) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }


  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    List<AnnualizedReturn> AR = new ArrayList<>();
    List<PortfolioTrade> trades = readTradesFromJson(args[0]);
    for (PortfolioTrade trade : trades) {
      List<Candle> candles = fetchCandles(trade, LocalDate.parse(args[1]), getToken());
      AR.add(calculateAnnualizedReturns(LocalDate.parse(args[1]), trade,
          getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles)));
    }

    Collections.sort(AR, new sortByAnnualizedReturns());
    return AR;
  }


  public static List<String> debugOutputs() {
    List<String> res = new ArrayList<String>();
    res.add("trades.json");
    res.add("trades.json");
    res.add("ObjectMapper");
    res.add("mainReadFile");
    return res;
  }
// TILL MOD 3


// PART OF MOD 4
  // CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  // call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static RestTemplate restTemplate = new RestTemplate();
  public static PortfolioManager portfolioManager =
      PortfolioManagerFactory.getPortfolioManager(restTemplate);

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  private static String readFileAsString(String file) throws IOException {
    Path path = Paths.get(file);
    String strContent = Files.readString(path);
    return strContent;
  }


  private static ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }



  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());



    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }


  private static void printJsonObject(List<AnnualizedReturn> mainCalculateReturnsAfterRefactor) {
    for (AnnualizedReturn annRtn : mainCalculateReturnsAfterRefactor) {
      System.out.println(annRtn);
    }
  }
// PART OF MOD 4
}

//PART OF MOD 3
class compareByClosingPrice implements Comparator<TotalReturnsDto> {

  @Override
  public int compare(TotalReturnsDto arg0, TotalReturnsDto arg1) {
    double sum = arg0.getClosingPrice() - arg1.getClosingPrice();
    return (int) sum;
  }

}


class sortByAnnualizedReturns implements Comparator<AnnualizedReturn> {

  @Override
  public int compare(AnnualizedReturn arg0, AnnualizedReturn arg1) {
    double sum = (arg1.getAnnualizedReturn() - arg0.getAnnualizedReturn()) * 100;
    return (int) sum;
  }
}
// PART OF MOD 3


