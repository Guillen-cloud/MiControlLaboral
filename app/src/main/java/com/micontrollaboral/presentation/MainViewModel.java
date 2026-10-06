package com.micontrollaboral.presentation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.micontrollaboral.database.EmploymentEntity;
import com.micontrollaboral.database.WorkSessionEntity;
import com.micontrollaboral.repository.EmploymentRepository;
import com.micontrollaboral.repository.PaymentRepository;
import com.micontrollaboral.repository.ReportRepository;
import com.micontrollaboral.repository.WorkSessionRepository;

import java.util.List;

public class MainViewModel extends ViewModel {
    private final LiveData<List<EmploymentEntity>> employments;
    private final LiveData<List<WorkSessionEntity>> workSessions;
    private final LiveData<Integer> generatedAmountCents;
    private final LiveData<Integer> allocatedAmountCents;
    private final WorkSessionRepository workSessionRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;

    public MainViewModel(EmploymentRepository employmentRepository, WorkSessionRepository workSessionRepository, PaymentRepository paymentRepository, ReportRepository reportRepository) {
        employments = employmentRepository.observeActiveEmployments();
        this.workSessionRepository = workSessionRepository;
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
        workSessions = workSessionRepository.observeActiveWorkSessions();
        generatedAmountCents = workSessionRepository.observeGeneratedAmountCents();
        allocatedAmountCents = paymentRepository.observeAllocatedAmountCents();
        employmentRepository.ensureDefaultEmployments();
    }

    public LiveData<List<EmploymentEntity>> getEmployments() {
        return employments;
    }

    public LiveData<List<WorkSessionEntity>> getWorkSessions() {
        return workSessions;
    }

    public LiveData<Integer> getGeneratedAmountCents() {
        return generatedAmountCents;
    }

    public LiveData<Integer> getAllocatedAmountCents() {
        return allocatedAmountCents;
    }

    public void registerWorkSession(String employmentId, String workedDate, String sessionType, int amountCents, String note, WorkSessionRepository.SaveCallback callback) {
        workSessionRepository.register(employmentId, workedDate, sessionType, amountCents, note, callback);
    }

    public void updateWorkSession(WorkSessionEntity workSession, WorkSessionRepository.SaveCallback callback) {
        workSessionRepository.update(workSession, callback);
    }

    public void registerPayment(String workSessionId, String receivedAt, int amountCents, String note, PaymentRepository.SaveCallback callback) {
        paymentRepository.register(workSessionId, receivedAt, amountCents, note, callback);
    }

    public void registerGroupedPayment(String receivedAt, String note, List<PaymentRepository.AllocationInput> allocations, PaymentRepository.SaveCallback callback) {
        paymentRepository.registerGrouped(receivedAt, note, allocations, callback);
    }

    public void generateReport(String startDate, String endDate, ReportRepository.ReportCallback callback) {
        reportRepository.generate(startDate, endDate, callback);
    }
}