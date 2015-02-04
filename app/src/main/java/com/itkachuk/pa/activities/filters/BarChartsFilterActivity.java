package com.itkachuk.pa.activities.filters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.itkachuk.pa.R;
import com.itkachuk.pa.activities.reports.BarChartsReportActivityNew;
import com.itkachuk.pa.entities.Account;
import com.itkachuk.pa.entities.DatabaseHelper;
import com.itkachuk.pa.utils.ActivityUtils;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Calendar;

/**
 * Created by itkachuk on 2/4/2015.
 */
public class BarChartsFilterActivity extends OrmLiteBaseActivity<DatabaseHelper> {
    private static final String TAG = "PocketAccountant";

    private Calendar mCalendar;

    private Spinner mAccountsFilterSpinner;
    private Spinner mTimeStepSelectorSpinner;
    private TextView mYearTimeFilter;
    private TextView mMonthTimeFilter;

    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_charts_filter_editor);
        context = this;
        mCalendar = Calendar.getInstance();
        //mCalendar.set(Calendar.MONTH, 0);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);
        mCalendar.clear(Calendar.HOUR_OF_DAY);
        mCalendar.clear(Calendar.MINUTE);
        mCalendar.clear(Calendar.SECOND);

        mAccountsFilterSpinner = (Spinner) findViewById(R.id.accountsFilteringSpinner);
        mTimeStepSelectorSpinner = (Spinner) findViewById(R.id.timeStepSelectorSpinner);
        mYearTimeFilter = (TextView) findViewById(R.id.yearTimeFilter);
        mMonthTimeFilter = (TextView) findViewById(R.id.monthTimeFilter);
        ImageButton mRollYearForwardButton = (ImageButton) findViewById(R.id.rollYearForwardButton);
        ImageButton mRollYearBackwardButton = (ImageButton) findViewById(R.id.rollYearBackwardButton);
        ImageButton mRollMonthForwardButton = (ImageButton) findViewById(R.id.rollMonthForwardButton);
        ImageButton mRollMonthBackwardButton = (ImageButton) findViewById(R.id.rollMonthBackwardButton);
        Button mShowReportButton = (Button) findViewById(R.id.showReportButton);
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
                //new RunBarChartReportJob(context).execute(); // TODO

                int accountsFilter, yearFilter, monthFilter;
                TimeSteps timeStepFilter;
                boolean isExpenseFilter;
                accountsFilter = ((Account) mAccountsFilterSpinner.getSelectedItem()).getId();
                timeStepFilter = TimeSteps.values()[mTimeStepSelectorSpinner.getSelectedItemPosition()];
                yearFilter = mCalendar.get(Calendar.YEAR);
                monthFilter = mCalendar.get(Calendar.MONTH);
                isExpenseFilter = ((RadioButton)findViewById(R.id.expensesRadioButton)).isChecked();

                BarChartsReportActivityNew.callMe(context, accountsFilter, timeStepFilter, yearFilter, monthFilter, isExpenseFilter);
            }
        });

        mTimeStepSelectorSpinner.setSelection(1, true); // Select Month time step as default
        ((RadioButton)findViewById(R.id.expensesRadioButton)).setChecked(true);
    }

    public static void callMe(Context c) {
        Intent intent = new Intent(c, BarChartsFilterActivity.class);
        c.startActivity(intent);
    }

    private void updateYearText() {
        mYearTimeFilter.setText(Integer.toString(mCalendar.get(Calendar.YEAR)));
    }

    private void updateMonthText() {
        mMonthTimeFilter.setText(getMonthStringFromDate());
    }

    private String getMonthStringFromDate() {
        String[] monthArray = context.getResources().getStringArray(R.array.month_names_list);
        return monthArray[mCalendar.get(Calendar.MONTH)];
    }

    private void showHideFilterControls() {
        switch (TimeSteps.values()[mTimeStepSelectorSpinner.getSelectedItemPosition()]) {
            case DAY: { // Day
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.VISIBLE);
                break;
            }
            case MONTH: { // Month
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.VISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
            case YEAR: { // Year
                findViewById(R.id.yearTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.monthTimeFilterBlock).setVisibility(View.INVISIBLE);
                findViewById(R.id.incomesExpensesFilterBlock).setVisibility(View.INVISIBLE);
                break;
            }
        }
    }


    public enum TimeSteps {
        DAY,
        MONTH,
        YEAR
    }
}
