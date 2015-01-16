package com.itkachuk.pa.activities.reports;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.widget.Spinner;
import android.widget.TextView;

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
	}
	
	public static void callMe(Context c) {
		Intent intent = new Intent(c, BarChartsReportActivity.class);
		c.startActivity(intent);
	}
	
	private void updateYearText() {
		mYearTimeFilter.setText(Integer.toString(mCalendar.get(Calendar.YEAR)));
	}

    private void updateMonthText() {
        //mMonthTimeFilter.setText(Integer.toString(mCalendar.get(Calendar.MONTH)));
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMM", Locale.getDefault());
        String month = dateFormat.format(mCalendar.getTime());
        mMonthTimeFilter.setText(month);
    }

    private void showHideFilterControls() {
        switch (mTimeStepSelectorSpinner.getSelectedItemPosition()) {
            case 0: {
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.VISIBLE);
                break;
            }
            case 1: {
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
            case 2: {
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
        }
    }
	
	private Intent getBarChartIntent(Context context) throws SQLException {
		// Set bars labels: "Incomes", "Expenses", "Balance"		
		String[] titles = new String[] { 
				getResources().getString(R.string.incomes_text), 
				getResources().getString(R.string.expenses_text), 
				getResources().getString(R.string.balance_text) };
		
		int accountFilter = -1;
		String  accountCurrency = null;
		if (mAccountsFilterSpinner.getSelectedItem() != null) {
			Account account = (Account) mAccountsFilterSpinner.getSelectedItem();
			if (account != null) {
				accountFilter = account.getId();
				accountCurrency = account.getCurrency();
			}
		}
		
		List<double[]> values = calculateChartValues(accountFilter, mCalendar.get(Calendar.YEAR));
		double minValue = values.get(3)[0]; // "values.get(3)[0]" - getting the minValue from values arrays
		double maxValue = values.get(3)[1] + values.get(3)[1] * 0.1f; // "values.get(3)[1]" - getting the maxValue from values arrays
		
		int[] colors = new int[] { 
				context.getResources().getColor(R.color.income_amount_color),
				context.getResources().getColor(R.color.expense_amount_color),
				context.getResources().getColor(R.color.light_yellow_color)};	
		XYMultipleSeriesRenderer renderer = ChartUtils.buildBarRenderer(colors);
		
		String balanceForYearText = getResources().getString(R.string.balance_for_year_text);
		String amountText = getResources().getString(R.string.amount_text);
		if (accountCurrency != null) {
			amountText = amountText + ", " + accountCurrency;
		}		
		ChartUtils.setChartSettings(renderer, balanceForYearText + " " + mCalendar.get(Calendar.YEAR),
				getResources().getString(R.string.month_text), amountText, 
				0.5, 12.5, minValue, maxValue, Color.GRAY, Color.LTGRAY);
        for (SimpleSeriesRenderer simpleRenderer : renderer.getSeriesRenderers()) {
            simpleRenderer.setDisplayChartValues(true);
            simpleRenderer.setDisplayChartValuesDistance(10); // try to set minimal distance - need to test
        }
		
		renderer.setXLabels(12);
		renderer.setYLabels(10);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setPanEnabled(true, true);
		renderer.setMargins(new int[]{30, 25, 20, 10});
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(true);
		renderer.setZoomRate(1.0f);
		renderer.setBarSpacing(0.3f);
		return ChartFactory.getBarChartIntent(context, ChartUtils.buildBarDataset(titles, values), renderer,
				Type.DEFAULT);
	}
	
	private List<double[]> calculateChartValues(int accountFilter, int year) throws SQLException {
		Dao<IncomeOrExpenseRecord, Integer> recordDao = getHelper().getRecordDao();
		List<double[]> values = new ArrayList<double[]>();
		double[] incomes = new double[12]; 
		double[] expenses = new double[12];
		double[] balances = new double[12];
		double[] minMaxValues = new double[2];
		minMaxValues[0] = 0; // min
		minMaxValues[1] = 0; // max
		double income, expense, balance;
		
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
			balance = income - expense;
			
			incomes[month] = income;
			expenses[month] = expense;
			balances[month] = balance;
			
			// identify the min and max values for BarChart graph (Y-axis)
			if (balance < 0) minMaxValues[0] = balance; // min			
			if (income > minMaxValues[1]) minMaxValues[1] = income;
			if (expense > minMaxValues[1]) minMaxValues[1] = expense;
		}
		
		values.add(incomes);
		values.add(expenses);
		values.add(balances);
		values.add(minMaxValues);
		
		return values;
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
    		startActivity(barChartIntent);
        }
	}
}
