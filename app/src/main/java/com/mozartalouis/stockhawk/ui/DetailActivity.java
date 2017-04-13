package com.mozartalouis.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mozartalouis.stockhawk.R;
import com.mozartalouis.stockhawk.data.Contract;
import com.mozartalouis.stockhawk.models.ChartModel;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Loader ID
    private static final int LOADER = 100;

    /**
     * Contains a uri to a specific stock in our content provider.
     */
    private Uri mStockUri;

    /**
     *
     */
    private List<ChartModel> mBarChartData = new ArrayList<>();


    // XML Views
    @BindView(R.id.detail_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.detail_chart)
    LineChart mBarChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        mStockUri = getIntent().getData();

        // Set up toolbar.
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportLoaderManager().initLoader(LOADER, null, this);
    }

    /**
     * {@link LoaderManager.LoaderCallbacks<Cursor> }
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mStockUri == null)
            return null;

        return new CursorLoader(
                this,
                mStockUri,
                Contract.Quote.QUOTE_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst())
            return;

        // Load all the history data doe the current cursor.
        final CSVReader csvReader = new CSVReader(new StringReader(
                data.getString(Contract.Quote.POSITION_HISTORY)), ',');
        try {
            String[] record;
            while ((record = csvReader.readNext()) != null) {
                mBarChartData.add(new ChartModel(Float.valueOf(record[0]), Float.valueOf(record[1])));
            }
            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        buildBarChart();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void buildBarChart() {
        mBarChart.setBackgroundColor(getResources().getColor(R.color.off_white));
        mBarChart.setDrawGridBackground(false);
        mBarChart.setTouchEnabled(true);
        mBarChart.setDragEnabled(true);
        mBarChart.setScaleEnabled(true);
        mBarChart.setPinchZoom(true);
        mBarChart.getAxisRight().setEnabled(false);
        mBarChart.setNoDataText("");
        mBarChart.invalidate();

        List<Entry> barDataList = new ArrayList<>();
        for (ChartModel data : mBarChartData)
            barDataList.add(new Entry(data.getHistory(), data.getPrice()));

        LineDataSet set = new LineDataSet(barDataList, "Test");
        set.enableDashedLine(10f, 5f, 0f);
        set.enableDashedHighlightLine(10f, 5f, 0f);
        set.setColor(Color.BLACK);
        set.setCircleColor(Color.BLACK);
        set.setLineWidth(1f);
        set.setCircleRadius(3f);
        set.setDrawCircleHole(false);
        set.setValueTextSize(9f);
        set.setDrawFilled(true);

        LineData data = new LineData(set);
        mBarChart.setData(data);
        mBarChart.invalidate();
    }
}
