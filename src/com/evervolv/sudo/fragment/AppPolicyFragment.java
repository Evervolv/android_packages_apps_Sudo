package com.evervolv.sudo.fragment;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.evervolv.sudo.R;

public class AppPolicyFragment extends Fragment {

    private static SlidingPaneLayout mSlidingPaneLayout;
    private static boolean mPaneOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.app_list_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mSlidingPaneLayout = (SlidingPaneLayout) view.findViewById(R.id.sliding_pane);
        mSlidingPaneLayout.setParallaxDistance(200);
        mSlidingPaneLayout.setShadowResource(R.drawable.sliding_pane_shadow);
        mSlidingPaneLayout.setSliderFadeColor(getContext().getResources().getColor(R.color.slider_fade));
        mSlidingPaneLayout.setPanelSlideListener(new PanelSlideListener() {
            AppInfoFragment infoFrag = (AppInfoFragment) getFragmentManager()
                    .findFragmentById(R.id.info_pane);
            @Override
            public void onPanelClosed(View panel) {
                getActivity().getActionBar().setTitle(R.string.app_policies_logs_title);
                mPaneOpen = false;
            }

            @Override
            public void onPanelOpened(View panel) {
                getActivity().getActionBar().setTitle(R.string.app_policies_title);
                mPaneOpen = true;
            }

            @Override
            public void onPanelSlide(View view, float v) { }
        });

        mSlidingPaneLayout.openPane();
    }

    public static void togglePane() {
        if (mSlidingPaneLayout.isOpen()) {
            mSlidingPaneLayout.closePane();
        } else {
            mSlidingPaneLayout.openPane();
        }
    }

    public static boolean isSlidingPaneOpen() {
        return mPaneOpen;
    }
}
