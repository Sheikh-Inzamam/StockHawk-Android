package com.mozartalouis.stockhawk.widget;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mozartalouis.stockhawk.R;
import com.mozartalouis.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewFactory();
    }

    private class ListRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private Cursor data = null;

        @Override
        public void onCreate() {
        }

        @Override
        public void onDestroy() {
            if (data != null) {
                data.close();
                data = null;
            }
        }

        @Override
        public void onDataSetChanged() {
            if (data != null) data.close();
            final long identityToken = Binder.clearCallingIdentity();
            data = getContentResolver().query(Contract.Quote.uri,
                    Contract.Quote.QUOTE_COLUMNS,
                    null,
                    null,
                    Contract.Quote.COLUMN_SYMBOL);
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public int getCount() {
            return data == null ? 0 : data.getCount();
        }

        @SuppressLint("PrivateResource")
        @Override
        public RemoteViews getViewAt(int position) {
            if (position == AdapterView.INVALID_POSITION || data == null ||
                    !data.moveToPosition(position))
                return null;

            // Initialize all vars for widget display
            final String stockSymbol = data.getString(Contract.Quote.POSITION_SYMBOL);
            final Float absoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);

            final DecimalFormat dollarFormat = (DecimalFormat)
                    NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormat.setMaximumFractionDigits(2);
            dollarFormat.setMinimumFractionDigits(2);

            final DecimalFormat dollarFormatWithPlus = (DecimalFormat)
                    NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+");
            dollarFormatWithPlus.setMaximumFractionDigits(2);
            dollarFormatWithPlus.setMinimumFractionDigits(2);

            // Initialize remote view.
            RemoteViews remoteViews = new RemoteViews(getPackageName(),
                    R.layout.widget_item_stock_quote);
            remoteViews.setTextViewText(R.id.widget_symbol, stockSymbol);
            remoteViews.setTextViewText(R.id.widget_price, dollarFormat.format
                    (data.getFloat(Contract.Quote.POSITION_PRICE)));
            remoteViews.setTextViewText(R.id.widget_change, dollarFormatWithPlus.format(absoluteChange));
            remoteViews.setInt(R.id.widget_change, "setBackgroundResource", absoluteChange >= 0 ?
                    R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red);

            //TODO Create an intent for the given item to open up the StockDetailActivity

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return data.moveToPosition(i) ? data.getLong(Contract.Quote.POSITION_ID) : i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
