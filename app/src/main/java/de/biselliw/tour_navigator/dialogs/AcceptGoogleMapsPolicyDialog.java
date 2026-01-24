package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;

import static de.biselliw.tour_navigator.activities.SettingsActivity.consentGoogleMaps;

public class AcceptGoogleMapsPolicyDialog extends FullScreenDialog {

    public AcceptGoogleMapsPolicyDialog(AppCompatActivity activity) {
        super(activity, R.layout.consent_google_maps);

        /* define OnClick events for declining / accepting the policy */
        Button buttonDecline = findViewById(R.id.bt_decline_GoogleMaps);
        buttonDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consentGoogleMaps(false);
                dismiss();
            }
        });
        Button buttonConsent = findViewById(R.id.bt_consent_GoogleMaps);
        buttonConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consentGoogleMaps(true);
                dismiss();
            }
        });
    }
}
