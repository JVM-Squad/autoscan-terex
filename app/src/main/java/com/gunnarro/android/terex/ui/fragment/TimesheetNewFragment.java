package com.gunnarro.android.terex.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.gunnarro.android.terex.R;
import com.gunnarro.android.terex.domain.entity.Timesheet;
import com.gunnarro.android.terex.exception.TerexApplicationException;
import com.gunnarro.android.terex.service.RecruitmentService;
import com.gunnarro.android.terex.utility.Utility;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class TimesheetNewFragment extends Fragment implements View.OnClickListener {

    RecruitmentService recruitmentService = new RecruitmentService();

    @Inject
    public TimesheetNewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.title_timesheet);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_timesheet_new, container, false);

        String[] clients = recruitmentService.getRecruitmentNames();
        String[] projects = recruitmentService.getProjectNames();
        Timesheet timesheet = Timesheet.createDefault(clients[0], projects[0], LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        // check if this is an existing or a new timesheet
        String timesheetJson = getArguments() != null ? getArguments().getString(TimesheetListFragment.TIMESHEET_JSON_KEY) : null;
        if (timesheetJson != null && !timesheetJson.isEmpty()) {
            try {
                timesheet = Utility.gsonMapper().fromJson(timesheetJson, Timesheet.class);
                Log.d(Utility.buildTag(getClass(), "onFragmentResult"), String.format("json: %s, timesheet: %s", timesheetJson, timesheet));
            } catch (Exception e) {
                Log.e("", e.toString());
                throw new TerexApplicationException("Application Error!", "5000", e);
            }
        } else {
            Log.d(Utility.buildTag(getClass(), "onFragmentResult"), String.format("default timesheet not found! use timesheet: %s", timesheet));
        }

        // create timesheet client spinner
        final AutoCompleteTextView clientSpinner = view.findViewById(R.id.timesheet_new_client_spinner);
        ArrayAdapter<String> clientAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, clients);
        clientSpinner.setAdapter(clientAdapter);
        clientSpinner.setListSelection(0);

        final AutoCompleteTextView projectSpinner = view.findViewById(R.id.timesheet_new_project_spinner);
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, projects);
        projectSpinner.setAdapter(projectAdapter);
        projectSpinner.setListSelection(0);

        // create timesheet status spinner
        final AutoCompleteTextView statusSpinner = view.findViewById(R.id.timesheet_new_status_spinner);
        ArrayAdapter<CharSequence> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, Timesheet.TimesheetStatusEnum.names());
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setListSelection(0);

        // create timesheet year spinner
        final AutoCompleteTextView yearSpinner = view.findViewById(R.id.timesheet_new_year_spinner);
        ArrayAdapter<CharSequence> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, Utility.getYears());
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setListSelection(0);
        yearSpinner.setOnItemClickListener((parent, view12, position, id) -> Log.d("yearSpinner", "selected: " + yearAdapter.getItem(position)));

        // create timesheet month spinner
        final AutoCompleteTextView monthSpinner = view.findViewById(R.id.timesheet_new_month_spinner);
        ArrayAdapter<CharSequence> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, Utility.getMonthNames());
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setListSelection(0);

        monthSpinner.setOnItemClickListener((parent, view1, position, id) -> {
            Log.d("monthSpinner", "selected: " + monthAdapter.getItem(position));
            AutoCompleteTextView year = requireView().findViewById(R.id.timesheet_new_year_spinner);
            Log.d("monthSpinner", String.format("selected, year=%s, mount=%s", year.getText(), monthAdapter.getItem(position)));
            updateFromToDate(Utility.toLocalDate(year.getText().toString(), monthAdapter.getItem(position).toString(), 1));
        });

        // disable save button as default
        view.findViewById(R.id.btn_timesheet_new_save).setEnabled(true);
        view.findViewById(R.id.btn_timesheet_new_save).setOnClickListener(v -> {
            if (!isInputDataValid()) {
                return;
            }
            view.findViewById(R.id.btn_timesheet_new_save).setBackgroundColor(getResources().getColor(R.color.color_btn_bg_cancel, view.getContext().getTheme()));
            Bundle result = new Bundle();
            result.putString(TimesheetListFragment.TIMESHEET_JSON_KEY, getTimesheetAsJson());
            result.putString(TimesheetListFragment.TIMESHEET_ACTION_KEY, TimesheetListFragment.TIMESHEET_ACTION_SAVE);
            getParentFragmentManager().setFragmentResult(TimesheetListFragment.TIMESHEET_REQUEST_KEY, result);
            returnToTimesheetList();
        });

        view.findViewById(R.id.btn_timesheet_new_delete).setOnClickListener(v -> {
            view.findViewById(R.id.btn_timesheet_new_delete).setBackgroundColor(getResources().getColor(R.color.color_btn_bg_cancel, view.getContext().getTheme()));
            Bundle result = new Bundle();
            result.putString(TimesheetListFragment.TIMESHEET_JSON_KEY, getTimesheetAsJson());
            result.putString(TimesheetListFragment.TIMESHEET_ACTION_KEY, TimesheetListFragment.TIMESHEET_ACTION_DELETE);
            getParentFragmentManager().setFragmentResult(TimesheetListFragment.TIMESHEET_REQUEST_KEY, result);
            Log.d(Utility.buildTag(getClass(), "onCreateView"), "add new delete item intent");
            returnToTimesheetList();
        });

        view.findViewById(R.id.btn_timesheet_new_cancel).setOnClickListener(v -> {
            view.findViewById(R.id.btn_timesheet_new_cancel).setBackgroundColor(getResources().getColor(R.color.color_btn_bg_cancel, view.getContext().getTheme()));
            // Simply return back to credential list
            NavigationView navigationView = requireActivity().findViewById(R.id.navigationView);
            requireActivity().onOptionsItemSelected(navigationView.getMenu().findItem(R.id.nav_timesheet_list));
            returnToTimesheetList();
        });

        if (timesheet != null) {
            updateTimesheetNewView(view, timesheet);
        }

        view.findViewById(R.id.timesheet_new_id).setVisibility(View.GONE);
        view.findViewById(R.id.timesheet_new_created_date).setVisibility(View.GONE);
        view.findViewById(R.id.timesheet_new_last_modified_date_layout).setVisibility(View.GONE);
        Log.d(Utility.buildTag(getClass(), "onCreateView"), String.format("%s", timesheet));
        return view;
    }

    private void returnToTimesheetList() {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, TimesheetListFragment.class, null).setReorderingAllowed(true).commit();
    }

    private void updateTimesheetNewView(View view, @NotNull Timesheet timesheet) {

        TextView timesheetId = view.findViewById(R.id.timesheet_new_id);
        timesheetId.setText(String.valueOf(timesheet.getId()));

        EditText createdDateView = view.findViewById(R.id.timesheet_new_created_date);
        createdDateView.setText(Utility.formatDateTime(timesheet.getCreatedDate()));

        EditText lastModifiedDateView = view.findViewById(R.id.timesheet_new_last_modified_date);
        lastModifiedDateView.setText(Utility.formatDateTime(timesheet.getLastModifiedDate()));

        AutoCompleteTextView clientSpinner = view.findViewById(R.id.timesheet_new_client_spinner);
        clientSpinner.setText(timesheet.getClientName());

        AutoCompleteTextView projectSpinner = view.findViewById(R.id.timesheet_new_project_spinner);
        projectSpinner.setText(timesheet.getProjectCode());

        AutoCompleteTextView statusSpinner = view.findViewById(R.id.timesheet_new_status_spinner);
        statusSpinner.setText(timesheet.getStatus());

        AutoCompleteTextView yearSpinner = view.findViewById(R.id.timesheet_new_year_spinner);
        yearSpinner.setText(String.format("%s", timesheet.getYear()));

        AutoCompleteTextView monthSpinner = view.findViewById(R.id.timesheet_new_month_spinner);
        monthSpinner.setText(Utility.mapMonthNumberToName(timesheet.getMonth() - 1));

        EditText fromTimeView = view.findViewById(R.id.timesheet_new_from_date);
        fromTimeView.setText(Utility.formatDate(timesheet.getFromDate()));

        EditText toTimeView = view.findViewById(R.id.timesheet_new_to_date);
        toTimeView.setText(Utility.formatDate(timesheet.getToDate()));

        EditText descriptionView = view.findViewById(R.id.timesheet_new_description);
        descriptionView.setText(timesheet.getDescription());

        // hide fields if this is a new
        if (timesheet.getId() == null) {
            view.findViewById(R.id.timesheet_new_created_date_layout).setVisibility(View.GONE);
            view.findViewById(R.id.timesheet_new_last_modified_date_layout).setVisibility(View.GONE);
            view.findViewById(R.id.btn_timesheet_new_delete).setVisibility(View.GONE);
        } else if (timesheet.isClosed()) {
            view.findViewById(R.id.timesheet_new_created_date_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.timesheet_new_last_modified_date_layout).setVisibility(View.VISIBLE);
            // disable all fields, timesheet is locked.
            createdDateView.setEnabled(false);
            lastModifiedDateView.setEnabled(false);
            clientSpinner.setEnabled(false);
            projectSpinner.setEnabled(false);
            statusSpinner.setEnabled(false);
            yearSpinner.setEnabled(false);
            monthSpinner.setEnabled(false);
            fromTimeView.setEnabled(false);
            toTimeView.setEnabled(false);
            descriptionView.setEnabled(false);
            view.findViewById(R.id.btn_timesheet_new_delete).setVisibility(View.GONE);
            view.findViewById(R.id.btn_timesheet_new_save).setVisibility(View.GONE);
        } else {
            // change button icon to from add new to save
            ((MaterialButton) view.findViewById(R.id.btn_timesheet_new_save)).setText(getResources().getString(R.string.btn_save));
            // not allowed to change a timesheet with status set equal to BILLED
            if (timesheet.isBilled()) {
                view.findViewById(R.id.btn_timesheet_new_delete).setVisibility(View.GONE);
                view.findViewById(R.id.btn_timesheet_new_save).setVisibility(View.GONE);
            }
            // only allowed to update status
            createdDateView.setEnabled(false);
            lastModifiedDateView.setEnabled(false);
            clientSpinner.setEnabled(false);
            projectSpinner.setEnabled(false);
            statusSpinner.setEnabled(true);
            yearSpinner.setEnabled(false);
            monthSpinner.setEnabled(false);
            fromTimeView.setEnabled(false);
            toTimeView.setEnabled(false);
            descriptionView.setEnabled(true);
        }
        Log.d(Utility.buildTag(getClass(), "updateTimesheetNewView"), String.format("updated %s ", timesheet));
    }

    @Override
    public void onClick(View view) {
        // ask every time
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // You have not been granted access, ask for permission now, no need for nay permissions
            Log.d(Utility.buildTag(getClass(), "onClick"), "save button, permission not granted");
        }

        int id = view.getId();
        if (id == R.id.btn_timesheet_new_save) {
            Log.d(Utility.buildTag(getClass(), "onClick"), "save button, save timesheet");
        } else if (id == R.id.btn_timesheet_new_cancel) {
            // return back to main view
            Log.d(Utility.buildTag(getClass(), "onClick"), "cancel button, return back to timesheet list view");
        }
    }

    private String getTimesheetAsJson() {
        Timesheet timesheet = new Timesheet();

        TextView timesheetIdView = requireView().findViewById(R.id.timesheet_new_id);
        timesheet.setId(Utility.isInteger(timesheetIdView.getText().toString()) ? Long.parseLong(timesheetIdView.getText().toString()) : null);

        EditText createdDateView = requireView().findViewById(R.id.timesheet_new_created_date);
        LocalDateTime createdDateTime = Utility.toLocalDateTime(createdDateView.getText().toString());
        if (createdDateTime != null) {
            timesheet.setCreatedDate(createdDateTime);
        }

        EditText lastModifiedDateView = requireView().findViewById(R.id.timesheet_new_last_modified_date);
        LocalDateTime lastModifiedDateTime = Utility.toLocalDateTime(lastModifiedDateView.getText().toString());
        if (lastModifiedDateTime != null) {
            timesheet.setLastModifiedDate(lastModifiedDateTime);
        }

        AutoCompleteTextView clientSpinner = requireView().findViewById(R.id.timesheet_new_client_spinner);
        timesheet.setClientName(clientSpinner.getText().toString());

        AutoCompleteTextView projectSpinner = requireView().findViewById(R.id.timesheet_new_project_spinner);
        timesheet.setProjectCode(projectSpinner.getText().toString());

        AutoCompleteTextView statusSpinner = requireView().findViewById(R.id.timesheet_new_status_spinner);
        timesheet.setStatus(statusSpinner.getText().toString());

        AutoCompleteTextView yearSpinner = requireView().findViewById(R.id.timesheet_new_year_spinner);
        timesheet.setYear(Integer.parseInt(yearSpinner.getText().toString()));

        AutoCompleteTextView monthSpinner = requireView().findViewById(R.id.timesheet_new_month_spinner);
        timesheet.setMonth(Utility.mapMonthNameToNumber(monthSpinner.getText().toString()));

        TextView fromDateView = requireView().findViewById(R.id.timesheet_new_from_date);
        timesheet.setFromDate(Utility.toLocalDate(fromDateView.getText().toString()));

        TextView toDateView = requireView().findViewById(R.id.timesheet_new_to_date);
        timesheet.setToDate(Utility.toLocalDate(toDateView.getText().toString()));

        TextView descriptionView = requireView().findViewById(R.id.timesheet_new_description);
        timesheet.setDescription(descriptionView.getText().toString());

        timesheet.createTimesheetRef();
        try {
            return Utility.gsonMapper().toJson(timesheet);
        } catch (Exception e) {
            Log.e("getTimesheetAsJson", e.toString());
            throw new TerexApplicationException("unable to parse object to json! ", "50040", e);
        }
    }

    private boolean isInputDataValid() {
        boolean hasValidationError = true;
        AutoCompleteTextView clientSpinner = requireView().findViewById(R.id.timesheet_new_client_spinner);
        if (!hasText(clientSpinner.getText())) {
            clientSpinner.setError(getString(R.string.lbl_required));
            hasValidationError = false;
        }
        AutoCompleteTextView projectSpinner = requireView().findViewById(R.id.timesheet_new_project_spinner);
        if (!hasText(projectSpinner.getText())) {
            projectSpinner.setError(getString(R.string.lbl_required));
            hasValidationError = false;
        }
        AutoCompleteTextView statusSpinner = requireView().findViewById(R.id.timesheet_new_status_spinner);
        if (!hasText(statusSpinner.getText())) {
            statusSpinner.setError(getString(R.string.lbl_required));
            hasValidationError = false;
        }
        AutoCompleteTextView yearSpinner = requireView().findViewById(R.id.timesheet_new_year_spinner);
        if (!hasText(yearSpinner.getText())) {
            yearSpinner.setError(getString(R.string.lbl_required));
            hasValidationError = false;
        }
        AutoCompleteTextView monthSpinner = requireView().findViewById(R.id.timesheet_new_month_spinner);
        if (!hasText(monthSpinner.getText())) {
            monthSpinner.setError(getString(R.string.lbl_required));
            hasValidationError = false;
        }
        // simply check if the timesheet already exist
        return hasValidationError;
    }

    private void updateFromToDate(LocalDate date) {
        TextView fromDateView = requireView().findViewById(R.id.timesheet_new_from_date);
        fromDateView.setText(Utility.formatDate(Utility.getFirstDayOfMonth(date)));

        TextView toDateView = requireView().findViewById(R.id.timesheet_new_to_date);
        toDateView.setText(Utility.formatDate(Utility.getLastDayOfMonth(date)));
    }

    private boolean hasText(Editable e) {
        return e != null && !e.toString().isBlank();
    }

    /**
     * Used for input validation
     */
    private TextWatcher createEmptyTextValidator(EditText editText, String regexp, String validationErrorMsg) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!editText.getText().toString().matches(regexp)) {
                    editText.setError(validationErrorMsg);
                }
            }
        };
    }

}
