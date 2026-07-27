package com.example.imanicommunityapp.supportProfile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.RetrofitClient;
import com.example.imanicommunityapp.auth.Model.OTPVerifyModel;
import com.example.imanicommunityapp.auth.Model.PhoneLoginModel;
import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.example.imanicommunityapp.auth.Repository.UserProfileRepository;
import com.example.imanicommunityapp.auth.Model.VerifyOTPResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class editPhoneNo extends Fragment {

    private EditText editPhoneNumber, editOTP;
    private Button btnConfirm, btnVerifyOTP;
    private ImageView backBtn;
    private EditProfile apiService;

    private TokenManager tokenManager;
    private UserProfileRepository userProfileRepository;

    public editPhoneNo() {
        super(R.layout.editingphoneno);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());
        userProfileRepository = new UserProfileRepository(requireContext());

        // UI elements
        backBtn = view.findViewById(R.id.backBtn);
        editPhoneNumber = view.findViewById(R.id.editPhoneNumber);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        editOTP = view.findViewById(R.id.editOTP);
        btnVerifyOTP = view.findViewById(R.id.btnVerifyOTP);

        // Hidden initially
        editOTP.setVisibility(View.GONE);
        btnVerifyOTP.setVisibility(View.GONE);

        apiService = RetrofitClient.getClient(requireContext()).create(EditProfile.class);

        // Prefill saved phone
        userProfileRepository.getUserProfile(profile -> {
            if (profile != null) {
                editPhoneNumber.setText(profile.phoneNo);
            }
        });

        // Back
        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        // Step 1: Send new phone to backend
        btnConfirm.setOnClickListener(v -> sendPhoneToBackend());

        // Step 2: Verify OTP
        btnVerifyOTP.setOnClickListener(v -> verifyOTP());
    }


    private void sendPhoneToBackend() {
        String newPhone = editPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(newPhone)) {
            editPhoneNumber.setError("Phone number required");
            return;
        }

        PhoneLoginModel model = new PhoneLoginModel(newPhone);

        apiService.editPhoneNo(
                tokenManager.getAccessToken(),
                model
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            "OTP sent to " + newPhone, Toast.LENGTH_SHORT).show();

                    // Reveal OTP field
                    editOTP.setVisibility(View.VISIBLE);
                    btnVerifyOTP.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(requireContext(),
                            "Failed to send OTP. Try again later.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private void verifyOTP() {
        String otp = editOTP.getText().toString().trim();
        String updatedPhone = editPhoneNumber.getText().toString();

        if (otp.isEmpty()) {
            editOTP.setError("Enter OTP");
            return;
        }

        OTPVerifyModel request = new OTPVerifyModel(updatedPhone, otp);

        apiService.verifyPhoneOTP(
                tokenManager.getAccessToken(),
                request
        ).enqueue(new Callback<VerifyOTPResponse>() {
            @Override
            public void onResponse(Call<VerifyOTPResponse> call,
                                   Response<VerifyOTPResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    if (response.body().isSuccess()) {

                        userProfileRepository.updateUserPhone(
                                response.body().getPhoneNumber(),
                                () -> {
                                    Toast.makeText(requireContext(),
                                            "Phone number updated successfully!",
                                            Toast.LENGTH_LONG).show();

                                    Navigation.findNavController(requireActivity(),
                                            R.id.nav_host_fragment).navigateUp();
                                }
                        );

                    } else {
                        Toast.makeText(requireContext(),
                                "Invalid OTP!",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Verification failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VerifyOTPResponse> call, Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
