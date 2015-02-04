package com.itkachuk.pa.activities.reports;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.itkachuk.pa.R;
import com.itkachuk.pa.activities.filters.BarChartsFilterActivity.TimeSteps;
import com.itkachuk.pa.entities.Account;
import com.itkachuk.pa.entities.DatabaseHelper;
import com.itkachuk.pa.entities.IncomeOrExpenseRecord;
import com.itkachuk.pa.utils.ActivityUtils;
import com.itkachuk.pa.utils.CalcUtils;
import com.itkachuk.pa.utils.ChartUtils;
import com.itkachuk.pa.utils.DateUtils;
import com.itkachuk.pa.utils.TimeRange;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by itkachuk on 2/4/2015.
 */
public class BarChartsReportActivityNew extends OrmLiteBaseActivity<DatabaseHelper> {
    private static final String TAG = "PocketAccountant";

    private static final String EXTRAS_ACCOUNTS_FILTER = "accountsFilter";
    private static final String EXTRAS_TIME_STEP_FILTER = "timeStepFilter";
    private static final String EXTRAS_YEAR_FILTER = "yearFilter";
    private static final String EXTRAS_MONTH_FILTER = "monthFilter";
    private static final String EXTRAS_IS_EXPENSE_FILTER = "isExpenseFilter";

    // Filters, passed via extras
    private int mAccountsFilter;
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_charts_report);
        context = this;

        parseFilters();
        initCalendar();

        new CalculateBarChartValuesJob(this).execute();
    }

    public static void callMe(Context c, int accountsFilter,
                              TimeSteps timeStepFilter, int yearFilter, int monthFilter, boolean isExpenseFilter) {
        Intent intent = new Intent(c, BarChartsReportActivityNew.class);
        intent.putExtra(EXTRAS_ACCOUNTS_FILTER, accountsFilter);
        intent.putExtra(EXTRAS_TIME_STEP_FILTER, timeStepFilter.ordinal());
        intent.putExtra(EXTRAS_YEAR_FILTER, yearFilter);
        intent.putExtra(EXTRAS_MONTH_FILTER, monthFilter);
        intent.putExtra(EXTRAS_IS_EXPENSE_FILTER, isExpenseFilter);
        c.startActivity(intent);
    }


    private int getAccountsFilter() {
        return getIntent().getIntExtra(EXTRAS_ACCOUNTS_FILTER, -1); // [main] account by default
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
        mAccountsFilter = getAccountsFilter();
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
                    if (seriesSelection == null) {
                        Toast.makeText(BarChartsReportActivityNew.this, "No chart element", Toast.LENGTH_SHORT).show();
                    } else {
                        // display information of the clicked point
                        Toast.makeText(
                                BarChartsReportActivityNew.this,
                                "Chart element in series index " + seriesSelection.getSeriesIndex()
                                        + " data point index " + seriesSelection.getPointIndex() + " was clicked"
                                        + " closest point value X=" + seriesSelection.getXValue() + ", Y="
                                        + seriesSelection.getValue(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            //boolean enabled = mDataset.getSeriesCount() > 0;
            //setSeriesWidgetsEnabled(enabled);
        } else {
            mChartView.repaint();
        }
    }

    private String getMonthStringFromDate() {
        String[] monthArray = context.getResources().getStringArray(R.array.month_names_list);
        return monthArray[mCalendar.get(Calendar.MONTH)];
    }

    private Boolean prepareBarChartRendererAndValues(Context context) throws SQLException {
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

        List<double[]> values = null;

        String  accountCurrency = null;
        try {
            Dao<Account, Integer> accountDao = getHelper().getAccountDao();
            Account account = accountDao.queryForId(mAccountsFilter);
            if (account != null) {
                accountCurrency = account.getCurrency();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String amountText = getResources().getString(R.string.amount_text);
        if (accountCurrency != null) {
            amountText = amountText + ", " + accountCurrency;
        }

        switch (mTimeStepFilter) {
            case DAY: {
                if (mIsExpenseFilter) {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.expenses_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Expenses_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.expense_amount_color)};
                    values = calculateChartValuesPerDay(mAccountsFilter, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), true);
                } else {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.incomes_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Incomes_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.income_amount_color)};
                    values = calculateChartValuesPerDay(mAccountsFilter, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), false);
                }
                xAxisTitle = getResources().getString(R.string.day_text);
                break;
            }
            case MONTH: {
                chartTitle = getResources().getString(R.string.charts_for_year_text) + " " + mCalendar.get(Calendar.YEAR);
                xAxisTitle = getResources().getString(R.string.month_text);
                values = calculateChartValuesPerMonth(mAccountsFilter, mCalendar.get(Calendar.YEAR));
                break;
            }
            case YEAR: {
                chartTitle = getResources().getString(R.string.charts_for_all_time_text);
                xAxisTitle = getResources().getString(R.string.year_text);
                values = calculateChartValuesPerYear(mAccountsFilter);
                break;
            }
        }

        if (values == null || values.size() == 0) return false; // If there is no data to display - return false

        double yMinValue = values.get(3)[0]; // "values.get(3)[0]" - getting the minValue from values arrays
        double yMaxValue = values.get(3)[1] + values.get(3)[1] * 0.1f; // "values.get(3)[1]" - getting the maxValue from values arrays
        double xMinValue = values.get(3)[2] - 0.5;
        double xMaxValue = values.get(3)[3] + 0.5;

        mRenderer = ChartUtils.buildBarRenderer(barsColors);

        ChartUtils.setChartRendererSettings(mRenderer, chartTitle, xAxisTitle, amountText,
                xMinValue, xMaxValue, yMinValue, yMaxValue, Color.GRAY, Color.LTGRAY, getResources().getColor(R.color.background));
        for (SimpleSeriesRenderer simpleRenderer : mRenderer.getSeriesRenderers()) {
            simpleRenderer.setDisplayChartValues(true);
            //simpleRenderer.setDisplayChartValuesDistance(10); // try to set minimal distance - need to test
        }

//        renderer.setXLabels((int) (xMaxValue - xMinValue)); // number of X-axis points
//        renderer.setYLabels(10);
//        renderer.setXLabelsAlign(Paint.Align.LEFT);
//        renderer.setYLabelsAlign(Paint.Align.LEFT);
//        renderer.setPanEnabled(true, true);
//        renderer.setMargins(new int[]{30, 25, 20, 10});
//        renderer.setZoomButtonsVisible(false);
//        renderer.setZoomEnabled(true);
//        renderer.setZoomRate(1.0f);
//        renderer.setBarSpacing(0.3f);
//        renderer.setBackgroundColor(getResources().getColor(R.color.background));
//        renderer.setApplyBackgroundColor(true);

        mDataset = ChartUtils.buildBarDataset(barsTitles, values);

        return true;
    }

    private List<double[]> calculateChartValuesPerDay(int accountFilter, int year, int month, boolean isExpenses) throws SQLException {
        Dao<IncomeOrExpenseRecord, Integer> recordDao = getHelper().getRecordDao();
        List<double[]> values = new ArrayList<double[]>();
        int daysInMonth = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        double[] results = new double[daysInMonth];

        double[] minMaxValues = new double[4];
        minMaxValues[0] = 0; // min Y
        minMaxValues[1] = 0; // max Y
        minMaxValues[2] = 1; // min X
        minMaxValues[3] = daysInMonth; // max X
        double result;

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        TimeRange timeRange = new TimeRange(DateUtils.DEFAULT_START_DATE, DateUtils.DEFAULT_END_DATE);


        for (int day = 0; day < daysInMonth; day++) {
            timeRange.setStartTime(calendar.getTimeInMillis());
            //Log.d(TAG, "StartDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + day + ")");
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            timeRange.setEndTime(calendar.getTimeInMillis());
            //Log.d(TAG, "EndDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + day + ")");

            result = toDoubleAndRound(CalcUtils.getSumOfRecords(recordDao, accountFilter, isExpenses, timeRange));
            results[day] = result;

            // identify the min and max values for BarChart graph (Y-axis)
            if (result > minMaxValues[1]) minMaxValues[1] = result;
        }

        if (minMaxValues[1] != 0) { // if at least one non-zero result exist - then return values. Otherwise, return empty to display no data message
            values.add(results);
            values.add(new double[1]); // for compatibility
            values.add(new double[1]); // for compatibility
            values.add(minMaxValues);
        }

        return values;
    }

    private List<double[]> calculateChartValuesPerMonth(int accountFilter, int year) throws SQLException {
        Dao<IncomeOrExpenseRecord, Integer> recordDao = getHelper().getRecordDao();
        List<double[]> values = new ArrayList<double[]>();
        double[] incomes = new double[12];
        double[] expenses = new double[12];
        double[] profits = new double[12];
        double[] minMaxValues = new double[4];
        minMaxValues[0] = 0; // min Y
        minMaxValues[1] = 0; // max Y
        minMaxValues[2] = 1; // min X
        minMaxValues[3] = 12; // max X
        double income, expense, profit;

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        TimeRange timeRange = new TimeRange(DateUtils.DEFAULT_START_DATE, DateUtils.DEFAULT_END_DATE);


        for (int month = 0; month < 12; month++) {
            timeRange.setStartTime(calendar.getTimeInMillis());
            //Log.d(TAG, "StartDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + month + ")");
            calendar.add(Calendar.MONTH, 1);
            timeRange.setEndTime(calendar.getTimeInMillis());
            //Log.d(TAG, "EndDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + month + ")");

            income = toDoubleAndRound(CalcUtils.getSumOfRecords(recordDao, accountFilter, false, timeRange));
            expense = toDoubleAndRound(CalcUtils.getSumOfRecords(recordDao, accountFilter, true, timeRange));
            profit = income - expense;

            incomes[month] = income;
            expenses[month] = expense;
            profits[month] = profit;

            // identify the min and max values for BarChart graph (Y-axis)
            if (profit < 0) minMaxValues[0] = profit; // min
            if (income > minMaxValues[1]) minMaxValues[1] = income;
            if (expense > minMaxValues[1]) minMaxValues[1] = expense;
        }

        if (minMaxValues[1] != 0) { // if at least one non-zero result exist - then return values. Otherwise, return empty to display no data message
            values.add(incomes);
            values.add(expenses);
            values.add(profits);
            values.add(minMaxValues);
        }

        return values;
    }

    private List<double[]> calculateChartValuesPerYear(int accountFilter) throws SQLException {
        Dao<IncomeOrExpenseRecord, Integer> recordDao = getHelper().getRecordDao();
        List<double[]> values = new ArrayList<double[]>();
        ArrayList<Double> incomesList = new ArrayList<Double>();
        ArrayList<Double> expensesList = new ArrayList<Double>();
        ArrayList<Double> profitsList = new ArrayList<Double>();

        int xAxisPoints = 10; // number of years to calculate
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int startYear = currentYear - xAxisPoints + 1;
        double[] minMaxValues = new double[4];
        minMaxValues[0] = 0; // min Y
        minMaxValues[1] = 0; // max Y
        minMaxValues[2] = startYear; // min X
        minMaxValues[3] = currentYear; // max X
        double income, expense, profit;

        calendar.clear();
        calendar.set(Calendar.YEAR, startYear);

        TimeRange timeRange = new TimeRange(DateUtils.DEFAULT_START_DATE, DateUtils.DEFAULT_END_DATE);

        for (int year = startYear; year <= currentYear; year++) {
            timeRange.setStartTime(calendar.getTimeInMillis());
            //Log.d(TAG, "StartDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + year + ")");
            calendar.add(Calendar.YEAR, 1);
            timeRange.setEndTime(calendar.getTimeInMillis());
            //Log.d(TAG, "EndDate: " + DateFormat.format("dd.MM.yy hh:mm:ss", mCalendar) + "(" + year + ")");

            income = toDoubleAndRound(CalcUtils.getSumOfRecords(recordDao, accountFilter, false, timeRange));
            expense = toDoubleAndRound(CalcUtils.getSumOfRecords(recordDao, accountFilter, true, timeRange));
            profit = income - expense;

            incomesList.add(income);
            expensesList.add(expense);
            profitsList.add(profit);

            // identify the min and max values for BarChart graph (Y-axis)
            if (profit < 0) minMaxValues[0] = profit; // min
            if (income > minMaxValues[1]) minMaxValues[1] = income;
            if (expense > minMaxValues[1]) minMaxValues[1] = expense;
        }

        // Trim result arrays from left and right - cut points with zero data
        int i = 0, startIndex = 0, endIndex = 0 ;
        while (i < xAxisPoints && incomesList.get(i) == 0 && expensesList.get(i) == 0) {
            minMaxValues[2] = minMaxValues[2] + 1;
            i++;
        }
        startIndex = i;
        i = xAxisPoints - 1;
        while (i >= 0 && incomesList.get(i) == 0 && expensesList.get(i) == 0) {
            minMaxValues[3] = minMaxValues[3] - 1;
            i--;
        }
        endIndex = i + 1;
        if ((startIndex < endIndex) && (startIndex > 0 || endIndex < xAxisPoints)) {
            incomesList = new ArrayList<Double>(incomesList.subList(startIndex, endIndex));
            expensesList = new ArrayList<Double>(expensesList.subList(startIndex, endIndex));
            profitsList = new ArrayList<Double>(profitsList.subList(startIndex, endIndex));
        }

        if (incomesList.size() > 0 || expensesList.size() > 0) {

            double[] incomes = getArrayFromList(incomesList);
            double[] expenses = getArrayFromList(expensesList);
            double[] profits = getArrayFromList(profitsList);

            values.add(incomes);
            values.add(expenses);
            values.add(profits);
            values.add(minMaxValues);
        }

        return values;
    }

    private double[] getArrayFromList(ArrayList<Double> list) {
        Object[] temp = list.toArray();
        double[] result = new double[temp.length];
        for (int i = 0; i < temp.length; i++) {
            result[i] = (Double) temp[i];
        }
        return result;
    }

    private double toDoubleAndRound(String value) {
        return Math.round(Double.valueOf(value));
    }

    private class CalculateBarChartValuesJob extends AsyncTask<Void,Void,Boolean> {
        private ProgressDialog progressDialog;

        public CalculateBarChartValuesJob(Context context) {
            super();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("");
            progressDialog.setMessage(context.getString(R.string.data_calculation_text));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            //Log.d(TAG, "RunBarChartReportJob: called onPreExecute");
            if (progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        };

        @Override
        protected Boolean doInBackground(Void... arg0) {
            //Log.d(TAG, "RunBarChartReportJob: called doInBackground");
            Boolean isDataAvailable = false;

            try {
                isDataAvailable = prepareBarChartRendererAndValues(context);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return isDataAvailable;
        }

        @Override
        protected void onPostExecute(Boolean isDataAvailable) {
            //Log.d(TAG, "RunBarChartReportJob: called onPostExecute");
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            if (isDataAvailable) {
                // repaint TODO
                mChartView.repaint();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_data_message), Toast.LENGTH_LONG).show();
            }
        }
    }
}
