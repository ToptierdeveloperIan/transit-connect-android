package com.example.imanicommunityapp.Sync.Rehydration;

public enum RehydrationScopeTags {
    BOOKINGS_CACHE,      // cached booking data (lists, details)
    ALL,                // full app-level rehydrate (use sparingly)
    BOOKINGS,           // booking lists, booking metadata, booking-related caches
    PROFILE,            // user profile, roles, permissions
    SETTINGS,           // app settings, preferences, feature flags
    NOTIFICATIONS,      // subscriptions and push settings
    ASSETS,             // cached binaries/images needed for UX
    PENDING_MUTATIONS,  // local mutations that need syncing (queue)
    CUSTOM              // custom domain; use with `affectedIds` to target
}
