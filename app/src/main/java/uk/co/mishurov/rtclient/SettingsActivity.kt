package uk.co.mishurov.rtclient

import android.os.Bundle
import android.content.Intent
import android.view.MenuItem
import android.preference.PreferenceFragment
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.content.SharedPreferences
import android.preference.EditTextPreference

import android.util.Log


class SettingsActivity : PreferenceActivity()
{
    class SettingsFragment : PreferenceFragment(),
                             SharedPreferences.OnSharedPreferenceChangeListener
    {
        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
            val prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext()
            )
            prefs?.registerOnSharedPreferenceChangeListener(this)

            val pref = findPreference(TEXT_PREF) as EditTextPreference
            pref.setSummary(pref.getText())
        }

        override fun onSharedPreferenceChanged(s: SharedPreferences, k: String)
        {
            val pref = findPreference(k)
            if (pref is EditTextPreference) {
                pref.setSummary(pref.getText())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val actionBar = getActionBar()
        actionBar.setDisplayHomeAsUpEnabled(true)

        getFragmentManager().beginTransaction().replace(
            android.R.id.content, SettingsFragment()
        ).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        val mainIntent = Intent(
            getApplicationContext(), MainActivity::class.java
        )
        startActivityForResult(mainIntent, 0)
        return true
    }

    companion object
    {
        private val TAG = "RtCLient SettingsActivity"
        private val TEXT_PREF = "host_preference"
    }
}

