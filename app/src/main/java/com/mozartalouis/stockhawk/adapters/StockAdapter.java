package com.mozartalouis.stockhawk.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mozartalouis.stockhawk.R;
import com.mozartalouis.stockhawk.data.Contract;
import com.mozartalouis.stockhawk.utils.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressLint("StringFormatInvalid")
public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    // Context
    final private Context mContext;

    // Formats for the stock increase and decrease
    final private DecimalFormat mDollarFormat, mPercentageFormat;

    // Interface for the main activity when a view is clicked.
    final private StockAdapterOnClickHandler mClickHandler;

    /**
     * A cursor that contains relevant stock data for the list.
     */
    private Cursor mCursor;

    public StockAdapter(Context context, StockAdapterOnClickHandler clickHandler) {
        this.mContext = context;
        this.mClickHandler = clickHandler;

        this.mDollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
        this.mDollarFormat.setMaximumFractionDigits(2);

        this.mPercentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        this.mPercentageFormat.setMinimumFractionDigits(2);
    }

    public void setCursor(Cursor cursor) {
        notifyDataSetChanged();
        this.mCursor = cursor;
    }

    public String getSymbolAtPosition(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(Contract.Quote.POSITION_SYMBOL);
    }

    @Override
    public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(mContext).inflate(R.layout.item_stock_quote, parent, false);
        return new StockViewHolder(item);
    }

    @Override
    public void onBindViewHolder(StockViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        // Set name
        holder.symbol.setText(mCursor.getString(Contract.Quote.POSITION_SYMBOL));

        // Set stock price for stock with content description for accessibility.
        holder.price.setText(mDollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));
        holder.price.setContentDescription(
                String.format(mContext.getString(R.string.accessibility_stock_price), holder.price.getText()));

        // Set the correct display mode for the change type.
        if (PrefUtils.getDisplayMode(mContext).equals
                (mContext.getString(R.string.pref_display_mode_absolute_key))) {
            holder.change.setText(mDollarFormat.format
                    (mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE)));
        } else {
            holder.change.setText(mPercentageFormat.format
                    (mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE) / 100));
        }

        // Change the color and content description based on where the change was positive or
        // negative.
        if (mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE) >= 0) {
            holder.change.setBackgroundResource(R.drawable.percent_change_pill_green);
            holder.change.setContentDescription(String.format
                    (mContext.getString(R.string.accessibility_stock_increment), holder.change.getText()));
        } else {
            holder.change.setBackgroundResource(R.drawable.percent_change_pill_red);
            holder.change.setContentDescription(String.format
                    (mContext.getString(R.string.accessibility_stock_decrement), holder.change.getText()));
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * An interface to enter the detail from the main activity when the user click on a stock
     */
    public interface StockAdapterOnClickHandler {
        void onStockClick(String symbol);
    }

    class StockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // XML Views
        @BindView(R.id.symbol)
        TextView symbol;
        @BindView(R.id.price)
        TextView price;
        @BindView(R.id.change)
        TextView change;

        StockViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int symbolColumn = mCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL);
            mClickHandler.onStockClick(mCursor.getString(symbolColumn));
        }
    }
}
