package com.itkachuk.pa.activities.reports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.itkachuk.pa.R;
import com.itkachuk.pa.activities.filters.BarChartsFilterActivity.TimeSteps;
import com.itkachuk.pa.utils.ChartUtils;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by itkachuk on 2/4/2015.
 */
public class BarChartsReportActivity extends Activity {
    private static final String TAG = "PocketAccountant";

    private static final String EXTRAS_CHART_VALUES_INCOMES = "chartValuesIncomes";
    private static final String EXTRAS_CHART_VALUES_EXPENSES = "chartValuesExpenses";
    private static final String EXTRAS_CHART_VALUES_PROFITS = "chartValuesProfits";
    private static final String EXTRAS_CHART_VALUES_MIN_MAX = "chartValuesMinMax";
    private static final String EXTRAS_ACCOUNTS_FILTER = "accountsFilter";
    private static final String EXTRAS_ACCOUNT_CURRENCY = "accountCurrency";
    private static final String EXTRAS_TIME_STEP_FILTER = "timeStepFilter";
    private static final String EXTRAS_YEAR_FILTER = "yearFilter";
    private static final String EXTRAS_MONTH_FILTER = "monthFilter";
    private static final String EXTRAS_IS_EXPENSE_FILTER = "isExpenseFilter";

    // Chart values, passed via extras
    List<double[]> mChartValues;

    // Filters, passed via extras
    private int mAccountsFilter;
    private String mAccountCurrency;
    private TimeSteps mTimeStepFilter;
    private int mYearFilter;
    private int mMonthFilter;
    private boolean mIsExpenseFilter;

    private Calendar mCalendar; // this calendar needs to be synced with calendar in BarChartFilterActivity
    private Context context;

    /** The main dataset that includes all the series that go into a chart. */
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    /** The main renderer that includes all the renderers customizing a chart. */
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    /** The chart view that displays the data. */
    private GraphicalView mChartView;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current data, for instance when changing screen orientation
        outState.putSerializable("dataset", mDataset);
        outState.putSerializable("renderer", mRenderer);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        // restore the current data, for instance when changing the screen orientation
        mDataset = (XYMultipleSeriesDataset) savedState.getSerializable("dataset");
        mRenderer = (XYMultipleSeriesRenderer) savedState.getSerializable("renderer");
        mRenderer.setZoomRate(1.0f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_charts_report);
        context = this;

        parseFilters();
        initCalendar();

        prepareBarChartRendererAndDataset();
    }

    public static void callMe(Context c, List<double[]> values, int accountsFilter, String accountCurrency,
                              TimeSteps timeStepFilter, int yearFilter, int monthFilter, boolean isExpenseFilter) {
        Intent intent = new Intent(c, BarChartsReportActivity.class);
        intent.putExtra(EXTRAS_CHART_VALUES_INCOMES, values.get(0));
        intent.putExtra(EXTRAS_CHART_VALUES_EXPENSES, values.get(1));
        intent.putExtra(EXTRAS_CHART_VALUES_PROFITS, values.get(2));
        intent.putExtra(EXTRAS_CHART_VALUES_MIN_MAX, values.get(3));
        intent.putExtra(EXTRAS_ACCOUNTS_FILTER, accountsFilter);
        intent.putExtra(EXTRAS_ACCOUNT_CURRENCY, accountCurrency);
        intent.putExtra(EXTRAS_TIME_STEP_FILTER, timeStepFilter.ordinal());
        intent.putExtra(EXTRAS_YEAR_FILTER, yearFilter);
        intent.putExtra(EXTRAS_MONTH_FILTER, monthFilter);
        intent.putExtra(EXTRAS_IS_EXPENSE_FILTER, isExpenseFilter);
        c.startActivity(intent);
    }


    private List<double[]> getChartValues() {
        List<double[]> chartValues = new ArrayList<double[]>();
        chartValues.add(getIntent().getDoubleArrayExtra(EXTRAS_CHART_VALUES_INCOMES));
        chartValues.add(getIntent().getDoubleArrayExtra(EXTRAS_CHART_VALUES_EXPENSES));
        chartValues.add(getIntent().getDoubleArrayExtra(EXTRAS_CHART_VALUES_PROFITS));
        chartValues.add(getIntent().getDoubleArrayExtra(EXTRAS_CHART_VALUES_MIN_MAX));
        return chartValues;
    }

    private int getAccountsFilter() {
        return getIntent().getIntExtra(EXTRAS_ACCOUNTS_FILTER, -1); // [main] account by default
    }

    private String getAccountCurrency() {
        return getIntent().getStringExtra(EXTRAS_ACCOUNT_CURRENCY); // [main] account by default
    }

    private TimeSteps getTimeStepFilter() {
        return TimeSteps.values()[(getIntent().getIntExtra(EXTRAS_TIME_STEP_FILTER, TimeSteps.MONTH.ordinal()))]; // Month by default
    }

    private int getYearFilter() {
        return getIntent().getIntExtra(EXTRAS_YEAR_FILTER, Calendar.getInstance().get(Calendar.YEAR)); // Current year by default
    }

    private int getMonthFilter() {
        return getIntent().getIntExtra(EXTRAS_MONTH_FILTER, Calendar.getInstance().get(Calendar.MONTH)); // Current month by default
    }

    private boolean getIsExpenseFilter() {
        return getIntent().getBooleanExtra(EXTRAS_IS_EXPENSE_FILTER, true); // Expenses by default
    }

    private void parseFilters() {
        mChartValues = getChartValues();
        mAccountsFilter = getAccountsFilter();
        mAccountCurrency = getAccountCurrency();
        mTimeStepFilter = getTimeStepFilter();
        mYearFilter = getYearFilter();
        mMonthFilter = getMonthFilter();
        mIsExpenseFilter = getIsExpenseFilter();
    }

    private void initCalendar() {
        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.YEAR, mYearFilter);
        mCalendar.set(Calendar.MONTH, mMonthFilter);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mCalendar.clear(Calendar.HOUR_OF_DAY);
        mCalendar.clear(Calendar.MINUTE);
        mCalendar.clear(Calendar.SECOND);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mChartView == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            mChartView = ChartFactory.getBarChartView(this, mDataset, mRenderer, Type.DEFAULT);
            // enable the chart click events
            mRenderer.setClickEnabled(true);
            mRenderer.setSelectableBuffer(10);
            mChartView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // handle the click event on the chart
                    SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                    //if (seriesSelection == null) {
                        //Toast.makeText(BarChartsReportActivity.this, "No chart element", Toast.LENGTH_SHORT).show();
                    //} else {
                    if (seriesSelection != null && (seriesSelection.getSeriesIndex() == 0 || seriesSelection.getSeriesIndex() == 1)) {
                        // display information of the clicked point
//                        Toast.makeText(
//                                BarChartsReportActivity.this,
//                                "Chart element in series index " + seriesSelection.getSeriesIndex()
//                                        + " data point index " + seriesSelection.getPointIndex() + " was clicked"
//                                        + " closest point value X=" + seriesSelection.getXValue() + ", Y="
//                                        + seriesSelection.getValue(), Toast.LENGTH_SHORT).show();
                        // Drill to consolidated report
                        String isExpenseFilter = "";
                        long startDate = 0, endDate = Long.MAX_VALUE;
                        Calendar calendar = Calendar.getInstance();
                        calendar.clear(Calendar.HOUR_OF_DAY);
                        calendar.clear(Calendar.MINUTE);
                        calendar.clear(Calendar.SECOND);
                        switch (mTimeStepFilter) {
                            case DAY: {
                                if (mIsExpenseFilter) {
                                    isExpenseFilter = getResources().getString(R.string.Expenses_text);
                                } else {
                                    isExpenseFilter = getResources().getString(R.string.Incomes_text);
                                }
                                calendar.set(mYearFilter, mMonthFilter, (int) seriesSelection.getXValue());
                                startDate = calendar.getTimeInMillis();
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                                endDate = calendar.getTimeInMillis();
                                break;
                            }
                            case MONTH: {
                                if (seriesSelection.getSeriesIndex() == 0) {
                                    isExpenseFilter = getResources().getString(R.string.Incomes_text);
                                } else if (seriesSelection.getSeriesIndex() == 1) {
                                    isExpenseFilter = getResources().getString(R.string.Expenses_text);
                                }
                                calendar.set(mYearFilter, (int) seriesSelection.getXValue()-1, 1);
                                startDate = calendar.getTimeInMillis();
                                calendar.add(Calendar.MONTH, 1);
                                endDate = calendar.getTimeInMillis();
                                break;
                            }
                            case YEAR: {
                                if (seriesSelection.getSeriesIndex() == 0) {
                                    isExpenseFilter = getResources().getString(R.string.Incomes_text);
                                } else if (seriesSelection.getSeriesIndex() == 1) {
                                    isExpenseFilter = getResources().getString(R.string.Expenses_text);
                                }
                                calendar.set((int) seriesSelection.getXValue(), 0, 1);
                                startDate = calendar.getTimeInMillis();
                                calendar.add(Calendar.YEAR, 1);
                                endDate = calendar.getTimeInMillis();
                            }
                        }
                        ConsolidatedReportActivity.callMe(context, BarChartsReportActivity.class.getName(),
                                isExpenseFilter, mAccountsFilter, startDate, endDate);
                    }
                }
            });
            layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        } else {
            mChartView.repaint();
        }
    }

    private String getMonthStringFromDate() {
        String[] monthArray = context.getResources().getStringArray(R.array.month_names_list);
        return monthArray[mCalendar.get(Calendar.MONTH)];
    }

    private Boolean prepareBarChartRendererAndDataset() {
        String chartTitle = "";
        String xAxisTitle = "";
        int[] barsColors = new int[] {
                context.getResources().getColor(R.color.income_amount_color),
                context.getResources().getColor(R.color.expense_amount_color),
                context.getResources().getColor(R.color.light_yellow_color)};
        String[] barsTitles = new String[] {
                getResources().getString(R.string.Incomes_text),
                getResources().getString(R.string.Expenses_text),
                getResources().getString(R.string.profit_text)};

        String amountText = getResources().getString(R.string.amount_text);
        if (mAccountCurrency != null) {
            amountText = amountText + ", " + mAccountCurrency;
        }

        switch (mTimeStepFilter) {
            case DAY: {
                if (mIsExpenseFilter) {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.expenses_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Expenses_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.expense_amount_color)};
                } else {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.incomes_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Incomes_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.income_amount_color)};
                }
                xAxisTitle = getResources().getString(R.string.day_text);
                break;
            }
            case MONTH: {
                chartTitle = getResources().getString(R.string.charts_for_year_text) + " " + mCalendar.get(Calendar.YEAR);
                xAxisTitle = getResources().getString(R.string.month_text);
                break;
            }
            case YEAR: {
                chartTitle = getResources().getString(R.string.charts_for_all_time_text);
                xAxisTitle = getResources().getString(R.string.year_text);
                break;
            }
        }

        if (mChartValues == null || mChartValues.size() == 0) return false; // This shouldn't happen, throw error if it will. TODO

        double yMinValue = mChartValues.get(3)[0]; // "values.get(3)[0]" - getting the minValue from values arrays
        double yMaxValue = mChartValues.get(3)[1] + mChartValues.get(3)[1] * 0.1f; // "values.get(3)[1]" - getting the maxValue from values arrays
        double xMinValue = mChartValues.get(3)[2] - 0.5;
        double xMaxValue = mChartValues.get(3)[3] + 0.5;

        mRenderer = ChartUtils.buildBarRenderer(barsColors);

        ChartUtils.setChartRendererSettings(mRenderer, chartTitle, xAxisTitle, amountText,
                xMinValue, xMaxValue, yMinValue, yMaxValue, Color.GRAY, Color.LTGRAY, getResources().getColor(R.color.background));

        mDataset = ChartUtils.buildBarDataset(barsTitles, mChartValues);

        return true;
    }
}
