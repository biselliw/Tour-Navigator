package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.ui.ControlElements;

/**
 * Dialog to comment the selected route point
 */
public class CommentDialog  extends FullScreenDialog {

    public CommentDialog(Context context, RecordAdapter recordAdapter) {
        super(context, R.layout.dialog_comment);
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                DataPoint dataPoint = record.getTrackPoint();
                TextView view = findViewById(R.id.tvComment);
                view.setText(dataPoint.getRoutePointName());

                EditText edit = findViewById(R.id.etComment);
                edit.setText(dataPoint.getComment());

                recordAdapter.notifyDataSetChanged();
                Button btnTake = findViewById(R.id.btComment);
                btnTake.setOnClickListener(view1 -> {
                    dataPoint.setFieldValue(Field.COMMENT, edit.getText().toString(), false);
                    ControlElements.updateAdditionalInfo();
                    ControlElements.updateGpxFile = true;
                    dismiss();
                });
                /* define OnClick event to cancel the dialog */
                Button cancelButton = findViewById(R.id.btn_cancel);
                cancelButton.setOnClickListener(v -> {
                    dismiss();
                });
            }
        }
    }
}
