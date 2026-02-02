package de.biselliw.tour_navigator.dialogs;

import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.SettingsActivity;


public class AcceptGoogleMapsPolicyDialog extends FullScreenDialog {

    SettingsActivity settings;

    public AcceptGoogleMapsPolicyDialog(AppCompatActivity activity) {
        super(activity, R.layout.dialog_consent_google_maps);

//        settings = (SettingsActivity)activity;

        /* define OnClick events for declining / accepting the policy */
        Button buttonDecline = findViewById(R.id.bt_decline_GoogleMaps);
        buttonDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.consentGoogleMaps(false);
                dismiss();
            }
        });
        Button buttonConsent = findViewById(R.id.bt_consent_GoogleMaps);
        buttonConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.consentGoogleMaps(true);
                dismiss();
            }
        });
    }
}
