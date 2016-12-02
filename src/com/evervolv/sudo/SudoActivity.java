/*
 * Copyright (C) 2016 The Evervolv Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evervolv.sudo;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settingslib.drawer.SettingsDrawerActivity;

import com.evervolv.sudo.R;
import com.evervolv.sudo.SudoPrefsFragment;

public class SudoActivity extends SettingsDrawerActivity implements
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        PreferenceFragment.OnPreferenceStartScreenCallback {

    private static final String TAG_SUDO = "sudo";

    private String mInitialTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getFragmentManager().findFragmentByTag(TAG_SUDO) == null) {
            final Fragment fragment = new SudoPrefsFragment();

            getFragmentManager().beginTransaction().replace(R.id.content_frame,
                    fragment, TAG_SUDO).commit();

            mInitialTitle = String.valueOf(getActionBar().getTitle());

            String extra = getIntent().getStringExtra(TAG_SUDO);
            if (extra != null) {
                startPreferenceScreen((PreferenceFragment)fragment, extra, false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        } else {
            getActionBar().setTitle(mInitialTitle);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        try {
            Class<?> cls = Class.forName(pref.getFragment());
            Fragment fragment = (Fragment) cls.newInstance();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            getActionBar().setTitle(pref.getTitle());
            transaction.replace(R.id.content_frame, fragment);
            transaction.addToBackStack("PreferenceFragment");
            transaction.commit();
            return true;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Log.d("SudoActivity", "Problem launching fragment", e);
            return false;
        }
    }

    private boolean startPreferenceScreen(PreferenceFragment caller, String key, boolean backStack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SubSettingsFragment fragment = new SubSettingsFragment();
        final Bundle b = new Bundle(1);
        b.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, key);
        fragment.setArguments(b);
        fragment.setTargetFragment(caller, 0);
        transaction.replace(R.id.content_frame, fragment);
        if (backStack) {
            transaction.addToBackStack("PreferenceFragment");
        }
        transaction.commit();

        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        return startPreferenceScreen(caller, pref.getKey(), true);
    }

    public static class SubSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceScreen p = (PreferenceScreen) ((PreferenceFragment) getTargetFragment())
                    .getPreferenceScreen().findPreference(rootKey);
            setPreferenceScreen(p);
            getActivity().getActionBar().setTitle(p.getTitle());
        }
    }
}
