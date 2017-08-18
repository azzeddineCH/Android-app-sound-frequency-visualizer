package com.votek.android.audiovisualizer;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by azeddine on 17/08/17.
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}