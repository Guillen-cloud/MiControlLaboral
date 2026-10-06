package com.micontrollaboral.presentation;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.micontrollaboral.repository.EmploymentRepository;
import com.micontrollaboral.repository.PaymentRepository;
import com.micontrollaboral.repository.ReportRepository;
import com.micontrollaboral.repository.WorkSessionRepository;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final EmploymentRepository employmentRepository;
    private final WorkSessionRepository workSessionRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;

    public MainViewModelFactory(EmploymentRepository employmentRepository, WorkSessionRepository workSessionRepository, PaymentRepository paymentRepository, ReportRepository reportRepository) {
        this.employmentRepository = employmentRepository;
        this.workSessionRepository = workSessionRepository;
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(employmentRepository, workSessionRepository, paymentRepository, reportRepository);
        }
        throw new IllegalArgumentException("ViewModel no soportado: " + modelClass.getName());
    }
}