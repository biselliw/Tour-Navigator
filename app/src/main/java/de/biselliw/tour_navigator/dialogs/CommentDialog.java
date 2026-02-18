package de.biselliw.tour_navigator.dialogs;

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public LicenseIf not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.adapter.RecordAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.ui.ControlElements;

/**
 * Dialog to comment the selected route point
 */
public class CommentDialog extends FullScreenDialog {

    public CommentDialog(Context context, RecordAdapter recordAdapter) {
        super(context, R.layout.dialog_comment);
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                DataPoint dataPoint = record.trackPoint;
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
