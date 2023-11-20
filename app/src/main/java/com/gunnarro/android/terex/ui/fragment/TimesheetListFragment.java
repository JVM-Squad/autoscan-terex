package com.gunnarro.android.terex.ui.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.gunnarro.android.terex.R;
import com.gunnarro.android.terex.domain.entity.Timesheet;
import com.gunnarro.android.terex.exception.TerexApplicationException;
import com.gunnarro.android.terex.ui.adapter.TimesheetListAdapter;
import com.gunnarro.android.terex.ui.dialog.DialogActionListener;
import com.gunnarro.android.terex.ui.swipe.SwipeCallback;
import com.gunnarro.android.terex.ui.view.TimesheetViewModel;
import com.gunnarro.android.terex.utility.Utility;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TimesheetListFragment extends Fragment implements DialogActionListener {
    public static final String TIMESHEET_REQUEST_KEY = "100";
    public static final String TIMESHEET_JSON_KEY = "timesheet_as_json";
    public static final String TIMESHEET_ID_KEY = "timesheet_id";
    public static final String TIMESHEET_ACTION_KEY = "111";
    public static final String TIMESHEET_ACTION_SAVE = "timesheet_save";
    public static final String TIMESHEET_ACTION_DELETE = "timesheet_delete";
    public static final String TIMESHEET_ACTION_EDIT = "timesheet_edit";
    public static final String TIMESHEET_ACTION_VIEW = "timesheet_view";
    private TimesheetViewModel timesheetViewModel;
    private List<Integer> timesheetYears;
    private Integer selectedYear = LocalDate.now().getYear();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setAllowEnterTransitionOverlap(true);
        requireActivity().setTitle(R.string.title_timesheets);
        // todo get from db
        timesheetYears = List.of(2023, 2024, 2025);
        selectedYear = LocalDate.now().getYear();
        // Get a new or existing ViewModel from the ViewModelProvider.
        try {
            timesheetViewModel = new TimesheetViewModel(getActivity().getApplication(), selectedYear);//new ViewModelProvider(this).get(TimesheetViewModel.class);
        } catch (Exception e) {
            throw new TerexApplicationException("Error creating fragment!", "50051", e);
        }
        // register listener for add, update and delete timesheet events
        getParentFragmentManager().setFragmentResultListener(TIMESHEET_REQUEST_KEY, this, new FragmentResultListener() {
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
        View view = inflater.inflate(R.layout.fragment_recycler_timesheet_list, container, false);
        setHasOptionsMenu(true);
        RecyclerView recyclerView = view.findViewById(R.id.timesheet_list_recyclerview);
        TimesheetListAdapter timesheetListAdapter = new TimesheetListAdapter(getParentFragmentManager(), new TimesheetListAdapter.TimesheetDiff());
        recyclerView.setAdapter(timesheetListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Update the cached copy of the timesheet entries in the adapter.
        timesheetViewModel.getTimesheetLiveData(selectedYear).observe(requireActivity(), timesheetListAdapter::submitList);

        FloatingActionButton addButton = view.findViewById(R.id.timesheet_add_btn);
        addButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, TimesheetNewFragment.class, createTimesheetBundle(timesheetListAdapter.getItemId(0), selectedYear))
                    .setReorderingAllowed(true)
                    .commit();
        });

        // enable swipe
        enableSwipeToLeftAndDeleteItem(recyclerView);
        enableSwipeToRightAndViewItem(recyclerView);
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
    public void onDestroyView() {
        super.onDestroyView();
    }

    private Bundle createTimesheetBundle(Long timesheetId, Integer year) {
        Timesheet timesheet = timesheetViewModel.getTimesheet(timesheetId);
        if (timesheet == null) {
            timesheet = Timesheet.createDefault(null, null, year, LocalDate.now().getMonthValue());
        }
        String timesheetJson = Utility.gsonMapper().toJson(timesheet, Timesheet.class);
        Bundle bundle = new Bundle();
        bundle.putString(TimesheetListFragment.TIMESHEET_JSON_KEY, timesheetJson);
        return bundle;
    }

    private void handleFragmentResult(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (bundle.getString(TIMESHEET_ACTION_KEY) != null) {
            handleTimesheetActions(bundle.getString(TIMESHEET_JSON_KEY), bundle.getString(TIMESHEET_ACTION_KEY));
        } else {
            Log.w("unknown action!", "unknown action: " + bundle);
        }
    }

    private void handleTimesheetActions(String timesheetJson, String action) {
        Log.d(Utility.buildTag(getClass(), "handleTimesheetActions"), String.format("action: %s, timesheet: %s", action, timesheetJson));
        try {
            Timesheet timesheet = Utility.gsonMapper().fromJson(timesheetJson, Timesheet.class);
            if (TIMESHEET_ACTION_SAVE.equals(action)) {
                timesheetViewModel.saveTimesheet(timesheet);
                showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_saved_msg_format), timesheet.getTimesheetRef()), R.color.color_snackbar_text_add);
            } else if (TIMESHEET_ACTION_DELETE.equals(action)) {
                if (timesheet.getStatus().equals("BILLED")) {
                    showInfoDialog("Info", "Can not delete timesheet with status BILLED", requireContext());
                } else {
                    confirmDeleteTimesheetDialog(getString(R.string.msg_delete_timesheet), getString(R.string.msg_confirm_delete), timesheet.getId());
                }
            } else if (TIMESHEET_ACTION_VIEW.equals(action)) {
                // redirect to timesheet entry list fragment
                Bundle bundle = new Bundle();
                bundle.putLong(TimesheetListFragment.TIMESHEET_ID_KEY, timesheet.getId());
                goToTimesheetEntryView(bundle);
            } else if (TIMESHEET_ACTION_EDIT.equals(action)) {
                // redirect to timesheet entry list fragment
                Bundle bundle = new Bundle();
                bundle.putString(TimesheetListFragment.TIMESHEET_JSON_KEY, timesheetJson);
                goToTimesheetView(bundle);
            } else {
                Log.w(Utility.buildTag(getClass(), "handleTimesheetActions"), "unknown action: " + action);
                showInfoDialog("Info", String.format("Application error!%s Unknown action: %s%s Please report.", action, System.lineSeparator(), System.lineSeparator()), getActivity());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfoDialog("Error", String.format("Application error!%sError: %s%s Please report.", ex.getMessage(), System.lineSeparator(), System.lineSeparator()), getActivity());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(Utility.buildTag(getClass(), "onOptionsItemSelected"), "selected: " + item.getTitle());
        handleOptionsMenuSelection(selectedYear);
        return true;
    }

    private void handleOptionsMenuSelection(Integer selectedYear) {
        // reload list with selected timesheet year
        Log.d("handleOptionsMenuSelection", "selected year: " + selectedYear);
        reloadTimesheetData(selectedYear);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // keep a reference to options menu
        //optionsMenu = menu;
        // clear current menu items
        menu.clear();
        // set fragment specific menu items
        inflater.inflate(R.menu.timesheet_menu, menu);
        MenuItem m = menu.findItem(R.id.year_dropdown_menu);
        Spinner spinner = (Spinner) m.getActionView();
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, timesheetYears);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = timesheetYears.get(position);
                handleOptionsMenuSelection(selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Log.d(Utility.buildTag(getClass(), "onCreateOptionsMenu"), "menu: " + menu);
    }

    private void reloadTimesheetData(Integer selectedYear) {
        RecyclerView recyclerView = requireView().findViewById(R.id.timesheet_list_recyclerview);
        TimesheetListAdapter timesheetListAdapter = (TimesheetListAdapter) recyclerView.getAdapter();
        timesheetViewModel.getTimesheetLiveData(selectedYear).observe(requireActivity(), timesheetListAdapter::submitList);
        timesheetListAdapter.notifyDataSetChanged();
    }

    private void goToTimesheetEntryView(Bundle bundle) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, TimesheetEntryListFragment.class, bundle)
                .setReorderingAllowed(true)
                .commit();
    }

    private void goToTimesheetView(Bundle bundle) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, TimesheetNewFragment.class, bundle)
                .setReorderingAllowed(true)
                .commit();
    }

    private void deleteTimesheet(Long timesheetId) {
        Timesheet timesheet = timesheetViewModel.getTimesheet(timesheetId);
        timesheetViewModel.deleteTimesheet(timesheet);
        showSnackbar(String.format(getResources().getString(R.string.info_timesheet_list_delete_msg_format), timesheet.getTimesheetRef()), R.color.color_snackbar_text_delete);
    }

    private void showSnackbar(String msg, @ColorRes int bgColor) {
        Resources.Theme theme = getResources().newTheme();
        Snackbar snackbar = Snackbar.make(requireView().findViewById(R.id.timesheet_list_layout), msg, BaseTransientBottomBar.LENGTH_LONG);
        snackbar.setTextColor(getResources().getColor(bgColor, theme));
        snackbar.show();
    }

    private void showInfoDialog(String severity, String message, Context context) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(severity)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void enableSwipeToRightAndViewItem(RecyclerView recyclerView) {
        SwipeCallback swipeToDeleteCallback = new SwipeCallback(requireContext(), ItemTouchHelper.RIGHT, getResources().getColor(R.color.color_bg_swipe_right, null), R.drawable.ic_add_black_24dp) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                LiveData<List<Timesheet>> listLiveData = timesheetViewModel.getTimesheetLiveData(selectedYear);
                int pos = viewHolder.getAbsoluteAdapterPosition();
                List<Timesheet> list = listLiveData.getValue();
                openTimesheetView(list.get(pos));
                Bundle bundle = new Bundle();
                bundle.putLong(TimesheetListFragment.TIMESHEET_ID_KEY, list.get(pos).getId());
                goToTimesheetEntryView(bundle);
            }
        };
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
        Log.i(Utility.buildTag(getClass(), "enableSwipeToRightAndAdd"), "enabled swipe handler for timesheet list item");
    }

    private void openTimesheetView(Timesheet timesheet) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, TimesheetNewFragment.class, createTimesheetBundle(timesheet.getId(), selectedYear))
                .setReorderingAllowed(true)
                .commit();
    }

    private void enableSwipeToLeftAndDeleteItem(RecyclerView recyclerView) {
        SwipeCallback swipeToDeleteCallback = new SwipeCallback(requireContext(), ItemTouchHelper.LEFT, getResources().getColor(R.color.color_bg_swipe_left, null), R.drawable.ic_delete_black_24dp) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAbsoluteAdapterPosition();
                Timesheet timesheet = timesheetViewModel.getTimesheetLiveData(selectedYear).getValue().get(position);
                confirmDeleteTimesheetDialog(getString(R.string.msg_delete_timesheet), getString(R.string.msg_confirm_delete), timesheet.getId());
            }
        };
        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
        Log.i(Utility.buildTag(getClass(), "enableSwipeToLeftAndDeleteItem"), "enabled swipe handler for delete timesheet list item");
    }

    /**
     * Pick up confirm dialog fragment events
     */
    @Override
    public void onDialogAction(int actionCode, Long entityId) {
        if (actionCode == DialogActionListener.OK_ACTION) {
            // the user confirmed the operation
            deleteTimesheet(entityId);
        } else {
            // dismiss, do nothing, the user canceled the operation
            Log.d(Utility.buildTag(getClass(), "onDialogAction"), "delete sms backup file action cancelled by user");
        }
    }

    private void confirmDeleteTimesheetDialog(final String title, final String message, final Long timesheetId) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.btn_ok, (dialogInterface, i) -> {
                    deleteTimesheet(timesheetId);
                })
                .setNeutralButton(R.string.btn_cancel, (dialogInterface, i) -> {
                    // nothing to do
                })
                .show();
    }
}
