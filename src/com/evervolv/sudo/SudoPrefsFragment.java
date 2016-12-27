package com.evervolv.sudo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.evervolv.sudo.R;
import com.evervolv.sudo.fragment.AppPolicyFragment;
import com.evervolv.sudo.fragment.AppLogsFragment;
import com.evervolv.sudo.util.Constants;

public class SudoPrefsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "SudoPrefsFragment";

    private static final String PREF_ROOT_ACCESS = "root_access";
    private static final String PREF_ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private static final String PREF_AUTO_RESPONSE = "auto_response";
    private static final String PREF_MULTIUSER_POLICY = "multiuser_policy";

    private static final String PREF_LOGGING = "logging";
    private static final String PREF_NOTIFICATIONS = "notifications";
    private static final String PREF_REQUEST_TIMEOUT = "request_timeout";

    private PreferenceScreen mPrefSet;
    private Context mContext;

    private DropDownPreference mRootAccess;
    private DropDownPreference mAutoResponse;
    private DropDownPreference mMultiuserPolicy;

    private SwitchPreference mLogging;
    private DropDownPreference mNotifications;
    private DropDownPreference mRequestTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.sudo_prefs);

        mContext = getActivity();
        mPrefSet = getPreferenceScreen();

        /* Access */
        mRootAccess = (DropDownPreference) mPrefSet.findPreference(PREF_ROOT_ACCESS);
        mRootAccess.setOnPreferenceChangeListener(this);

        mAutoResponse = (DropDownPreference) mPrefSet.findPreference(
                PREF_AUTO_RESPONSE);
        mAutoResponse.setOnPreferenceChangeListener(this);
        mAutoResponse.setValueIndex(Constants.getAutomaticResponse(mContext));
        setAutoResponseSummary(Constants.getAutomaticResponse(mContext));

        mMultiuserPolicy = (DropDownPreference) mPrefSet.findPreference(PREF_MULTIUSER_POLICY);
        mMultiuserPolicy.setOnPreferenceChangeListener(this);

        if (Constants.getMultiuserMode(mContext) != Constants.MULTIUSER_MODE_NONE) {
            int mode = Constants.getMultiuserMode(mContext);
            setMultiuserModeSummary(mode);
            mMultiuserPolicy.setValueIndex(mode);
        } else {
            mPrefSet.removePreference(mMultiuserPolicy);
        }

        mLogging = (SwitchPreference) mPrefSet.findPreference(PREF_LOGGING);
        mLogging.setChecked(Constants.getLogging(mContext));

        mNotifications = (DropDownPreference) mPrefSet.findPreference(PREF_NOTIFICATIONS);
        mNotifications.setValueIndex(Constants.getNotificationType(mContext));
        mNotifications.setOnPreferenceChangeListener(this);
        setNotificationTypeSummary(Constants.getNotificationType(mContext));

        mRequestTimeout = (DropDownPreference) mPrefSet.findPreference(PREF_REQUEST_TIMEOUT);
        mRequestTimeout.setOnPreferenceChangeListener(this);
        mRequestTimeout.setSummary(getString(
                R.string.pref_request_timeout_summary,
                Constants.getRequestTimeout(mContext)));
        mRequestTimeout.setValueIndex(getRequestTimeoutIndex(
                Constants.getRequestTimeout(mContext)));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        readAccessOptions();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sudo_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_app_policies:
                getActivity().getFragmentManager().beginTransaction().replace(R.id.content_frame,
                        new AppPolicyFragment(), TAG).commit();
                return true;
            case R.id.menu_logs:
                getActivity().getFragmentManager().beginTransaction().replace(R.id.content_frame,
                        new AppLogsFragment(), TAG).commit();
                return true;
           default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int intValue = Integer.valueOf((String) newValue);

        if (preference == mRootAccess) {
            writeAccessOptions(intValue);
            return true;
        }
        if (preference == mAutoResponse) {
            setAutoResponseSummary(intValue);
            Constants.setAutomaticResponse(mContext, intValue);
            return true;
        }
        if (preference == mMultiuserPolicy) {
            setMultiuserModeSummary(intValue);
            Constants.setMultiuserMode(mContext, intValue);
            return true;
        }
        if (preference == mLogging) {
            Constants.setLogging(mContext, mLogging.isChecked());
            return true;
        }
        if (preference == mNotifications) {
            Constants.setNotificationType(mContext, intValue);
            setNotificationTypeSummary(intValue);
            return true;
        }
        if (preference == mRequestTimeout) {
            Constants.setTimeout(mContext, intValue);
            mRequestTimeout.setSummary(getString(
                    R.string.pref_request_timeout_summary, intValue));
        }
        return false;
    }

   private void readAccessOptions() {
        String value = SystemProperties.get(PREF_ROOT_ACCESS_PROPERTY, "0");
        mRootAccess.setValue(value);
        mRootAccess.setSummary(getResources()
                .getStringArray(R.array.pref_access_list_titles)[Integer.valueOf(value)]);
    }

    private void writeAccessOptions(Object newValue) {
        String oldValue = SystemProperties.get(PREF_ROOT_ACCESS_PROPERTY, "0");
        SystemProperties.set(PREF_ROOT_ACCESS_PROPERTY, newValue.toString());
        if (Integer.valueOf(newValue.toString()) < 2 && !oldValue.equals(newValue)
                && "1".equals(SystemProperties.get("service.adb.root", "0"))) {
            SystemProperties.set("service.adb.root", "0");
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.ADB_ENABLED, 0);
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.ADB_ENABLED, 1);
        }
        readAccessOptions();
    }

    private void setNotificationTypeSummary(int type) {
        switch (type) {
            case Constants.NOTIFICATION_TYPE_NONE:
                mNotifications.setSummary(R.string.pref_notifications_none);
                break;
            case Constants.NOTIFICATION_TYPE_TOAST:
                mNotifications.setSummary(R.string.pref_notifications_toast);
                break;
            case Constants.NOTIFICATION_TYPE_NOTIFICATION:
                mNotifications.setSummary(R.string.pref_notifications_notification);
                break;
        }
    }

    private void setMultiuserModeSummary(int mode) {
        switch (mode) {
            case Constants.MULTIUSER_MODE_OWNER_MANAGED:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_owner_managed);
                break;
            case Constants.MULTIUSER_MODE_OWNER_ONLY:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_owner_only);
                break;
            case Constants.MULTIUSER_MODE_USER:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_user);
                break;
        }
    }

    private void setAutoResponseSummary(int response) {
        switch (response) {
            case Constants.AUTOMATIC_RESPONSE_PROMPT:
                mAutoResponse.setSummary(R.string.pref_auto_response_prompt);
                break;
            case Constants.AUTOMATIC_RESPONSE_ALLOW:
                mAutoResponse.setSummary(R.string.pref_auto_response_allow);
                break;
            case Constants.AUTOMATIC_RESPONSE_DENY:
                mAutoResponse.setSummary(R.string.pref_auto_response_deny);
                break;
        }
    }

    private int getRequestTimeoutIndex(int timeout) {
        switch (timeout) {
            case Constants.REQUEST_TIMEOUT_TEN:
                return 0;
            case Constants.REQUEST_TIMEOUT_TWENTY:
                return 1;
            case Constants.REQUEST_TIMEOUT_THIRTY:
                return 2;
            case Constants.REQUEST_TIMEOUT_SIXTY:
                return 3;
        }
        return 2;
    }
}
