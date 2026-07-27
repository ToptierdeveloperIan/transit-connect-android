package com.example.imanicommunityapp.auth.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.imanicommunityapp.auth.Repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> otpRequested = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccessful = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getOtpRequested() {
        return otpRequested;
    }

    public LiveData<Boolean> getLoginSuccessful() {
        return loginSuccessful;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void requestOtp(String phoneNumber) {
        loading.setValue(true);
        authRepository.requestOtp(phoneNumber, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                otpRequested.postValue(true);
                message.postValue("OTP sent successfully");
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                message.postValue(errorMessage);
            }
        });
    }

    public void loginWithOtp(String phoneNumber, String otp) {
        loading.setValue(true);
        authRepository.loginWithOtp(phoneNumber, otp, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                loginSuccessful.postValue(true);
                message.postValue("Login successful");
            }

            @Override
            public void onError(String errorMessage) {
                loading.postValue(false);
                message.postValue(errorMessage);
            }
        });
    }

    public void onMessageShown() {
        message.setValue(null);
    }

    public void onNavigationHandled() {
        loginSuccessful.setValue(false);
    }
}
