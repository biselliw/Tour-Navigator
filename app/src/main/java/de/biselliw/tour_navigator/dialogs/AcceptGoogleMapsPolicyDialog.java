package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;

import static de.biselliw.tour_navigator.activities.SettingsActivity.consentGoogleMaps;

public class AcceptGoogleMapsPolicyDialog extends FullScreenDialog {

    public AcceptGoogleMapsPolicyDialog(Context context, MainActivity activity) {
        super(activity, R.layout.consent_google_maps);

        /* define OnClick events for declining / accepting the policy */
        Button buttonDecline = (Button) findViewById(R.id.bt_decline_GoogleMaps);
        buttonDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consentGoogleMaps(false);
                dismiss();
            }
        });
        Button buttonConsent = (Button) findViewById(R.id.bt_consent_GoogleMaps);
        buttonConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consentGoogleMaps(true);
                dismiss();
                activity.runGoogleMaps();
            }
        });
    }
}
