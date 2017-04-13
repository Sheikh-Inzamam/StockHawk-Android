package com.mozartalouis.stockhawk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mozartalouis.stockhawk.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PrefUtils {

    public static Set<String> getStocks(Context context) {
        final String stocksKey = context.getString(R.string.pref_stocks_key);
        final String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        final HashSet<String> defaultStocks = new HashSet<>(Arrays.asList
                (context.getResources().getStringArray(R.array.default_stocks)));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }
        return prefs.getStringSet(stocksKey, new HashSet<String>());
    }

    private static void editStockPref(Context context, String symbol, Boolean add) {
        final String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks = getStocks(context);

        if (add) stocks.add(symbol);
        else stocks.remove(symbol);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        // Since were saving a new set, it is better to remove then re-save.
        editor.remove(key);
        editor.apply();
        editor.putStringSet(key, stocks);
        editor.apply();
    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context) {
        final String key = context.getString(R.string.pref_display_mode_key);
        final String defaultValue = context.getString(R.string.pref_display_mode_default);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context) {
        final String key = context.getString(R.string.pref_display_mode_key);
        final String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        final String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);
        final String displayMode = getDisplayMode(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) editor.putString(key, percentageKey);
        else editor.putString(key, absoluteKey);

        editor.apply();
    }
}
