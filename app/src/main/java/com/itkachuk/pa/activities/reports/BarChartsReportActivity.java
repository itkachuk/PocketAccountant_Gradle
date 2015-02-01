package com.itkachuk.pa.activities.reports;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.itkachuk.pa.R;
import com.itkachuk.pa.entities.Account;
import com.itkachuk.pa.entities.DatabaseHelper;
import com.itkachuk.pa.entities.IncomeOrExpenseRecord;
import com.itkachuk.pa.utils.CalcUtils;
import com.itkachuk.pa.utils.ChartUtils;
import com.itkachuk.pa.utils.DateUtils;
import com.itkachuk.pa.utils.ActivityUtils;
import com.itkachuk.pa.utils.TimeRange;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

public class BarChartsReportActivity extends OrmLiteBaseActivity<DatabaseHelper> {
	private static final String TAG = "PocketAccountant";

	private Calendar mCalendar;
	
	private Spinner mAccountsFilterSpinner;
	private Spinner mTimeStepSelectorSpinner;
	private TextView mYearTimeFilter;
    private TextView mMonthTimeFilter;
	private ImageButton mRollMonthForwardButton;
	private ImageButton mRollMonthBackwardButton;
    private ImageButton mRollYearForwardButton;
    private ImageButton mRollYearBackwardButton;
	private Button mShowReportButton;
	
	private Context context;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_charts_report);
        context = this;
        mCalendar = Calendar.getInstance();
		mCalendar.set(Calendar.MONTH, 0);
		mCalendar.set(Calendar.DAY_OF_MONTH, 1);
		mCalendar.clear(Calendar.HOUR_OF_DAY);
		mCalendar.clear(Calendar.MINUTE);
		mCalendar.clear(Calendar.SECOND);
                   
        mAccountsFilterSpinner = (Spinner) findViewById(R.id.accountsFilteringSpinner);
        mTimeStepSelectorSpinner = (Spinner) findViewById(R.id.timeStepSelectorSpinner);
        mYearTimeFilter = (TextView) findViewById(R.id.yearTimeFilter);
        mMonthTimeFilter = (TextView) findViewById(R.id.monthTimeFilter);
        mRollYearForwardButton = (ImageButton) findViewById(R.id.rollYearForwardButton);
        mRollYearBackwardButton = (ImageButton) findViewById(R.id.rollYearBackwardButton);
        mRollMonthForwardButton = (ImageButton) findViewById(R.id.rollMonthForwardButton);
        mRollMonthBackwardButton = (ImageButton) findViewById(R.id.rollMonthBackwardButton);
        mShowReportButton = (Button) findViewById(R.id.showReportButton);
        // Hide status bar, but keep title bar
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // fill up spinners entries
        try {
        	Dao<Account, Integer> accountDao = getHelper().getAccountDao();
			ActivityUtils.refreshAccountSpinnerEntries(this, accountDao, mAccountsFilterSpinner);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        ActivityUtils.fillSpinnerEntriesByArrayAdapter(this, mTimeStepSelectorSpinner, R.array.bar_charts_report_time_steps_list);

        // update time filter values
        updateYearText();
        updateMonthText();

        mTimeStepSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                showHideFilterControls();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish(); // Close activity on Back button pressing
			}
		});
        
        mRollYearForwardButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		mCalendar.add(Calendar.YEAR, 1);
        		updateYearText();
			}
        });
        
        mRollYearBackwardButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		mCalendar.add(Calendar.YEAR, -1);
        		updateYearText();
			}
        });

        mRollMonthForwardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCalendar.add(Calendar.MONTH, 1);
                updateMonthText();
                updateYearText();
            }
        });

        mRollMonthBackwardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCalendar.add(Calendar.MONTH, -1);
                updateMonthText();
                updateYearText();
            }
        });
        
        mShowReportButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				new RunBarChartReportJob(context).execute();
			}
        });

        mTimeStepSelectorSpinner.setSelection(1, true); // Select Month time step as default
        ((RadioButton)findViewById(R.id.expensesRadioButton)).setChecked(true);
	}
	
	public static void callMe(Context c) {
		Intent intent = new Intent(c, BarChartsReportActivity.class);
		c.startActivity(intent);
	}
	
	private void updateYearText() {
		mYearTimeFilter.setText(Integer.toString(mCalendar.get(Calendar.YEAR)));
	}

    private void updateMonthText() {
        mMonthTimeFilter.setText(getMonthStringFromDate());
    }

    private String getMonthStringFromDate() {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMM", Locale.getDefault());
//        String month = dateFormat.format(mCalendar.getTime());
        String[] monthArray = context.getResources().getStringArray(R.array.month_names_list);
        return monthArray[mCalendar.get(Calendar.MONTH)];
    }

    private void showHideFilterControls() {
        switch (mTimeStepSelectorSpinner.getSelectedItemPosition()) {
            case 0: { // Day
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.VISIBLE);
                break;
            }
            case 1: { // Month
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
            case 2: { // Year
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
        }
    }
	
	private Intent getBarChartIntent(Context context) throws SQLException {
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

		int accountFilter = -1;
		String  accountCurrency = null;
		if (mAccountsFilterSpinner.getSelectedItem() != null) {
			Account account = (Account) mAccountsFilterSpinner.getSelectedItem();
			if (account != null) {
				accountFilter = account.getId();
				accountCurrency = account.getCurrency();
			}
		}

        switch (mTimeStepSelectorSpinner.getSelectedItemPosition()) {
            case 0: { // Day
                if (((RadioButton)findViewById(R.id.expensesRadioButton)).isChecked()) {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.expenses_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Expenses_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.expense_amount_color)};
                    values = calculateChartValuesPerDay(accountFilter, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), true);
                } else {
                    chartTitle = getResources().getString(R.string.charts_for_month_text) + " " + getResources().getString(R.string.incomes_text)
                            + ": " + getMonthStringFromDate() + ", " + mCalendar.get(Calendar.YEAR);
                    // For day time step we draw only one graph - expenses or incomes
                    barsTitles = new String[]{getResources().getString(R.string.Incomes_text)};
                    barsColors = new int[]{context.getResources().getColor(R.color.income_amount_color)};
                    values = calculateChartValuesPerDay(accountFilter, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), false);
                }
                xAxisTitle = getResources().getString(R.string.day_text);
                break;
            }
            case 1: { // Month
                chartTitle = getResources().getString(R.string.charts_for_year_text) + " " + mCalendar.get(Calendar.YEAR);
                xAxisTitle = getResources().getString(R.string.month_text);
                values = calculateChartValuesPerMonth(accountFilter, mCalendar.get(Calendar.YEAR));
                break;
            }
            case 2: { // Year
                chartTitle = getResources().getString(R.string.charts_for_all_time_text);
                xAxisTitle = getResources().getString(R.string.year_text);
                values = calculateChartValuesPerYear(accountFilter); // TODO
                break;
            }
        }

        if (values == null || values.size() == 0) return null; // If there is no data to display - return null

        double yMinValue = values.get(3)[0]; // "values.get(3)[0]" - getting the minValue from values arrays
        double yMaxValue = values.get(3)[1] + values.get(3)[1] * 0.1f; // "values.get(3)[1]" - getting the maxValue from values arrays
        double xMinValue = values.get(3)[2] - 0.5;
        double xMaxValue = values.get(3)[3] + 0.5;

		XYMultipleSeriesRenderer renderer = ChartUtils.buildBarRenderer(barsColors);

		String amountText = getResources().getString(R.string.amount_text);
		if (accountCurrency != null) {
			amountText = amountText + ", " + accountCurrency;
		}		
		ChartUtils.setChartSettings(renderer, chartTitle, xAxisTitle, amountText,
                xMinValue, xMaxValue, yMinValue, yMaxValue, Color.GRAY, Color.LTGRAY);
        for (SimpleSeriesRenderer simpleRenderer : renderer.getSeriesRenderers()) {
            simpleRenderer.setDisplayChartValues(true);
            //simpleRenderer.setDisplayChartValuesDistance(10); // try to set minimal distance - need to test
        }
		
		renderer.setXLabels((int) (xMaxValue - xMinValue)); // number of X-axis points
		renderer.setYLabels(10);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setPanEnabled(true, true);
		renderer.setMargins(new int[]{30, 25, 20, 10});
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(true);
		renderer.setZoomRate(1.0f);
		renderer.setBarSpacing(0.3f);
        renderer.setBackgroundColor(getResources().getColor(R.color.background));
        renderer.setApplyBackgroundColor(true);
		return ChartFactory.getBarChartIntent(context, ChartUtils.buildBarDataset(barsTitles, values), renderer,
				Type.DEFAULT);
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
	
	private class RunBarChartReportJob extends AsyncTask<Void,Void,Intent> {
		private ProgressDialog progressDialog;
				
		public RunBarChartReportJob(Context context) {
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
		protected Intent doInBackground(Void... arg0) {
			//Log.d(TAG, "RunBarChartReportJob: called doInBackground");				
			Intent barChartIntent = null;
			
			try {
				barChartIntent = getBarChartIntent(context);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			return barChartIntent;
		}
		
		@Override
        protected void onPostExecute(Intent barChartIntent) {
			//Log.d(TAG, "RunBarChartReportJob: called onPostExecute");	                					
    		if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    		if (barChartIntent != null) {
                startActivity(barChartIntent);
            } else {
                Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_data_message), Toast.LENGTH_LONG).show();
            }
        }
	}
}
