package de.biselliw.tour_navigator.dialogs;

import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.SettingsActivity;

/**
 * Dialog to accept/discard Wikipedia policy
 */
public class AcceptWikipediaPolicyDialog extends FullScreenDialog {

    public AcceptWikipediaPolicyDialog(AppCompatActivity activity) {
        super(activity, R.layout.dialog_consent_wikipedia);

        /* define OnClick events for declining / accepting the policy */
        Button buttonDecline = findViewById(R.id.bt_decline);
        buttonDecline.setOnClickListener(v -> {
            SettingsActivity.consentWikipedia(false);
            dismiss();
        });
        Button buttonConsent = findViewById(R.id.bt_consent);
        buttonConsent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.consentWikipedia(true);
                dismiss();
            }
        });
    }
}
