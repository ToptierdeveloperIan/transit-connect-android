package com.example.imanicommunityapp.supportProfile;

import com.google.gson.annotations.SerializedName;

public class editingNameModel {
    @SerializedName("firstName")
    private final String firstName;
    @SerializedName("secondName")
    private final String secondName;

    //model constructor
    public editingNameModel(String firstName,String secondName){
        this.firstName= firstName;
        this.secondName= secondName;
    }

    //getter for firstname
    public String getFirstName(){
        return this.firstName;
    }
    //getter for secondname
    public String getSecondName(){
        return this.secondName;
    }


}
