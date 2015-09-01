package udacity.mariosoberanis.spotifystreamer.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import udacity.mariosoberanis.spotifystreamer.R;

public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    /*
    * We'll use these to ensure the user enters a valid country code.
    */
    private static final Set<String> VALID_COUNTRY_CODES =
            new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    private String mValidCountryCode;
    private Boolean mExplicitValue;
    private Boolean mOnLockValue;

    private String mTrue;
    private String mFalse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        /*
        * Get the most recent values for the preferences.
        */
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mValidCountryCode = prefs.getString(SearchActivity.PREF_COUNTRY_CODE,
                getResources().getString(R.string.prefs_default_country_code));

        mExplicitValue = prefs.getBoolean(SearchActivity.PREF_ALLOW_EXPLICIT, true);
        mOnLockValue = prefs.getBoolean(SearchActivity.PREF_ALLOW_ON_LOCK, true);

        mTrue = getResources().getString(R.string.true_string);
        mFalse = getResources().getString(R.string.false_string);

        bindPreferenceSummaryToValue(findPreference(SearchActivity.PREF_COUNTRY_CODE));
        bindPreferenceSummaryToValue(findPreference(SearchActivity.PREF_ALLOW_EXPLICIT));
        bindPreferenceSummaryToValue(findPreference(SearchActivity.PREF_ALLOW_ON_LOCK));

    }

    private void bindPreferenceSummaryToValue(Preference preference) {

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        if (preference instanceof EditTextPreference) {
            onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .getString(preference.getKey(), ""));

        } else if (preference instanceof CheckBoxPreference) {
            onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .getBoolean(preference.getKey(), true));
        }

        // Determine which preference was modified.
        switch (preference.getKey())  {
            case SearchActivity.PREF_COUNTRY_CODE:
                preference.setSummary(mValidCountryCode);
                break;
            case SearchActivity.PREF_ALLOW_EXPLICIT:
                preference.setSummary((mExplicitValue) ? mTrue : mFalse);
                break;
            case SearchActivity.PREF_ALLOW_ON_LOCK:
                preference.setSummary((mOnLockValue) ? mTrue : mFalse);
                break;
            default:
                // Do Nothing
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        switch (preference.getKey()) {
            case SearchActivity.PREF_COUNTRY_CODE: {
                String newCountryCode = ((String) newValue).toUpperCase().trim();
                handleCountryCodePrefChange(preference, (String) newCountryCode);
                break;
            }
            case SearchActivity.PREF_ALLOW_EXPLICIT: {
                handleExplicitPrefChange(preference, (boolean) newValue);
                break;
            }
            case SearchActivity.PREF_ALLOW_ON_LOCK: {
                handleAllowOnLockPrefChange(preference, (boolean) newValue);
                break;
            }
            default: // Do Nothing
        }

        return true;
    }

    private void handleCountryCodePrefChange(Preference preference, String newCountryCode) {

        if (mValidCountryCode.equals(newCountryCode))  return;

        // Check for a valid country code.
        if (VALID_COUNTRY_CODES.contains(newCountryCode)) {
            mValidCountryCode = newCountryCode;

        } else {
            String msg = getString(R.string.invalid_country_code, newCountryCode);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            /*
            * Reset country to last know good value.
            */
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit()
                    .putString(SearchActivity.PREF_COUNTRY_CODE, mValidCountryCode)
                    .commit();
        }

        preference.setSummary(mValidCountryCode);

    }

    private void handleExplicitPrefChange(Preference preference, Boolean newValue)  {

        if (mExplicitValue.equals(newValue)) return;

        mExplicitValue = newValue;
        preference.setSummary((newValue) ? mTrue : mFalse);
    }

    private void handleAllowOnLockPrefChange(Preference preference, Boolean newValue)  {

        if (mOnLockValue.equals(newValue)) return;

        mOnLockValue = newValue;
        preference.setSummary((newValue) ? mTrue : mFalse);
    }
}
