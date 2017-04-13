package com.mozartalouis.stockhawk.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mozartalouis.stockhawk.R;
import com.mozartalouis.stockhawk.adapters.StockAdapter;
import com.mozartalouis.stockhawk.data.Contract;
import com.mozartalouis.stockhawk.ui.dialogs.AddStockDialog;
import com.mozartalouis.stockhawk.utils.PrefUtils;
import com.mozartalouis.stockhawk.sync.QuoteSyncJob;
import com.mozartalouis.stockhawk.utils.NetworkUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressLint("StringFormatInvalid")
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    /**
     * Cursor loader ID
     */
    private static final int STOCK_LOADER = 0;

    /**
     *
     */
    private StockAdapter mStockAdapter;

    /**
     *
     */
    private Snackbar mSnackBar;

    /**
     *
     */
    private BroadcastReceiver mBroadcastReceiver;

    // XML Views
    @BindView(R.id.main_coordinator_layout)
    public CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.main_toolbar)
    public Toolbar mToolbar;
    @BindView(R.id.main_swipe_refresh)
    public SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.main_recycler_view)
    public RecyclerView mRecyclerView;
    @BindView(R.id.main_fab)
    public FloatingActionButton mFloatingActionButton;
    @BindView(R.id.main_error)
    public TextView mErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Setup Toolbar;
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.app_name);

        // Setup StockAdapter
        mStockAdapter = new StockAdapter(this, this);

        // Setup RecyclerView
        mRecyclerView.setAdapter(mStockAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup SwipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if (savedInstanceState == null) {
            QuoteSyncJob.initialize(this);
            mSwipeRefreshLayout.setRefreshing(true);
        }

        // Initialize & Register Broadcast Receiver.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!NetworkUtils.hasInternetConnection(context)) {
                    showInternetOffSnackBar();
                } else {
                    mSwipeRefreshLayout.setRefreshing(true);
                    if (mSnackBar != null) mSnackBar.dismiss();
                    updateStatus();
                }
            }
        };
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


        setUpDeletionOnSlide();

        // Load Cursor
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            mStockAdapter.notifyDataSetChanged();
            getWindow().getDecorView().findViewById(R.id.action_change_units).announceForAccessibility(
                    String.format(getString(R.string.accessibility_display_mode_change), PrefUtils.getDisplayMode(this))
            );
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@link LoaderManager.LoaderCallbacks<Cursor> }
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.uri,
                Contract.Quote.QUOTE_COLUMNS,
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mStockAdapter.setCursor(null);
        updateStatus();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mStockAdapter.setCursor(data);
        mSwipeRefreshLayout.setRefreshing(false);

        QuoteSyncJob.updateWidget(MainActivity.this);

        updateStatus();
        if (data.getCount() == 0) {
            supportStartPostponedEnterTransition();
        }
    }

    /**
     * {@link StockAdapter.StockAdapterOnClickHandler}
     */

    @Override
    public void onStockClick(String symbol) {
        final Intent intent = new Intent(this, DetailActivity.class);
        intent.setData(Contract.Quote.makeUriForStock(symbol));
        startActivity(intent);
    }

    /**
     * {@link SwipeRefreshLayout.OnRefreshListener}
     */
    @Override
    public void onRefresh() {
        if (NetworkUtils.hasInternetConnection(this)) {
            QuoteSyncJob.syncImmediately(this);
            mSwipeRefreshLayout.setRefreshing(true);
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
            showInternetOffSnackBar();
        }
    }

    private void setUpDeletionOnSlide() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = mStockAdapter.getSymbolAtPosition(viewHolder.getAdapterPosition());

                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);

                mStockAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                getSupportLoaderManager().restartLoader(STOCK_LOADER, null, MainActivity.this);

                if (mStockAdapter.getItemCount() == 0) {
                    updateStatus();
                }
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    private void showInternetOffSnackBar() {
        mSnackBar = Snackbar.make(mCoordinatorLayout, getString(R.string.error_no_network),
                Snackbar.LENGTH_INDEFINITE);
        mSnackBar.setAction(getString(R.string.generic_retry), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
                if (!NetworkUtils.hasInternetConnection(MainActivity.this)) {
                    showInternetOffSnackBar();
                }
            }
        });
        mSnackBar.setActionTextColor(getResources().getColor(R.color.generic_white));
        mSnackBar.show();
    }

    private void updateStatus() {
        if (mStockAdapter.getItemCount() != 0) {
            mErrorTextView.setVisibility(View.INVISIBLE);
            return;
        }

        mErrorTextView.setVisibility(View.VISIBLE);

        if (!NetworkUtils.hasInternetConnection(this)) {
            mErrorTextView.setText(R.string.error_no_network);
            return;
        }

        if (PrefUtils.getStocks(this).size() == 0) {
            mErrorTextView.setText(R.string.error_no_stocks);
            return;
        }

        mSwipeRefreshLayout.setRefreshing(true);
    }


    public void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (NetworkUtils.hasInternetConnection(this)) {
                mSwipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                showInternetOffSnackBar();
            }
            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    public void button(View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        item.setIcon(PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))
                ? R.drawable.ic_percentage
                : R.drawable.ic_dollar);
    }
}
