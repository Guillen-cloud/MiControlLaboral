package com.micontrollaboral.presentation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.micontrollaboral.R;
import com.micontrollaboral.MiControlLaboralApplication;
import com.google.firebase.auth.FirebaseAuth;
import com.micontrollaboral.database.AppDatabase;
import com.micontrollaboral.database.EmploymentEntity;
import com.micontrollaboral.database.WorkSessionEntity;
import com.micontrollaboral.repository.EmploymentRepository;
import com.micontrollaboral.repository.PaymentRepository;
import com.micontrollaboral.repository.BackupRepository;
import com.micontrollaboral.repository.ReportRepository;
import com.micontrollaboral.repository.WorkSessionRepository;
import com.micontrollaboral.domain.ReportSummary;
import com.micontrollaboral.domain.EmploymentReportRow;
import com.micontrollaboral.utils.MoneyUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {
    private MainViewModel viewModel;
    private final List<EmploymentEntity> employments = new ArrayList<>();
    private final List<WorkSessionEntity> workSessions = new ArrayList<>();
    private final List<EditText> groupedAllocationInputs = new ArrayList<>();
    private int latestGeneratedAmountCents;
    private int latestAllocatedAmountCents;
    private BackupRepository backupRepository;
    private final ActivityResultLauncher<String> exportBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) backupRepository.exportTo(uri, backupCallback());
            });
    private final ActivityResultLauncher<String[]> importBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) showRestoreConfirmation(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        AppDatabase database = ((MiControlLaboralApplication) getApplication()).getDatabase(ownerUid);
        backupRepository = new BackupRepository(database, getContentResolver());
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(
                new EmploymentRepository(database.employmentDao(), database.syncRecordDao()),
                new WorkSessionRepository(database.workSessionDao(), database.syncRecordDao()),
                new PaymentRepository(database, database.syncRecordDao()),
                new ReportRepository(database)
        )).get(MainViewModel.class);

        viewModel.getEmployments().observe(this, values -> {
            employments.clear();
            if (values != null) {
                employments.addAll(values);
            }
            renderEmployments();
        });

        viewModel.getWorkSessions().observe(this, values -> {
            workSessions.clear();
            if (values != null) {
                workSessions.addAll(values);
            }
            renderWorkSessions();
        });

        viewModel.getGeneratedAmountCents().observe(this, amountCents -> {
            latestGeneratedAmountCents = amountCents == null ? 0 : amountCents;
            TextView generatedAmount = findViewById(R.id.generated_amount);
            generatedAmount.setText(getString(R.string.generated_amount, latestGeneratedAmountCents / 100.0));
            renderPendingAmount();
        });

        viewModel.getAllocatedAmountCents().observe(this, amountCents -> {
            latestAllocatedAmountCents = amountCents == null ? 0 : amountCents;
            TextView paidAmount = findViewById(R.id.paid_amount);
            paidAmount.setText(getString(R.string.paid_amount, latestAllocatedAmountCents / 100.0));
            renderPendingAmount();
        });

        findViewById(R.id.register_work_session_button).setOnClickListener(view -> showRegisterDialog());
        findViewById(R.id.register_payment_button).setOnClickListener(view -> showPaymentDialog());
        findViewById(R.id.register_grouped_payment_button).setOnClickListener(view -> showGroupedPaymentDialog());
        findViewById(R.id.reports_button).setOnClickListener(view -> showReportDialog());
        findViewById(R.id.export_backup_button).setOnClickListener(view -> exportBackupLauncher.launch("mi-control-laboral-respaldo.json"));
        findViewById(R.id.import_backup_button).setOnClickListener(view -> importBackupLauncher.launch(new String[]{"application/json", "text/json"}));
    }

    private BackupRepository.Callback backupCallback() {
        return new BackupRepository.Callback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void showRestoreConfirmation(android.net.Uri source) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.restore_backup_title)
                .setMessage(R.string.restore_backup_warning)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> backupRepository.importFrom(source, backupCallback()))
                .show();
    }

    private void renderPendingAmount() {
        int pendingAmountCents = Math.max(0, latestGeneratedAmountCents - latestAllocatedAmountCents);
        TextView pendingAmount = findViewById(R.id.pending_amount);
        pendingAmount.setText(getString(R.string.pending_amount, pendingAmountCents / 100.0));
    }

    private void renderEmployments() {
        LinearLayout employmentList = findViewById(R.id.employment_list);
        employmentList.removeAllViews();
        for (EmploymentEntity employment : employments) {
            TextView item = new TextView(this);
            item.setText(getString(R.string.employment_summary, employment.name, employment.fullRateCents / 100));
            item.setPadding(0, 16, 0, 16);
            employmentList.addView(item);
        }
    }

    private void renderWorkSessions() {
        LinearLayout sessionList = findViewById(R.id.session_list);
        sessionList.removeAllViews();
        for (WorkSessionEntity workSession : workSessions) {
            TextView item = new TextView(this);
            item.setText(getString(R.string.work_session_summary,
                    employmentName(workSession.employmentId),
                    workSession.workedDate,
                    workSession.sessionType,
                    workSession.amountCents / 100.0));
            item.setPadding(0, 16, 0, 16);
            item.setOnClickListener(view -> showEditDialog(workSession));
            sessionList.addView(item);
        }
    }

    private String employmentName(String employmentId) {
        for (EmploymentEntity employment : employments) {
            if (employment.id.equals(employmentId)) {
                return employment.name;
            }
        }
        return getString(R.string.unknown_employment);
    }

    private void showRegisterDialog() {
        if (employments.isEmpty()) {
            Toast.makeText(this, R.string.employments_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        View form = LayoutInflater.from(this).inflate(R.layout.dialog_register_work_session, null);
        Spinner employmentSpinner = form.findViewById(R.id.employment_spinner);
        EditText dateInput = form.findViewById(R.id.worked_date_input);
        RadioGroup typeGroup = form.findViewById(R.id.session_type_group);
        EditText amountInput = form.findViewById(R.id.amount_input);
        EditText noteInput = form.findViewById(R.id.note_input);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employmentNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        employmentSpinner.setAdapter(adapter);
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> amountInput.setEnabled(checkedId == R.id.custom_session_radio));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.register_work_session)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> saveWorkSession(employmentSpinner, dateInput, typeGroup, amountInput, noteInput))
                .show();
    }

    private List<String> employmentNames() {
        List<String> names = new ArrayList<>();
        for (EmploymentEntity employment : employments) {
            names.add(employment.name);
        }
        return names;
    }

    private void saveWorkSession(Spinner employmentSpinner, EditText dateInput, RadioGroup typeGroup, EditText amountInput, EditText noteInput) {
        try {
            String workedDate = dateInput.getText().toString().trim();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setLenient(false);
            Date parsedDate = dateFormat.parse(workedDate);
            if (!dateFormat.format(parsedDate).equals(workedDate)) {
                throw new ParseException("Formato de fecha inválido", 0);
            }
            EmploymentEntity employment = employments.get(employmentSpinner.getSelectedItemPosition());
            int selectedType = typeGroup.getCheckedRadioButtonId();
            String sessionType;
            int amountCents;
            if (selectedType == R.id.half_session_radio) {
                sessionType = "MEDIA";
                amountCents = employment.fullRateCents / 2;
            } else if (selectedType == R.id.custom_session_radio) {
                sessionType = "PERSONALIZADA";
                amountCents = MoneyUtils.bolivianosToCents(amountInput.getText().toString());
            } else {
                sessionType = "COMPLETA";
                amountCents = employment.fullRateCents;
            }
            viewModel.registerWorkSession(employment.id, workedDate, sessionType, amountCents, noteInput.getText().toString().trim(), new WorkSessionRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, R.string.work_session_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (ParseException | NumberFormatException | IndexOutOfBoundsException exception) {
            Toast.makeText(this, R.string.invalid_work_session, Toast.LENGTH_LONG).show();
        }
    }

    private void showEditDialog(WorkSessionEntity workSession) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_register_work_session, null);
        EditText dateInput = form.findViewById(R.id.worked_date_input);
        RadioGroup typeGroup = form.findViewById(R.id.session_type_group);
        EditText amountInput = form.findViewById(R.id.amount_input);
        EditText noteInput = form.findViewById(R.id.note_input);
        Spinner employmentSpinner = form.findViewById(R.id.employment_spinner);
        employmentSpinner.setVisibility(View.GONE);
        dateInput.setText(workSession.workedDate);
        noteInput.setText(workSession.note == null ? "" : workSession.note);
        if ("MEDIA".equals(workSession.sessionType)) {
            typeGroup.check(R.id.half_session_radio);
        } else if ("PERSONALIZADA".equals(workSession.sessionType)) {
            typeGroup.check(R.id.custom_session_radio);
            amountInput.setEnabled(true);
            amountInput.setText(String.format(Locale.US, "%.2f", workSession.amountCents / 100.0));
        } else {
            typeGroup.check(R.id.full_session_radio);
        }
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> amountInput.setEnabled(checkedId == R.id.custom_session_radio));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.edit_work_session)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> updateWorkSession(workSession, dateInput, typeGroup, amountInput, noteInput))
                .show();
    }

    private void updateWorkSession(WorkSessionEntity workSession, EditText dateInput, RadioGroup typeGroup, EditText amountInput, EditText noteInput) {
        try {
            String workedDate = dateInput.getText().toString().trim();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setLenient(false);
            Date parsedDate = dateFormat.parse(workedDate);
            if (!dateFormat.format(parsedDate).equals(workedDate)) {
                throw new ParseException("Formato de fecha inválido", 0);
            }
            int selectedType = typeGroup.getCheckedRadioButtonId();
            if (selectedType == R.id.half_session_radio) {
                workSession.sessionType = "MEDIA";
            } else if (selectedType == R.id.custom_session_radio) {
                workSession.sessionType = "PERSONALIZADA";
                workSession.amountCents = MoneyUtils.bolivianosToCents(amountInput.getText().toString());
            } else {
                workSession.sessionType = "COMPLETA";
            }
            if (selectedType != R.id.custom_session_radio) {
                for (EmploymentEntity employment : employments) {
                    if (employment.id.equals(workSession.employmentId)) {
                        workSession.amountCents = selectedType == R.id.half_session_radio ? employment.fullRateCents / 2 : employment.fullRateCents;
                        break;
                    }
                }
            }
            workSession.workedDate = workedDate;
            workSession.note = noteInput.getText().toString().trim();
            viewModel.updateWorkSession(workSession, new WorkSessionRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, R.string.work_session_updated, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (ParseException | NumberFormatException | IndexOutOfBoundsException exception) {
            Toast.makeText(this, R.string.invalid_work_session, Toast.LENGTH_LONG).show();
        }
    }

    private void showPaymentDialog() {
        if (workSessions.isEmpty()) {
            Toast.makeText(this, R.string.no_work_sessions_for_payment, Toast.LENGTH_SHORT).show();
            return;
        }
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_register_payment, null);
        Spinner sessionSpinner = form.findViewById(R.id.payment_session_spinner);
        EditText dateInput = form.findViewById(R.id.payment_date_input);
        EditText amountInput = form.findViewById(R.id.payment_amount_input);
        EditText noteInput = form.findViewById(R.id.payment_note_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentSessionNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sessionSpinner.setAdapter(adapter);
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.register_payment)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> savePayment(sessionSpinner, dateInput, amountInput, noteInput))
                .show();
    }

    private List<String> paymentSessionNames() {
        List<String> names = new ArrayList<>();
        for (WorkSessionEntity workSession : workSessions) {
            names.add(getString(R.string.payment_session_summary, employmentName(workSession.employmentId), workSession.workedDate, workSession.amountCents / 100.0));
        }
        return names;
    }

    private void savePayment(Spinner sessionSpinner, EditText dateInput, EditText amountInput, EditText noteInput) {
        try {
            String receivedAt = dateInput.getText().toString().trim();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setLenient(false);
            Date parsedDate = dateFormat.parse(receivedAt);
            if (!dateFormat.format(parsedDate).equals(receivedAt)) {
                throw new ParseException("Formato de fecha inválido", 0);
            }
            WorkSessionEntity workSession = workSessions.get(sessionSpinner.getSelectedItemPosition());
            int amountCents = MoneyUtils.bolivianosToCents(amountInput.getText().toString());
            viewModel.registerPayment(workSession.id, receivedAt, amountCents, noteInput.getText().toString().trim(), new PaymentRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, R.string.payment_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (ParseException | NumberFormatException | IndexOutOfBoundsException exception) {
            Toast.makeText(this, R.string.invalid_payment, Toast.LENGTH_LONG).show();
        }
    }

    private void showGroupedPaymentDialog() {
        if (workSessions.isEmpty()) {
            Toast.makeText(this, R.string.no_work_sessions_for_payment, Toast.LENGTH_SHORT).show();
            return;
        }
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_register_grouped_payment, null);
        LinearLayout allocationRows = form.findViewById(R.id.grouped_allocation_rows);
        EditText dateInput = form.findViewById(R.id.grouped_payment_date_input);
        EditText noteInput = form.findViewById(R.id.grouped_payment_note_input);
        groupedAllocationInputs.clear();
        for (WorkSessionEntity workSession : workSessions) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView description = new TextView(this);
            description.setText(getString(R.string.payment_session_summary, employmentName(workSession.employmentId), workSession.workedDate, workSession.amountCents / 100.0));
            description.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            EditText amount = new EditText(this);
            amount.setHint(R.string.allocation_amount_hint);
            amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            amount.setLayoutParams(new LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT));
            groupedAllocationInputs.add(amount);
            row.addView(description);
            row.addView(amount);
            allocationRows.addView(row);
        }
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.register_grouped_payment)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> saveGroupedPayment(dateInput, noteInput))
                .show();
    }

    private void saveGroupedPayment(EditText dateInput, EditText noteInput) {
        try {
            String receivedAt = dateInput.getText().toString().trim();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setLenient(false);
            Date parsedDate = dateFormat.parse(receivedAt);
            if (!dateFormat.format(parsedDate).equals(receivedAt)) {
                throw new ParseException("Formato de fecha inválido", 0);
            }
            List<PaymentRepository.AllocationInput> allocations = new ArrayList<>();
            for (int index = 0; index < groupedAllocationInputs.size(); index++) {
                String value = groupedAllocationInputs.get(index).getText().toString().trim();
                if (!value.isEmpty()) {
                    allocations.add(new PaymentRepository.AllocationInput(workSessions.get(index).id, MoneyUtils.bolivianosToCents(value)));
                }
            }
            viewModel.registerGroupedPayment(receivedAt, noteInput.getText().toString().trim(), allocations, new PaymentRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, R.string.payment_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (ParseException | NumberFormatException | IndexOutOfBoundsException exception) {
            Toast.makeText(this, R.string.invalid_payment, Toast.LENGTH_LONG).show();
        }
    }

    private void showReportDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_report, null);
        Spinner periodSpinner = form.findViewById(R.id.report_period_spinner);
        EditText startInput = form.findViewById(R.id.report_start_input);
        EditText endInput = form.findViewById(R.id.report_end_input);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.report_periods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);
        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    setPeriodInputs(position, startInput, endInput);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        setPeriodInputs(1, startInput, endInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.reports_title)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.generate_report, (dialog, which) -> generateReport(startInput, endInput))
                .show();
    }

    private void setPeriodInputs(int period, EditText startInput, EditText endInput) {
        LocalDate today = LocalDate.now(ZoneId.of("America/La_Paz"));
        LocalDate start;
        LocalDate end;
        if (period == 1) {
            start = today.with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else if (period == 2) {
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(today.lengthOfMonth());
        } else {
            start = today;
            end = today;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        startInput.setText(start.format(formatter));
        endInput.setText(end.format(formatter));
    }

    private void generateReport(EditText startInput, EditText endInput) {
        String startDate = startInput.getText().toString().trim();
        String endDate = endInput.getText().toString().trim();
        viewModel.generateReport(startDate, endDate, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(ReportSummary summary) {
                StringBuilder reportText = new StringBuilder(getString(R.string.report_result, summary.startDate, summary.endDate,
                    summary.generatedAmountCents / 100.0,
                    summary.allocatedAmountCents / 100.0,
                    summary.receivedAmountCents / 100.0,
                    summary.getPendingAmountCents() / 100.0));
                reportText.append("\n\n").append(getString(R.string.report_by_employment_title));
                for (EmploymentReportRow row : summary.employmentRows) {
                    reportText.append("\n").append(getString(R.string.report_by_employment_row, row.employmentName,
                        row.generatedAmountCents / 100.0,
                        row.allocatedAmountCents / 100.0,
                        row.getPendingAmountCents() / 100.0));
                }
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.report_result_title)
                    .setMessage(reportText.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}