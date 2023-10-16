package com.gunnarro.android.terex.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.gunnarro.android.terex.R;
import com.gunnarro.android.terex.domain.entity.Timesheet;
import com.gunnarro.android.terex.domain.entity.TimesheetEntry;
import com.gunnarro.android.terex.domain.entity.TimesheetWithEntries;
import com.gunnarro.android.terex.exception.TerexApplicationException;
import com.gunnarro.android.terex.ui.adapter.TimesheetEntryListAdapter;
import com.gunnarro.android.terex.ui.view.TimesheetEntryViewModel;
import com.gunnarro.android.terex.utility.Utility;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TimesheetEntryListFragment extends Fragment {
    public static final String TIMESHEET_ENTRY_REQUEST_KEY = "200";
    public static final String TIMESHEET_ENTRY_JSON_KEY = "timesheet_entry_as_json";
    public static final String TIMESHEET_ENTRY_ACTION_KEY = "211";
    public static final String TIMESHEET_ENTRY_ACTION_SAVE = "timesheet_entry_save";
    public static final String TIMESHEET_ENTRY_ACTION_DELETE = "timesheet_entry_delete";

    private TimesheetEntryViewModel timesheetEntryViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.title_timesheets);
        // Get a new or existing ViewModel from the ViewModelProvider.
        try {
            timesheetEntryViewModel = new ViewModelProvider(this).get(TimesheetEntryViewModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        getParentFragmentManager().setFragmentResultListener(TIMESHEET_ENTRY_REQUEST_KEY, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                Log.d(Utility.buildTag(getClass(), "onFragmentResult"), "requestKey: " + requestKey + ", bundle:" + bundle);
                handleFragmentResult(bundle);
            }
        });
        Log.d(Utility.buildTag(getClass(), "onCreate"), "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recycler_timesheet_entry_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.timesheet_entry_recyclerview);
        final TimesheetEntryListAdapter adapter = new TimesheetEntryListAdapter(getParentFragmentManager(), new TimesheetEntryListAdapter.TimesheetEntryDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() == null || getArguments().getLong(TimesheetFragment.TIMESHEET_ID_KEY) == 0) {
            // timesheet id must be provided, if not, return
            throw new TerexApplicationException("Missing timesheet id!", "50023", null);
        }
        Long timesheetId = getArguments().getLong(TimesheetFragment.TIMESHEET_ID_KEY);

        // Update the cached copy of the timesheet entries in the adapter.
        timesheetEntryViewModel.getTimesheetLiveData(timesheetId).observe(requireActivity(), adapter::submitList);

        TimesheetWithEntries timesheetWithEntries = timesheetEntryViewModel.getTimesheetWithEntries(timesheetId);
        Log.d("all timesheets", "timesheet with entries: " + timesheetWithEntries);

        TextView listHeaderView = view.findViewById(R.id.timesheet_entry_list_header);
        if (timesheetWithEntries != null && timesheetWithEntries.getTimesheet() != null) {
            listHeaderView.setText(String.format("[%s-%s] %s - %s %S", timesheetWithEntries.getTimesheet().getMonth(), timesheetWithEntries.getTimesheet().getYear(), timesheetWithEntries.getTimesheet().getClientName(), timesheetWithEntries.getTimesheet().getProjectCode(),
                    timesheetWithEntries.getTimesheet().getStatus()));
        }

        FloatingActionButton addButton = view.findViewById(R.id.timesheet_entry_add_btn);
        addButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, TimesheetEntryAddFragment.class, createTimesheetEntryBundle(timesheetWithEntries.getTimesheet().getId()))
                    .setReorderingAllowed(true)
                    .commit();
        });

        FloatingActionButton calendarButton = view.findViewById(R.id.timesheet_entry_calendar_btn);
        calendarButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, TimesheetEntryCustomCalendarFragment.class, createTimesheetEntryBundle(timesheetWithEntries.getTimesheet().getId()))
                    .setReorderingAllowed(true)
                    .commit();
        });

        // if timesheet has status closed, it is not possible to do any kind of changes
        if (timesheetWithEntries != null && timesheetWithEntries.getTimesheet().getStatus().equals(Timesheet.TimesheetStatusEnum.BILLED.name())) {
            addButton.setVisibility(View.INVISIBLE);
            calendarButton.setVisibility(View.INVISIBLE);
        }
        // listen after timesheet add and delete events
        //RxBus.getInstance().listen().subscribe(getInputObserver());
        Log.d(Utility.buildTag(getClass(), "onCreateView"), "");
        return view;
    }

    private Bundle createTimesheetEntryBundle(Long timesheetId) {
        TimesheetEntry mostRecentTimesheetEntry = timesheetEntryViewModel.getMostRecentTimesheetEntry(timesheetId);
        if (mostRecentTimesheetEntry == null) {
            mostRecentTimesheetEntry = createDefaultTimesheetEntry(timesheetId);
        }
        String timesheetJson = Utility.gsonMapper().toJson(mostRecentTimesheetEntry, TimesheetEntry.class);
        Bundle bundle = new Bundle();
        bundle.putLong(TimesheetFragment.TIMESHEET_ID_KEY, timesheetId);
        bundle.putString(TimesheetEntryListFragment.TIMESHEET_ENTRY_JSON_KEY, timesheetJson);
        return bundle;
    }

    /**
     * Update backup info after view is successfully create
     */
    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void handleFragmentResult(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (bundle.getString(TIMESHEET_ENTRY_ACTION_KEY) != null) {
            handleTimesheetEntryActions(bundle.getString(TIMESHEET_ENTRY_JSON_KEY), bundle.getString(TIMESHEET_ENTRY_ACTION_KEY));
        } else {
            Log.w("unknown action!", "unknown action: " + bundle);
        }
    }

    private void handleTimesheetEntryActions(String timesheetEntryJson, String action) {
        Log.d(Utility.buildTag(getClass(), "handleTimesheetEntryActions"), String.format("action: %s, timesheet: %s", action, timesheetEntryJson));
        try {
            TimesheetEntry timesheetEntry = Utility.gsonMapper().fromJson(timesheetEntryJson, TimesheetEntry.class);
            if (TIMESHEET_ENTRY_ACTION_SAVE.equals(action)) {
                timesheetEntryViewModel.saveTimesheetEntry(timesheetEntry);
                if (timesheetEntry.getId() == null) {
                    showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_add_msg_format), timesheetEntry.getWorkdayDate()), R.color.color_snackbar_text_add);
                } else {
                    showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_update_msg_format), timesheetEntry.getWorkdayDate()), R.color.color_snackbar_text_update);
                }

            } else if (TIMESHEET_ENTRY_ACTION_DELETE.equals(action)) {
                timesheetEntryViewModel.deleteTimesheetEntry(timesheetEntry);
                showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_delete_msg_format), timesheetEntry.getWorkdayDate()), R.color.color_snackbar_text_delete);
            } else {
                Log.w(Utility.buildTag(getClass(), "handleTimesheetEntryActions"), "unknown action: " + action);
                showInfoDialog(String.format("Application error!%s Unknown action: %s%s Please report.", action, System.lineSeparator(), System.lineSeparator()), getActivity());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfoDialog(String.format("Application error!%sError: %s%s Please report.", ex.getMessage(), System.lineSeparator(), System.lineSeparator()), getActivity());
        }
    }

    private TimesheetEntry createDefaultTimesheetEntry(Long timesheetId) {
        return TimesheetEntry.createDefault(timesheetId, Timesheet.TimesheetStatusEnum.OPEN.name(), Utility.DEFAULT_DAILY_BREAK_IN_MINUTES, LocalDate.now(), Utility.DEFAULT_DAILY_WORKING_HOURS_IN_MINUTES, Utility.DEFAULT_HOURLY_RATE);
    }

    private void showSnackbar(String msg, @ColorRes int bgColor) {
        Resources.Theme theme = getResources().newTheme();
        Snackbar snackbar = Snackbar.make(requireView().findViewById(R.id.timesheet_entry_list_layout), msg, BaseTransientBottomBar.LENGTH_LONG);
        snackbar.setTextColor(getResources().getColor(bgColor, theme));
        snackbar.show();
    }

    private void showInfoDialog(String infoMessage, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Info");
        builder.setMessage(infoMessage);
        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);
        // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Ok", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}