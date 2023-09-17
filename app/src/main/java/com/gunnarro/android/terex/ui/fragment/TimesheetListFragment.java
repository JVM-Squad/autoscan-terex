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
import com.gunnarro.android.terex.ui.adapter.TimesheetListAdapter;
import com.gunnarro.android.terex.ui.view.TimesheetViewModel;
import com.gunnarro.android.terex.utility.Utility;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TimesheetListFragment extends Fragment {
    public static final String TIMESHEET_JSON_INTENT_KEY = "timesheet_as_json";
    public static final String TIMESHEET_REQUEST_KEY = "1";
    public static final String TIMESHEET_ACTION_KEY = "11";
    public static final String TIMESHEET_ACTION_SAVE = "timesheet_save";
    public static final String TIMESHEET_ACTION_DELETE = "timesheet_delete";
    private TimesheetViewModel timesheetViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get a new or existing ViewModel from the ViewModelProvider.
        timesheetViewModel = new ViewModelProvider(this).get(TimesheetViewModel.class);

        getParentFragmentManager().setFragmentResultListener(TIMESHEET_REQUEST_KEY, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                Log.d(Utility.buildTag(getClass(), "onFragmentResult"), "intent: " + requestKey + "json:  + " + bundle.getString(TIMESHEET_JSON_INTENT_KEY));
                try {
                    Timesheet timesheet = Utility.gsonMapper().fromJson(bundle.getString(TIMESHEET_JSON_INTENT_KEY), Timesheet.class);
                    handleButtonActions(timesheet, bundle.getString(TIMESHEET_ACTION_KEY));
                    Log.d(Utility.buildTag(getClass(), "onFragmentResult"), String.format("action: %s, timesheet: %s", bundle.getString(TIMESHEET_ACTION_KEY), timesheet));
                } catch (Exception e) {
                    Log.e("", e.toString());
                    showInfoDialog(String.format("Application error!%s Error: %s%sErrorCode: 5001%sPlease report.", e.getMessage(), System.lineSeparator(), System.lineSeparator(), System.lineSeparator()), getActivity());
                }
            }
        });
        Log.d(Utility.buildTag(getClass(), "onCreate"), "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recycler_timesheet_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.timesheet_recyclerview);
        final TimesheetListAdapter adapter = new TimesheetListAdapter(getParentFragmentManager(), new TimesheetListAdapter.TimesheetDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //recyclerView.setOnClickListener(v -> Log.d("", "clicked on list item...."));

        // Add an observer on the LiveData returned by getAlphabetizedWords.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        // Update the cached copy of the words in the adapter.
        timesheetViewModel.getTimesheetLiveData().observe(requireActivity(), adapter::submitList);

        FloatingActionButton addButton = view.findViewById(R.id.add_timesheet);
        addButton.setOnClickListener(v -> {
            String timesheetJson = Utility.gsonMapper().toJson(timesheetViewModel.getMostRecent(), Timesheet.class);
            Bundle bundle = new Bundle();
            bundle.putString(TimesheetListFragment.TIMESHEET_JSON_INTENT_KEY, timesheetJson);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, TimesheetAddFragment.class, bundle)
                    .setReorderingAllowed(true)
                    .commit();
        });

        FloatingActionButton calendarButton = view.findViewById(R.id.view_timesheet_calendar);
        calendarButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, TimesheetCalendarFragment.class, bundle)
                    .setReorderingAllowed(true)
                    .commit();
        });

        Log.d(Utility.buildTag(getClass(), "onCreateView"), "");
        return view;
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

    private void handleButtonActions(Timesheet timesheet, String action) {
        if (TIMESHEET_ACTION_SAVE.equals(action)) {
            try {
                timesheetViewModel.save(timesheet);
                if (timesheet.getId() == null) {
                    showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_add_msg_format), timesheet.getWorkdayDate()), R.color.color_snackbar_text_add);
                } else {
                    showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_update_msg_format), timesheet.getWorkdayDate()), R.color.color_snackbar_text_update);
                }
            } catch (Exception ex) {
                showInfoDialog(String.format("Application error!%sError: %s%s Please report.", ex.getMessage(), System.lineSeparator(), System.lineSeparator()), getActivity());
            }
        } else if (TIMESHEET_ACTION_DELETE.equals(action)) {
            timesheetViewModel.delete(timesheet);
            showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_delete_msg_format), timesheet.getWorkdayDate()), R.color.color_snackbar_text_delete);
        } else {
            Log.w(Utility.buildTag(getClass(), "onFragmentResult"), "unknown action: " + action);
            showInfoDialog(String.format("Application error!%s Unknown action: %s%s Please report.", action, System.lineSeparator(), System.lineSeparator()), getActivity());
        }
    }

    private void showSnackbar(String msg, @ColorRes int bgColor) {
        Resources.Theme theme = getResources().newTheme();
        Snackbar snackbar = Snackbar.make(requireView().findViewById(R.id.timesheet_list_layout), msg, BaseTransientBottomBar.LENGTH_LONG);
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
