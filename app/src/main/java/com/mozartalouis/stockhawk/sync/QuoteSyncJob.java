package com.mozartalouis.stockhawk.sync;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.mozartalouis.stockhawk.R;
import com.mozartalouis.stockhawk.data.Contract;
import com.mozartalouis.stockhawk.utils.PrefUtils;
import com.mozartalouis.stockhawk.utils.NetworkUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import static android.os.Looper.getMainLooper;

@SuppressLint("StringFormatInvalid")
public final class QuoteSyncJob {

    // Used to notify widget with stock data updates
    public static final String ACTION_DATA_UPDATED = "com.mozartalouis.stockhawk.ACTION_DATA_UPDATED";

    // Used for immediate sync functionality
    private static final int INITIAL_BACKOFF = 10000;
    private static final int ONE_OFF_ID = 2;

    // Used to schedules updates on stock data
    private static final int PERIODIC_ID = 1;
    private static final int PERIOD = 300000;

    // Used to obtain historical data.
    private static final int YEARS_OF_HISTORY = 2;

    /**
     * Initializes sync jobs on runtime.
     *
     * @param context Activity context
     */
    public synchronized static void initialize(final Context context) {
        schedulePeriodic(context);
        syncImmediately(context);
    }

    /**
     * Syncs stock data as soon as it can. This is used to update the stock data at any given \
     * moment.
     *
     * @param context Activity context
     */
    public synchronized static void syncImmediately(Context context) {
        if (NetworkUtils.hasInternetConnection(context)) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }

    /**
     * Schedules a periodic sync for background syncing of data. This will run every 5 miniutes.
     * {@link QuoteSyncJob##PERIOD}
     *
     * @param context Activity context
     */
    private synchronized static void schedulePeriodic(Context context) {
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    static void getQuotes(Context context) {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);


        Set<String> stockPref = PrefUtils.getStocks(context);
        Set<String> stockCopy = new HashSet<>();
        stockCopy.addAll(stockPref);
        String[] stockArray = stockPref.toArray(new String[stockPref.size()]);
        if (stockArray.length == 0)
            return;


        Map<String, Stock> quotes;
        try {
            quotes = YahooFinance.get(stockArray);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        Iterator<String> iterator = stockCopy.iterator();
        ArrayList<ContentValues> quoteCVs = new ArrayList<>();

        while (iterator.hasNext()) {
            String symbol = iterator.next();
            Stock stock = quotes.get(symbol);
            StockQuote quote;
            float change;
            float price;
            float percentChange;
            try {
                quote = stock.getQuote();
                price = quote.getPrice().floatValue();
                change = quote.getChange().floatValue();
                percentChange = quote.getChangeInPercent().floatValue();

            } catch (NullPointerException exception) {
                showErrorOnMainThread(context, symbol);
                // We want to make sure that when a stock is not found, We remove it from
                // our preferences so that were not holding onto an invalid stock.
                PrefUtils.removeStock(context, symbol);
                break;
            }

            try {
                // If we've made it this far, We know the stock exists, so let's try to get some
                // historical data on it!
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);
                StringBuilder historyBuilder = new StringBuilder();
                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                // Store all that data in the content provider
                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());
                quoteCVs.add(quoteCV);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        context.getContentResolver()
                .bulkInsert(
                        Contract.Quote.uri,
                        quoteCVs.toArray(new ContentValues[quoteCVs.size()]));
        updateWidget(context);
    }

    public static void updateWidget(Context context) {
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
        context.sendBroadcast(dataUpdatedIntent);
    }

    /**
     * Shows a toast on the UI thread (Main Thread) indicating that the stock does not exist.
     *
     * @param context Activity context
     * @param symbol  Stock name
     */
    private static void showErrorOnMainThread(final Context context, final String symbol) {
        Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, String.format(
                        context.getString(R.string.toast_stock_invalid),
                        symbol), Toast.LENGTH_LONG).show();
            }
        });
    }
}
