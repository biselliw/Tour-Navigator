package de.biselliw.tour_navigator.dialogs;

import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.SettingsActivity;

/**
 * Dialog to accept/discard VMV public transport policy
 */
public class AcceptVmvPolicyDialog extends FullScreenDialog {

    public AcceptVmvPolicyDialog(AppCompatActivity activity) {
        super(activity, R.layout.dialog_consent_vmv);

        /* define OnClick events for declining / accepting the policy */
        Button buttonDecline = findViewById(R.id.bt_decline);
        buttonDecline.setOnClickListener(v -> {
            SettingsActivity.consentVMV(false);
            dismiss();
        });
        Button buttonConsent = findViewById(R.id.bt_consent);
        buttonConsent.setOnClickListener(v -> {
            SettingsActivity.consentVMV(true);
            dismiss();
        });

    }
}
