package com.example.time.fragment


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import com.example.time.LanguageManager
import com.example.time.R
import com.example.time.calculate.getCalculationMethod
import com.example.time.calculate.saveCalculationMethod
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
        val displayTextView = binding.display
        saveTextCalculationMethod()
        languageManager = LanguageManager(requireContext())

        // Apply saved language settings
        languageManager.applySelectedLanguage()

        // Set click listener to show dialog
        displayTextView.setOnClickListener {
            showCalculationMethodDialog()
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
                id: Long
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
    fun saveTextCalculationMethod() {
        val sharedPreferences = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedMethod = sharedPreferences.getString("calculationMethod", "Egyptian") // Default value

        // Get the shortened display name
        val displayText = calculationMethodDisplayNames[savedMethod] ?: savedMethod
        binding.display.text = displayText
    }
    private fun saveCalculationtext(context: Context, method: String) {
        val sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("calculationMethod", method)
            apply()
        }
    }

}
