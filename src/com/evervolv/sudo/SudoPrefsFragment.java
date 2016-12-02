package com.evervolv.sudo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.evervolv.sudo.R;
import com.evervolv.sudo.app.AppPolicyActivity;
import com.evervolv.sudo.app.AppLogsActivity;
import com.evervolv.sudo.preference.PinEntryPreference;
import com.evervolv.sudo.util.Constants;
import com.evervolv.sudo.util.PinViewHelper;

public class SudoPrefsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_ROOT_ACCESS = "root_access";
    private static final String PREF_ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private static final String PREF_AUTO_RESPONSE = "auto_response";
    private static final String PREF_MULTIUSER_POLICY = "multiuser_policy";
    private static final String PREF_PIN_ENTRY = "pin_entry";

    private static final String PREF_LOGGING = "logging";
    private static final String PREF_NOTIFICATIONS = "notifications";
    private static final String PREF_REQUEST_TIMEOUT = "request_timeout";

    private PreferenceScreen mPrefSet;
    private Context mContext;

    private ListPreference mRootAccess;
    private ListPreference mAutoResponse;
    private ListPreference mMultiuserPolicy;
    private PinEntryPreference mPin;

    private SwitchPreference mLogging;
    private ListPreference mNotifications;
    private ListPreference mRequestTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.sudo_prefs);

        mContext = getActivity();
        mPrefSet = getPreferenceScreen();

        /* Access */
        mRootAccess = (ListPreference) mPrefSet.findPreference(PREF_ROOT_ACCESS);
        mRootAccess.setOnPreferenceChangeListener(this);

        mAutoResponse = (ListPreference) mPrefSet.findPreference(
                PREF_AUTO_RESPONSE);
        mAutoResponse.setOnPreferenceChangeListener(this);
        mAutoResponse.setValueIndex(Constants.getAutomaticResponse(mContext));
        setAutoResponseSummary(Constants.getAutomaticResponse(mContext));

        mMultiuserPolicy = (ListPreference) mPrefSet.findPreference(PREF_MULTIUSER_POLICY);
        mMultiuserPolicy.setOnPreferenceChangeListener(this);

        if (Constants.getMultiuserMode(mContext) != Constants.MULTIUSER_MODE_NONE) {
            int mode = Constants.getMultiuserMode(mContext);
            setMultiuserModeSummary(mode);
            mMultiuserPolicy.setValueIndex(mode);
        } else {
            mPrefSet.removePreference(mMultiuserPolicy);
        }

        mPin = (PinEntryPreference) mPrefSet.findPreference(PREF_PIN_ENTRY);
        mPin.setOnPreferenceChangeListener(this);

        mLogging = (SwitchPreference) mPrefSet.findPreference(PREF_LOGGING);
        mLogging.setChecked(Constants.getLogging(mContext));

        mNotifications = (ListPreference) mPrefSet.findPreference(PREF_NOTIFICATIONS);
        mNotifications.setValueIndex(Constants.getNotificationType(mContext));
        mNotifications.setOnPreferenceChangeListener(this);
        setNotificationTypeSummary(Constants.getNotificationType(mContext));

        mRequestTimeout = (ListPreference) mPrefSet.findPreference(PREF_REQUEST_TIMEOUT);
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
                Intent applist = new Intent(getActivity(), AppPolicyActivity.class);
                startActivity(applist);
                return true;
            case R.id.menu_logs:
                Intent logs = new Intent(getActivity(), AppLogsActivity.class);
                startActivity(logs);
                return true;
           default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRootAccess) {
            writeAccessOptions(Integer.valueOf((String) newValue));
            return true;
        } else if (preference == mAutoResponse) {
            int response = Integer.valueOf((String) newValue);
            setAutoResponseSummary(response);
            Constants.setAutomaticResponse(mContext, response);
            return true;
        } else if (preference == mMultiuserPolicy) {
            int mode = Integer.valueOf((String) newValue);
            setMultiuserModeSummary(mode);
            Constants.setMultiuserMode(mContext, mode);
            return true;
        } else if (preference == mPin) {
            if (Constants.isPinProtected(mContext)) {
                final Dialog dlg = new Dialog(mContext);
                dlg.setTitle(R.string.verify_pin);
                dlg.setContentView(new PinViewHelper((LayoutInflater)
                        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                        null, null) {

                    public void onEnter(String password) {
                        super.onEnter(password);
                        if (Constants.checkPin(mContext, password)) {
                            super.onEnter(password);
                            dlg.dismiss();
                            return;
                        }
                        Toast.makeText(mContext, getString(R.string.incorrect_pin),
                                Toast.LENGTH_SHORT).show();
                    };

                    public void onCancel() {
                        super.onCancel();
                        dlg.dismiss();
                        mPin.getDialog().dismiss();
                    };

                }.getView(), new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                dlg.show();
            }
            if (!newValue.equals("")) {
                Constants.setPin(mContext, newValue.toString());
            } else {
                Constants.setPin(mContext, null);
            }
            return true;
        } else if (preference == mLogging) {
            Constants.setLogging(mContext, mLogging.isChecked());
            return true;
        } else if (preference == mNotifications) {
            int notifType = Integer.valueOf((String) newValue);
            Constants.setNotificationType(mContext, notifType);
            setNotificationTypeSummary(notifType);
            return true;
        } else if (preference == mRequestTimeout) {
            int timeout = Integer.valueOf((String) newValue);
            Constants.setTimeout(mContext, timeout);
            mRequestTimeout.setSummary(getString(
                    R.string.pref_request_timeout_summary, timeout));
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

    private void resetSuperuserAccessOptions() {
        String oldValue = SystemProperties.get(PREF_ROOT_ACCESS_PROPERTY, "0");
        SystemProperties.set(PREF_ROOT_ACCESS_PROPERTY, "0");
        if (!oldValue.equals("0") && "1".equals(SystemProperties.get("service.adb.root", "0"))) {
            SystemProperties.set("service.adb.root", "0");
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 0);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 1);
        }
        readAccessOptions();
    }

    private void setNotificationTypeSummary(int type) {
        switch (type) {
            case Constants.NOTIFICATION_TYPE_NONE:
                mNotifications.setSummary(
                        R.string.pref_notifications_no_notification_summary);
                break;
            case Constants.NOTIFICATION_TYPE_TOAST:
                mNotifications.setSummary(getString(
                        R.string.pref_notifications_summary,
                        getString(R.string.pref_notifications_toast).toLowerCase()));
                break;
            case Constants.NOTIFICATION_TYPE_NOTIFICATION:
                mNotifications.setSummary(getString(
                        R.string.pref_notifications_summary,
                        getString(R.string.pref_notifications_notification).toLowerCase()));
                break;
        }
    }

    private void setMultiuserModeSummary(int mode) {
        switch (mode) {
            case Constants.MULTIUSER_MODE_OWNER_MANAGED:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_owner_managed_summary);
                break;
            case Constants.MULTIUSER_MODE_OWNER_ONLY:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_owner_only_summary);
                break;
            case Constants.MULTIUSER_MODE_USER:
                mMultiuserPolicy.setSummary(R.string.pref_multiuser_user_summary);
                break;
        }
    }

    private void setAutoResponseSummary(int response) {
        switch (response) {
            case Constants.AUTOMATIC_RESPONSE_PROMPT:
                mAutoResponse.setSummary(R.string.pref_auto_response_prompt_summary);
                break;
            case Constants.AUTOMATIC_RESPONSE_ALLOW:
                mAutoResponse.setSummary(R.string.pref_auto_response_allow_summary);
                break;
            case Constants.AUTOMATIC_RESPONSE_DENY:
                mAutoResponse.setSummary(R.string.pref_auto_response_deny_summary);
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
