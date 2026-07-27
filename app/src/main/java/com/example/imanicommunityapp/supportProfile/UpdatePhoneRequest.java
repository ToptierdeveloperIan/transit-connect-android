package com.example.imanicommunityapp.supportProfile;



public class UpdatePhoneRequest {
    private final String phoneNumber;

    public UpdatePhoneRequest ( String PhoneNumber){
        this.phoneNumber = PhoneNumber;

    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

}
