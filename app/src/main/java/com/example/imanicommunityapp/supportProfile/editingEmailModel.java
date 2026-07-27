package com.example.imanicommunityapp.supportProfile;

import com.google.gson.annotations.SerializedName;

public class editingEmailModel {
    @SerializedName("email")
    private final String Email;


    //model constructor
    public editingEmailModel(String Email){
        this.Email= Email;

    }

    //getter for firstname
    public String getEmail(){
        return this.Email;
    }

}
