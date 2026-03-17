package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private val viewModel: PlayerViewModel by activityViewModels()

    private lateinit var stepWelcome: LinearLayout
    private lateinit var stepDiscovery: LinearLayout
    private lateinit var stepComplete: LinearLayout

    private var pairingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stepWelcome = view.findViewById(R.id.stepWelcome)
        stepDiscovery = view.findViewById(R.id.stepDiscovery)
        stepComplete = view.findViewById(R.id.stepComplete)

        // Welcome step
        view.findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            showStep(stepDiscovery)
            viewModel.startDiscovery()
        }

        // Discovery step
        val deviceList: RecyclerView = view.findViewById(R.id.onboardingDeviceList)
        deviceList.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DeviceAdapter(
            onAction = { device ->
                if (device.isPaired) {
                    viewModel.connectDevice(device)
                } else {
                    viewModel.startPairing(device)
                }
            },
            onLongPress = { }
        )
        deviceList.adapter = adapter

        view.findViewById<TextView>(R.id.btnManualEntry).setOnClickListener {
            showManualEntryDialog()
        }

        view.findViewById<TextView>(R.id.btnSkipOnboarding).setOnClickListener {
            viewModel.stopDiscovery()
            navigateToPlayerHome()
        }

        // Completion step
        view.findViewById<MaterialButton>(R.id.btnStartListening).setOnClickListener {
            navigateToPlayerHome()
        }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.setItems(state.devices)

                // If connected during onboarding, show completion
                if (state.connectionState == TvConnectionManager.ConnectionState.CONNECTED
                    && stepDiscovery.visibility == View.VISIBLE) {
                    viewModel.stopDiscovery()
                    view.findViewById<TextView>(R.id.completionTitle).text =
                        "Connected to ${state.deviceName ?: "your TV"}"
                    showStep(stepComplete)
                }
            }
        }

        // Observe pairing state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pairingManager.pairingState.collect { state ->
                handlePairingState(state)
            }
        }
    }

    private fun showStep(step: View) {
        stepWelcome.visibility = View.GONE
        stepDiscovery.visibility = View.GONE
        stepComplete.visibility = View.GONE
        step.visibility = View.VISIBLE
    }

    private fun handlePairingState(state: PairingState) {
        when (state) {
            is PairingState.AwaitingPin -> showPinDialog()
            is PairingState.Failed -> {
                pairingDialog?.dismiss()
                AlertDialog.Builder(requireContext())
                    .setTitle("Pairing Failed")
                    .setMessage(state.reason)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is PairingState.Paired -> {
                pairingDialog?.dismiss()
            }
            else -> {}
        }
    }

    private fun showPinDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pair, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)

        pairingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter PIN")
            .setMessage("Enter the 4-digit PIN shown on your TV")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.confirmPin(pinInput.text.toString())
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.cancelPairing()
            }
            .show()
    }

    private fun showManualEntryDialog() {
        val input = EditText(requireContext())
        input.hint = "192.168.1.100"

        AlertDialog.Builder(requireContext())
            .setTitle("Enter IP Address")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotBlank()) {
                    MantleApp.instance.connectionManager.connect(ip, 8899, "")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToPlayerHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PlayerHomeFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pairingDialog?.dismiss()
    }
}
