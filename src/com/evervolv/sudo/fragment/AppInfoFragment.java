package com.evervolv.sudo.fragment;

import android.annotation.Nullable;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.evervolv.sudo.R;
import com.evervolv.sudo.database.LogEntry;
import com.evervolv.sudo.database.DatabaseHelper;
import com.evervolv.sudo.database.UidPolicy;
import com.evervolv.sudo.fragment.AppPolicyFragment;
import com.evervolv.sudo.util.Helper;

import java.util.ArrayList;
import java.util.List;

public class AppInfoFragment extends Fragment {

    private Context mContext;
    private Resources mRes;
    private UidPolicy mCurrPolicy;

    private ImageView mAppIcon;
    private ImageView mPolicyIcon;
    private TextView mAppName;
    private TextView mAppId;
    private TextView mAppPackage;
    private TextView mAppRequestedUuid;
    private TextView mAppCommand;
    private Switch mAppEnableLogging;
    private Switch mAppEnableNotifications;
    private ListView mAppLogList;

    public AppInfoFragment() { }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        mContext = getContext();
        mRes = mContext.getResources();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_info, container, false);

        mAppIcon = (ImageView) v.findViewById(R.id.info_app_icon);
        mPolicyIcon = (ImageView) v.findViewById(R.id.info_policy_icon);
        mAppName = (TextView) v.findViewById(R.id.info_app_name);
        mAppId = (TextView) v.findViewById(R.id.info_app_id);
        mAppPackage = (TextView) v.findViewById(R.id.info_app_package);
        mAppRequestedUuid = (TextView) v.findViewById(R.id.info_app_requested_uuid);
        mAppCommand = (TextView) v.findViewById(R.id.info_app_command);
        mAppLogList = (ListView) v.findViewById(R.id.log_list);

        mAppEnableLogging = (Switch) v.findViewById(R.id.info_app_logging);
        mAppEnableLogging.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mCurrPolicy.logging = isChecked;
                DatabaseHelper.Binary.setPolicy(mContext, mCurrPolicy);
            }
        });

        mAppEnableNotifications = (Switch) v.findViewById(R.id.info_app_notification);
        mAppEnableNotifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mCurrPolicy.notification = isChecked;
                DatabaseHelper.Binary.setPolicy(mContext, mCurrPolicy);
            }
        });

        //Get the first policy
        if (mCurrPolicy == null) {
            UidPolicy firstPolicy = DatabaseHelper.Binary.getPolicy(mContext, 0);
            if (firstPolicy != null) {
                setPolicy(firstPolicy);
            }
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (AppPolicyFragment.isSlidingPaneOpen() == false) {
            getActivity().getActionBar().setTitle(getCurrentPolicyName());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.app_policy_info_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete_policy) {
            deleteCurrentPolicy();
            return true;
        }
        return false;
    }

    public void deleteCurrentPolicy() {
        AppListFragment frag = (AppListFragment) getFragmentManager()
                .findFragmentById(R.id.list_pane);
        frag.deletePolicy(mCurrPolicy);
        AppPolicyFragment.togglePane();
    }

    public void setPolicy(UidPolicy policy) {
        mCurrPolicy = policy;

        ArrayList<LogEntry> logs;
        logs = DatabaseHelper.Application.getLogs(getActivity(), policy, -1);
        mAppLogList.setAdapter(new PolicyLogAdapter(getActivity(),
                R.layout.policy_log_list_item, logs));

        mAppIcon.setImageDrawable(Helper.loadPackageIcon(mContext, policy.packageName));
        mPolicyIcon.setAlpha(0.5f);
        if (policy.policy.equals(UidPolicy.ALLOW)) {
            mPolicyIcon.setImageResource(R.drawable.ic_allowed);
        } else if (policy.policy.equals(UidPolicy.DENY)) {
            mPolicyIcon.setImageResource(R.drawable.ic_denied);
        } else {
            mPolicyIcon.setImageResource(0);
        }

        mAppName.setText(policy.name);
        mAppId.setText(Integer.toString(policy.uid));
        mAppPackage.setText(policy.packageName);
        mAppRequestedUuid.setText(Integer.toString(policy.desiredUid));
        mAppCommand.setText(TextUtils.isEmpty(policy.command) ? mRes.getString(
                R.string.app_all_commands) : policy.command);
        mAppEnableLogging.setChecked(policy.logging);
        mAppEnableNotifications.setChecked(policy.notification);
    }

    public String getCurrentPolicyName() {
        if (mCurrPolicy == null) {
            //TODO: Should we do this different?
            //      Maybe a default bogus policy?
            return "";
        }
        return mCurrPolicy.name;
    }

    public class PolicyLogAdapter extends ArrayAdapter<LogEntry> {

        private Context mContext;

        public PolicyLogAdapter(Context context, int resource,
                List<LogEntry> objects) {
            super(context, resource, objects);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.policy_log_list_item,
                    parent, false);

            TextView logDate = (TextView) v.findViewById(R.id.policy_log_date);
            TextView logAction = (TextView) v.findViewById(R.id.policy_log_action);

            LogEntry log = getItem(position);
            java.text.DateFormat time = DateFormat.getTimeFormat(getActivity());
            java.text.DateFormat day = DateFormat.getDateFormat(getActivity());
            String dateTime = (day.format(log.getDate()) + " - " +
                    time.format(log.getDate()));

            logDate.setText(dateTime);
            logAction.setText(log.getActionResource());

            return v;
        }

    }

}
