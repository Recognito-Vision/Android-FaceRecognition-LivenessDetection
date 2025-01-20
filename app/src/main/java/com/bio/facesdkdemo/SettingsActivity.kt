package com.bio.facesdkdemo

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*




class SettingsActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_LIVENESS_THRESHOLD = "0.7"
        const val DEFAULT_MATCHING_THRESHOLD = "0.8"
        const val DEFAULT_LIVENESS_MODEL = "0"
        const val YAW_THRESHOLD = "10.0"
        const val ROLL_THRESHOLD = "10.0"
        const val PITCH_THRESHOLD = "10.0"
        const val OCCLUSION_THRESHOLD = "0.5"
        const val EYECLOSE_THRESHOLD = "0.5"
        const val MOUTHOPEN_THRESHOLD = "0.5"

        @JvmStatic
        fun getLivenessThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("liveness_threshold", SettingsActivity.DEFAULT_LIVENESS_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getMatchThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("match_threshold", SettingsActivity.DEFAULT_MATCHING_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getLivenessModelType(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val livenessModel = sharedPreferences.getString("liveness_model", SettingsActivity.DEFAULT_LIVENESS_MODEL)
            if(livenessModel == "0") {
                return 0
            } else {
                return 1
            }
        }

        @JvmStatic
        fun getYawThreshold(context: Context): Float {
            return SettingsActivity.YAW_THRESHOLD.toFloat()
        }

        @JvmStatic
        fun getRollThreshold(context: Context): Float {
            return SettingsActivity.ROLL_THRESHOLD.toFloat()
        }

        @JvmStatic
        fun getPitchThreshold(context: Context): Float {
            return SettingsActivity.PITCH_THRESHOLD.toFloat()
        }

        @JvmStatic
        fun getOcclusionThreshold(context: Context): Float {
            return SettingsActivity.OCCLUSION_THRESHOLD.toFloat()
        }

        @JvmStatic
        fun getEyecloseThreshold(context: Context): Float {
            return SettingsActivity.EYECLOSE_THRESHOLD.toFloat()
        }

        @JvmStatic
        fun getMouthopenThreshold(context: Context): Float {
            return SettingsActivity.MOUTHOPEN_THRESHOLD.toFloat()
        }
    }

    lateinit var dbManager: DBManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbManager = DBManager(this)

        findViewById<Button>(R.id.btnSite).setOnClickListener {
            val url = "https://recognito.vision"

            // Create an intent to open a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

            // Verify that the intent resolves to an activity
            if (intent.resolveActivity(packageManager) != null) {
                // Start the activity
                startActivity(intent)
            } else {

            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val livenessThresholdPref = findPreference<EditTextPreference>("liveness_threshold")
            val livenessModelPref = findPreference<ListPreference>("liveness_model")
            val matchThresholdPref = findPreference<EditTextPreference>("match_threshold")
            val buttonRestorePref = findPreference<Preference>("restore_default_settings")

            livenessThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.set_failure), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.set_failure), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            matchThresholdPref?.setOnPreferenceChangeListener{ preference, newValue ->
                val stringPref = newValue as String
                try {
                    if(stringPref.toFloat() < 0.0f || stringPref.toFloat() > 1.0f) {
                        Toast.makeText(context, getString(R.string.set_failure), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        true
                    }
                } catch (e:Exception) {
                    Toast.makeText(context, getString(R.string.set_failure), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            buttonRestorePref?.setOnPreferenceClickListener {
                val builder = AlertDialog.Builder(context ?: requireContext())
                builder.setTitle("Confirm")
                builder.setMessage("Are you sure you want to reset all settings?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    livenessModelPref?.value = SettingsActivity.DEFAULT_LIVENESS_MODEL
                    livenessThresholdPref?.text = SettingsActivity.DEFAULT_LIVENESS_THRESHOLD
                    matchThresholdPref?.text = SettingsActivity.DEFAULT_MATCHING_THRESHOLD

                    Toast.makeText(activity, getString(R.string.reseted_default_settings), Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("No") { dialog, which ->
                    // Dismiss the dialog if "No" is clicked
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
                true
            }

            val buttonClearPref = findPreference<Preference>("clear_all_person")
            buttonClearPref?.setOnPreferenceClickListener {
                val builder = AlertDialog.Builder(context ?: requireContext())
                builder.setTitle("Confirm")
                builder.setMessage("Are you sure you want to remove all users?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    val settingsActivity = activity as SettingsActivity
                    settingsActivity.dbManager.clearDB()

                    Toast.makeText(activity, getString(R.string.removed_all_users), Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("No") { dialog, which ->
                    // Dismiss the dialog if "No" is clicked
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
                true
            }
        }
    }
}