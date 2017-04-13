package com.mozartalouis.stockhawk.models;

public class ChartModel {
    private final float mHistory;
    private final float mPrice;

    public ChartModel(final float history, final float price) {
        this.mHistory = history;
        this.mPrice = price;
    }

    public float getHistory() {return mHistory;}

    public float getPrice() {
        return mPrice;
    }
}
