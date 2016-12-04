package com.evervolv.sudo.app;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.view.MenuItem;
import android.view.View;

import com.evervolv.sudo.R;
import com.evervolv.sudo.fragment.AppInfoFragment;

public class AppPolicyActivity extends Activity {

    private SlidingPaneLayout mSlidingPaneLayout;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list_activity);

        Resources res = this.getResources();

        mSlidingPaneLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane);
        mSlidingPaneLayout.setParallaxDistance(200);
        mSlidingPaneLayout.setShadowResource(R.drawable.sliding_pane_shadow);
        mSlidingPaneLayout.setSliderFadeColor(res.getColor(R.color.slider_fade));
        mSlidingPaneLayout.setPanelSlideListener(new PanelSlideListener() {
            AppInfoFragment infoFrag = (AppInfoFragment) getFragmentManager()
                    .findFragmentById(R.id.info_pane);
            @Override
            public void onPanelClosed(View panel) {
                infoFrag.setHasOptionsMenu(true);
                setTitle(infoFrag.getCurrentPolicyName());
            }

            @Override
            public void onPanelOpened(View panel) {
                infoFrag.setHasOptionsMenu(false);
                setTitle(R.string.app_policies_title);
            }

            @Override
            public void onPanelSlide(View view, float v) { }
        });

        mSlidingPaneLayout.openPane();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void togglePane() {
        if (mSlidingPaneLayout.isOpen()) {
            mSlidingPaneLayout.closePane();
        } else {
            mSlidingPaneLayout.openPane();
        }
    }
}
