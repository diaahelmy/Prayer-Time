package com.example.time.fragment


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.navigation.Navigation
import com.example.time.Manager.LanguageManager
import com.example.time.R
import com.example.time.alarm.AlarmReceivers
import com.example.time.calculate.getCalculationMethod
import com.example.time.calculate.getCalculationMethodAsr
import com.example.time.calculate.saveCalculationMethod
import com.example.time.calculate.saveCalculationMethodAsr
import com.example.time.databinding.FragmentSettingBinding


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var languageSpinner: Spinner
    private val languages = listOf(
        "English",
        "العربية",
        "Español",
        "Français",
        "Deutsch",
        "Italiano",
        "Português",
        "Русский",
        "中文"
    )
    private val languageCodes = listOf(
        "en", "ar", "es", "fr",
        "de", "it", "pt", "ru", "zh"
    )
    private lateinit var languageDialog: AlertDialog

    private lateinit var languageManager: LanguageManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val displayTextView = binding.calculationFajrandIsha
        saveTextCalculationMethodFajrandIsha()
        saveTextSoundFajr()
        saveTextSoundAll()
        languageManager = LanguageManager(requireContext())
        saveTextCalculationMethodAsr()        // Apply saved language settings
        languageManager.applySelectedLanguage()

        languageManager = LanguageManager(requireContext())
        saveTextCalculationMethodAsr()        // Apply saved language settings
        languageManager.applySelectedLanguage()
        binding.soundAzanFajrText.text = saveTextSoundFajr()
        binding.tvsoundAzanall.text = saveTextSoundAll()

        binding.soundAzanFajr.setOnClickListener {
            val actionChoices = arrayOf(
                getString(R.string.sound_abdel_basset),
                getString(R.string.sound_nasser_al_qatami),
                getString(R.string.sound_mishary_rashid),
                getString(R.string.sound_makkah),
                getString(R.string.sound_ali_bin_ahmed_mulla)
            )
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Select Azan Fajr")
                .setItems(actionChoices) { dialog, which ->
                    when (which) {
                        0 -> {
                            // Update action constants in AlarmReceivers for FAJR_ALARM
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/alarm")
                            alarmReceiver.updateSoundUriForFajr(requireContext(), newUriForFajr)
                            binding.soundAzanFajrText.text = getString(R.string.sound_abdel_basset)

                            dialog.dismiss()

                        }

                        1 -> {
                            // Update action constants in AlarmReceivers for All_ALARM
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/nasserfajr")
                            alarmReceiver.updateSoundUriForFajr(requireContext(), newUriForFajr)
                            binding.soundAzanFajrText.text =
                                getString(R.string.sound_nasser_al_qatami)
                            dialog.dismiss()

                        }

                        2 -> {
                            // Cancel the dialog
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/misharyrashidfajr")
                            alarmReceiver.updateSoundUriForFajr(requireContext(), newUriForFajr)
                            binding.soundAzanFajrText.text =
                                getString(R.string.sound_mishary_rashid)
                            dialog.dismiss()
                        }

                        3 -> {
                            // Exit the app
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/makkahfakr")

                            alarmReceiver.updateSoundUriForFajr(requireContext(), newUriForFajr)
                            binding.soundAzanFajrText.text = getString(R.string.sound_makkah)
                            dialog.dismiss()

                        }

                        4 -> {
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/alibinahmedmullafajr")
                            alarmReceiver.updateSoundUriForFajr(requireContext(), newUriForFajr)
                            binding.soundAzanFajrText.text =
                                getString(R.string.sound_ali_bin_ahmed_mulla)
                            dialog.dismiss()
                        }
                    }
                    saveSoundtextFajr(requireContext(), actionChoices[which])

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.soundAzanAll.setOnClickListener {
            val actionChoices = arrayOf(
                getString(R.string.sound_abdel_basset),
                getString(R.string.sound_nasser_al_qatami),
                getString(R.string.sound_mishary_rashid),
                getString(R.string.sound_makkah),
                getString(R.string.sound_ali_bin_ahmed_mulla)
            )

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Select Azan")
                .setItems(actionChoices) { dialog, which ->
                    when (which) {
                        0 -> {
                            // Update action constants in AlarmReceivers for FAJR_ALARM
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/abdelbasset")
                            alarmReceiver.updateSoundUriForAll(requireContext(), newUriForFajr)
                            binding.tvsoundAzanall.text = getString(R.string.sound_abdel_basset)
                            dialog.dismiss()

                        }

                        1 -> {
                            // Update action constants in AlarmReceivers for All_ALARM
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/nasserfajr")
                            alarmReceiver.updateSoundUriForAll(requireContext(), newUriForFajr)
                            binding.tvsoundAzanall.text = getString(R.string.sound_nasser_al_qatami)
                            dialog.dismiss()

                        }

                        2 -> {
                            // Cancel the dialog
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/misharyrashid")
                            alarmReceiver.updateSoundUriForAll(requireContext(), newUriForFajr)
                            binding.tvsoundAzanall.text = getString(R.string.sound_mishary_rashid)
                            dialog.dismiss()
                        }

                        3 -> {
                            // Exit the app
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/Makkah")
                            alarmReceiver.updateSoundUriForAll(requireContext(), newUriForFajr)
                            binding.tvsoundAzanall.text = getString(R.string.sound_makkah)
                            dialog.dismiss()

                        }

                        4 -> {
                            val alarmReceiver = AlarmReceivers()
                            val newUriForFajr =
                                Uri.parse("android.resource://com.example.time/raw/alibinahmedmulla")
                            alarmReceiver.updateSoundUriForAll(requireContext(), newUriForFajr)
                            binding.tvsoundAzanall.text =
                                getString(R.string.sound_ali_bin_ahmed_mulla)
                            dialog.dismiss()
                        }
                    }
                    saveSoundtextAll(requireContext(), actionChoices[which])

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

        }


        // Set click listener to show dialog
        binding.display.text = saveTextCalculationMethodFajrandIsha()
        binding.hnafi.text = saveTextCalculationMethodAsr()        // Apply saved language settings


        displayTextView.setOnClickListener {
            showCalculationMethodDialog()
        }
        binding.asrCalculation.setOnClickListener {
            showCalculationASR()
        }

        binding.back.setOnClickListener {
            Navigation.findNavController(view).navigateUp()
        }

        // Initialize the Spinner
        languageSpinner = binding.languageSpinner

        // Set up the spinner with a default item "Select Language"
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf(getString(R.string.select_language))
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (position != 0) { // Ensure it's not the default "Select Language" item
                    val selectedLanguageCode = languageCodes[position - 1] // Adjust position
                    languageManager.saveSelectedLanguage(selectedLanguageCode)
                    languageManager.setLocale(selectedLanguageCode)
                    languageManager.applyLayoutDirection(selectedLanguageCode)

                    // Optionally, recreate the activity to apply language changes
                    requireActivity().recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        // Show dialog on spinner click
        languageSpinner.setOnTouchListener { _, _ ->
            showLanguageDialog()
            true // consume the touch event
        }
    }

    // Function to show a dialog for sound selection


    private fun showLanguageDialog() {
        // Dismiss any existing dialog
        if (::languageDialog.isInitialized && languageDialog.isShowing) {
            languageDialog.dismiss()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_language_selection, null)
        val listView = dialogView.findViewById<ListView>(R.id.language_list_view)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, languages)
        listView.adapter = adapter

        languageDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_language)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < languageCodes.size) {
                val selectedLanguageCode = languageCodes[position]
                languageManager.saveSelectedLanguage(selectedLanguageCode)
                languageManager.setLocale(selectedLanguageCode)
                languageManager.applyLayoutDirection(selectedLanguageCode)

                // Update the spinner's adapter to reflect the new language
                val newAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    listOf(getString(R.string.select_language)) + languages
                )
                newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                languageSpinner.adapter = newAdapter

                requireActivity().recreate()
                languageDialog.dismiss()
            }
        }

        languageDialog.show()
    }


    private val calculationMethodDisplayNames = mapOf(
        "Muslim World League" to "Muslim World League",
        "Islamic Society of North America (ISNA)" to " North America (ISNA)",
        "Egyptian General Authority of Survey" to "Egyptian",
        "Umm al-Qura University, Makkah" to "Makkah",
        "University of Islamic Sciences, Karachi" to "Karachi"
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCalculationMethodDialog() {
        val calculationMethods = arrayOf(
            getString(R.string.calculation_method_mwl),
            getString(R.string.calculation_method_isna),
            getString(R.string.calculation_method_egas),
            getString(R.string.calculation_method_umm_al_qura),
            getString(R.string.calculation_method_karachi)
        )

        // Retrieve the saved calculation method
        val savedMethod = getCalculationMethod(requireContext())

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Choose Calculation Method")

        // Highlight the saved method
        val checkedItem = calculationMethods.indexOf(savedMethod)

        dialogBuilder.setSingleChoiceItems(calculationMethods, checkedItem) { dialog, which ->
            val selectedMethod = calculationMethods[which]


            // Save the selected method
            saveCalculationMethod(requireContext(), selectedMethod)
            saveCalculationtext(requireContext(), selectedMethod)
            val displayText = calculationMethodDisplayNames[selectedMethod] ?: selectedMethod
            binding.display.text = displayText


            // Log or use the result as needed
            Log.d("PrayerTime", "Isha Time calculated using $selectedMethod:")

            dialog.dismiss()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun showCalculationASR() {
        val calculationMethods = arrayOf(
            getString(R.string.majority),
            getString(R.string.hanafi),
        )

        // Retrieve the saved calculation method
        val savedMethod = getCalculationMethodAsr(requireContext())

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Choose Calculation Method")

        // Highlight the saved method
        val checkedItem = calculationMethods.indexOf(savedMethod)

        dialogBuilder.setSingleChoiceItems(calculationMethods, checkedItem) { dialog, which ->
            val selectedMethod = calculationMethods[which]


            // Save the selected method
            saveCalculationMethodAsr(requireContext(), selectedMethod)
            saveCalculationtextAsr(requireContext(), selectedMethod)
            val displayText = calculationMethodDisplayNames[selectedMethod] ?: selectedMethod
            binding.hnafi.text = displayText


            // Log or use the result as needed
            Log.d("PrayerTime", "Isha Time calculated using $selectedMethod:")

            dialog.dismiss()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    fun saveTextCalculationMethodFajrandIsha(): String? {
        val sharedPreferences =
            requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedMethod =
            sharedPreferences.getString("calculationMethod", "Egyptian") // Default value

        val displayTextResId = when (savedMethod) {
            getString(R.string.calculation_method_mwl) ->  getString(R.string.calculation_method_mwl)
            getString(R.string.calculation_method_isna) ->   getString(R.string.calculation_method_isna)
            getString(R.string.calculation_method_egas)->  getString(R.string.calculation_method_egas)
            getString(R.string.calculation_method_umm_al_qura) ->   getString(R.string.calculation_method_umm_al_qura)
            getString(R.string.calculation_method_karachi)->  getString(R.string.calculation_method_karachi)
            else ->  getString(R.string.calculation_method_egas)// Default to majority if not set
        }
        // Get the shortened display name
        val displayText = displayTextResId
        binding.display.text = displayText
        return displayText
    }

    private fun saveCalculationtext(context: Context, method: String) {
        val sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("calculationMethod", method)
            apply()
        }
    }

    fun saveTextSoundFajr(): String? {
        val sharedPreferences =
            requireContext().getSharedPreferences("Sounds", Context.MODE_PRIVATE)
        val savedMethod =
            sharedPreferences.getString("SoundsFajr", getString(R.string.sound_abdel_basset)) // Default value
        val displayTextResId = when (savedMethod) {
            getString(R.string.sound_abdel_basset) -> getString(R.string.sound_abdel_basset)
            getString(R.string.sound_nasser_al_qatami) -> getString(R.string.sound_nasser_al_qatami)
            getString(R.string.sound_mishary_rashid) -> getString(R.string.sound_mishary_rashid)
            getString(R.string.sound_makkah) -> getString(R.string.sound_makkah)
            getString(R.string.sound_ali_bin_ahmed_mulla) -> getString(R.string.sound_ali_bin_ahmed_mulla)
            else -> getString(R.string.sound_abdel_basset) // Default to majority if not set
        }
        // Get the shortened display name
        val displayText = displayTextResId
        binding.soundAzanFajrText.text = displayText
        return displayText
    }

    private fun saveSoundtextFajr(context: Context, method: String) {
        val sharedPreferences = context.getSharedPreferences("Sounds", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("SoundsFajr", method)
            apply()
        }

    }

    fun saveTextSoundAll(): String {
        val sharedPreferences =
            requireContext().getSharedPreferences("Sound", Context.MODE_PRIVATE)
        val savedMethod =
            sharedPreferences.getString("SoundsAll", "Abdel Basset Abdel Samad") // Default value
        val displayTextResId = when (savedMethod) {
            getString(R.string.sound_abdel_basset) -> getString(R.string.sound_abdel_basset)
            getString(R.string.sound_nasser_al_qatami) -> getString(R.string.sound_nasser_al_qatami)
            getString(R.string.sound_mishary_rashid) -> getString(R.string.sound_mishary_rashid)
            getString(R.string.sound_makkah) -> getString(R.string.sound_makkah)
            getString(R.string.sound_ali_bin_ahmed_mulla) -> getString(R.string.sound_ali_bin_ahmed_mulla)
            else -> getString(R.string.sound_abdel_basset) // Default to majority if not set
        }
        // Get the shortened display name
        val displayText = displayTextResId
        binding.tvsoundAzanall.text = displayText
        return displayText
    }

    private fun saveSoundtextAll(context: Context, method: String) {
        val sharedPreferences = context.getSharedPreferences("Sound", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("SoundsAll", method)
            apply()
        }
        // Update the display text for `hnafi` TextView based on the method


    }

    fun saveTextCalculationMethodAsr(): String? {
        val sharedPreferences =
            requireContext().getSharedPreferences("Setting", Context.MODE_PRIVATE)
        val savedMethod =
            sharedPreferences.getString("calculationMethodAsr", "Majority") // Default value
        val displayTextResId = when (savedMethod) {
            getString(R.string.majority) -> R.string.majority
            getString(R.string.hanafi) -> R.string.hanafi
            else -> R.string.majority // Default to majority if not set
        }

        // Get the display text from the resource ID
        val displayText = getString(displayTextResId)
        binding.hnafi.text = displayText
        return displayText

    }

    private fun saveCalculationtextAsr(context: Context, method: String) {
        val sharedPreferences = context.getSharedPreferences("Setting", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("calculationMethodAsr", method)
            apply()
        }
 
    }
}
