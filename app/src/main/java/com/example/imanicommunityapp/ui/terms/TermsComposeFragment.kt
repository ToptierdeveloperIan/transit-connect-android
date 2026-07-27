package com.example.imanicommunityapp.ui.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.imanicommunityapp.R
import com.example.imanicommunityapp.auth.Repository.TokenManager

/**
 * Hosts the Jetpack Compose Terms of Service experience inside the existing
 * Navigation Component graph (Java/XML app shell).
 *
 * Nav argument [ARG_REQUIRE_ACCEPT]: gate mode after login/splash — no back until accepted.
 */
class TermsComposeFragment : Fragment() {

    private val viewModel: TermsViewModel by viewModels()
    private var requireAccept: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        requireAccept = arguments?.getBoolean(ARG_REQUIRE_ACCEPT, false) ?: false

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TermsRoute(
                    viewModel = viewModel,
                    requireAccept = requireAccept,
                    onBack = {
                        if (!requireAccept) {
                            findNavController().navigateUp()
                        }
                    },
                    onAccepted = {
                        if (requireAccept) {
                            navigateAfterAccept()
                        } else {
                            findNavController().navigateUp()
                        }
                    },
                )
            }
        }
    }

    private fun navigateAfterAccept() {
        val tokenManager = TokenManager(requireContext())
        val role = tokenManager.getUserRole()
        val nav = findNavController()
        // Clear terms from back stack so user cannot return without re-auth path
        val options = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.SplashScreen, true)
            .build()
        if ("driver".equals(role, ignoreCase = true)) {
            nav.navigate(R.id.driverhomeFragment, null, options)
        } else {
            nav.navigate(R.id.homeFragment, null, options)
        }
    }

    companion object {
        const val ARG_REQUIRE_ACCEPT = "requireAccept"
    }
}
