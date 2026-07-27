/**
 * Network connectivity and <b>context-aware offline UI</b> policy.
 *
 * <h2>What lives here</h2>
 * <ul>
 *   <li>{@link com.example.imanicommunityapp.Network.ConnectivityChecker} — OS link sensor</li>
 *   <li>{@link com.example.imanicommunityapp.Network.NetworkMonitor} — process-wide status hub</li>
 *   <li>{@link com.example.imanicommunityapp.Network.OfflineScope} — where the user is</li>
 *   <li>{@link com.example.imanicommunityapp.Network.OfflinePolicy} — scope × network → UI rules</li>
 *   <li>{@link com.example.imanicommunityapp.Network.OfflineScopeTracker} — stack of scopes</li>
 *   <li>{@link com.example.imanicommunityapp.Network.ContextualOfflineController} — combine both for UI</li>
 * </ul>
 *
 * <h2>Design goal</h2>
 * Detect offline/online immediately, then let each screen react differently
 * (soft banner vs hard block) based on {@link com.example.imanicommunityapp.Network.OfflineScope}.
 *
 * <h2>Boundary</h2>
 * This package must not depend on feature modules (Settings, Wallet, booking, …).
 * Features call into Network; Network never calls out.
 *
 * @see com.example.imanicommunityapp.Network.ContextualOfflineController
 */
package com.example.imanicommunityapp.Network;
