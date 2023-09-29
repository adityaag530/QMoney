
package com.crio.warmup.stock.portfolio;


import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {



  private RestTemplate restTemplate;

  public static String token = "token_key";

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF



  // private Comparator<AnnualizedReturn> getComparator() {
  //   return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  // }

  // CHECKSTYLE:OFF

  // CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    // RestTemplate restTemplate = new RestTemplate();
    if (from.compareTo(to) >= 0) {
      throw new RuntimeException();
    }
    String uri = buildUri(symbol, from, to);
    TiingoCandle[] stocksStartToEndDate = restTemplate.getForObject(uri, TiingoCandle[].class);
    if (stocksStartToEndDate == null) {
      return new ArrayList<Candle>();
    } else {
      List<Candle> stocks = Arrays.asList(stocksStartToEndDate);
      return stocks;
    }

  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    // String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
    // + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    // return uriTemplate;
    String url = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate="
        + startDate.toString() + "&endDate=" + endDate.toString() + "&token=" + token;

    return url;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {

    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    for (int i = 0; i < portfolioTrades.size(); i++) {
      annualizedReturn = getAnnulizedReturn(portfolioTrades.get(i), endDate);
      annualizedReturns.add(annualizedReturn);
    }

    Comparator<AnnualizedReturn> SortByAnnReturn =
        Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();

    Collections.sort(annualizedReturns, SortByAnnReturn);
    return annualizedReturns;

    // List<AnnualizedReturn> AR = new ArrayList<>();
    // for (PortfolioTrade trade : portfolioTrades) {
    // List<Candle> candles;
    // try {
    // candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
    // double totalReturn = (candles.get(candles.size() - 1).getClose() - candles.get(0).getOpen())
    // / candles.get(0).getOpen();
    // double years = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.25;
    // double time = 1 / years;
    // double annualizedReturn = Math.pow((1 + totalReturn), time) - 1;
    // AR.add(new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn));
    // } catch (JsonProcessingException e) {
    // e.printStackTrace();
    // }

    // }
    // Collections.sort(AR, getComparator());
    // return AR;
  }


  private AnnualizedReturn getAnnulizedReturn(PortfolioTrade trade, LocalDate endLocalDate) {

    AnnualizedReturn annualizedReturn;
    String symbol = trade.getSymbol();
    LocalDate startLocalDate = trade.getPurchaseDate();
    try {

      List<Candle> stocksStartToEndDate;

      stocksStartToEndDate = getStockQuote(symbol, startLocalDate, endLocalDate);

      Candle stockStartDate = stocksStartToEndDate.get(0);
      Candle stocksEndDate = stocksStartToEndDate.get(stocksStartToEndDate.size() - 1);

      Double buyPrice = stockStartDate.getOpen();
      Double sellPrice = stocksEndDate.getClose();

      Double totalReturn = (sellPrice - buyPrice) / buyPrice;

      Double numYears = (double) ChronoUnit.DAYS.between(startLocalDate, endLocalDate) / 365;

      Double annualizedReturns = Math.pow((1 + totalReturn), (1 / numYears)) - 1;
      annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn);

    } catch (JsonProcessingException e) {

      annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
    }
    return annualizedReturn;
  }
}
