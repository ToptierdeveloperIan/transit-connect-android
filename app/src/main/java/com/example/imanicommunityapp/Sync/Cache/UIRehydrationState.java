package com.example.imanicommunityapp.Sync.Cache;

public class UIRehydrationState {
    private boolean hydtarionneeded;

    public UIRehydrationState() {
        this.hydtarionneeded = true; // default to needing hydration
    }

    public boolean gethydrationstate() {
        return hydtarionneeded;
    }

}
