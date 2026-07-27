package com.example.imanicommunityapp.Network;

/**
 * Coarse link usability.
 *
 * <ul>
 *   <li>{@link #ONLINE} — {@code NET_CAPABILITY_INTERNET} + {@code VALIDATED}</li>
 *   <li>{@link #LIMITED} — network present but not validated</li>
 *   <li>{@link #OFFLINE} — no usable default network</li>
 * </ul>
 */
public enum InternetState {
    ONLINE,
    LIMITED,
    OFFLINE,
}
