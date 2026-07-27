package com.example.imanicommunityapp;



import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavHostController;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupFragment extends Fragment {

    public SignupFragment() {
        super(R.layout.signup); // This links to fragment_home.xml
    }
    //instance of the fragment
    SignupFragment fragment = this;


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Link UI Elements
        EditText firstNametxt= view.findViewById(R.id.nameedittext);
        EditText secondNametxt= view.findViewById(R.id.secondnameedittext);
        EditText emailtxt= view.findViewById(R.id.emailedittext);
        EditText Phonenumbertxt= view.findViewById(R.id.phoneedittext);

        // Plain hub client (register is unauthenticated)
        APIService sendsignupdetailstoapi =
                authRetrofitClient.getPlainClient().create(APIService.class);

        //signup form logic
        Button SignupButton = view.findViewById(R.id.SignupButton);
        SignupButton.setOnClickListener(v -> {
            // get and validate data then send to API
            String firstName= firstNametxt.getText().toString();
            if(firstName.isEmpty()){
                firstNametxt.setError("First Name is required");
            }
            String secondname= secondNametxt.getText().toString();
            if(secondname.isEmpty()){
                secondNametxt.setError("Second Name is required");
            }
            String email= emailtxt.getText().toString();
            if(email.isEmpty()){
                emailtxt.setError("Email required");
            }
            String phonenumber= Phonenumbertxt.getText().toString();
            if(phonenumber.isEmpty()){
                Phonenumbertxt.setError("phone number required");
            }

            //register the data
            SignupData data = new SignupData(phonenumber);
            // send the data to the api
            sendsignupdetailstoapi.signup(data).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    //handle success or error
                    if (response.isSuccessful()){
                    Toast.makeText(requireContext(), "Data added to API", Toast.LENGTH_SHORT).show();
                    //navigate to verification fragment

                    }
                    else{
                        String errorMessage = "Something went wrong";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage = response.errorBody().string();

// Optional: Extract message from JSON if your backend returns structured error messages
// JSONObject jsonObject = new JSONObject(errorMessage);
// errorMessage = jsonObject.optString("message", errorMessage);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }


                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // handles failure
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    t.printStackTrace();
                }
            });



        });
    }
}
