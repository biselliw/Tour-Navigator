package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.adapter.SearchResultAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;

public abstract class SearchResultDialog extends FullScreenDialog {
    Context _context;
    /** track table */
    protected SearchResultAdapter _trackTable = null;
    private ListView recordsView;


    public List<RecordAdapter.Record> recordList;
    Handler timerHandler = new Handler();

    /** Description box */
    private TextView _descriptionBox = null;
    //protected JLabel _statusLabel = null;
    /** Load button */
     // private JButton _loadButton = null;
     /** Show button */
     // private JButton _showButton = null;
    /** Status label */

    protected String lang = "en";

    /** list model */
    protected TrackListModel _trackListModel = null;

    public SearchResultDialog(Context context, DataPoint inPoint) {
        super(context, R.layout.search_result_dialog);

        _context = context;
        lang = context.getString(R.string.lang);


        // Status label
//        _statusLabel = new JLabel("confirm.running");
//        dialogPanel.add(_statusLabel, BorderLayout.NORTH);
        // Main panel with track list
        _trackListModel = new TrackListModel(getColumnKey(0), getColumnKey(1));

        _trackTable = new SearchResultAdapter(this, findViewById(R.id.search_result_view), _trackListModel);
/*
        _trackTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                {
                    final int numSelected = _trackTable.getSelectedRowCount();
                    boolean foundUrl = false;
                    if (numSelected > 0)
                    {
                        setDescription(_trackListModel.getTrack(_trackTable.getSelectedRow()).getDescription());
                        _descriptionBox.setCaretPosition(0);
                        foundUrl = _trackListModel.getTrack(_trackTable.getSelectedRow()).getWebUrl() != null;
                    }
                    else {
                        _descriptionBox.setText("");
                    }
                    _loadButton.setEnabled(numSelected > 0);
                    _showButton.setEnabled(numSelected == 1 && foundUrl);
                }
            }
        });
 */
        /*
        _trackTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        if (_trackListModel.getColumnCount() > 1) {
            _trackTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        }
        JScrollPane tablePane = new JScrollPane(_trackTable);
        tablePane.setPreferredSize(new Dimension(450, 200));
         */
        /*

        // Panel to hold description label and box
        JPanel descPanel = new JPanel();
        descPanel.setLayout(new BorderLayout());
        JLabel descLabel = new JLabel(I18nManager.getText("dialog.gpsies.description") + " :");
        descPanel.add(descLabel, BorderLayout.NORTH);
        */
        _descriptionBox = findViewById(R.id.desc_search_result_view);
        _descriptionBox.setText("wikipedia");

//        _descriptionBox.setEditable(false);
//        _descriptionBox.setLineWrap(true);
//        _descriptionBox.setWrapStyleWord(true);
        /*
        JScrollPane descPane = new JScrollPane(_descriptionBox);
        descPane.setPreferredSize(new Dimension(400, 80));
        descPanel.add(descPane, BorderLayout.CENTER);
        // Use split pane to split table from description
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePane, descPanel);
        splitPane.setResizeWeight(1.0);
        dialogPanel.add(splitPane, BorderLayout.CENTER);

        // button panel at bottom
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        _loadButton = new JButton(I18nManager.getText("button.load"));
        _loadButton.setEnabled(false);
        _loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                loadSelected();
            }
        });
        buttonPanel.add(_loadButton);
        _showButton = new JButton(I18nManager.getText("button.showwebpage"));
        _showButton.setEnabled(false);
        _showButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                showSelectedWebpage();
            }
        });
        buttonPanel.add(_showButton);
        JButton cancelButton = new JButton(I18nManager.getText("button.cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                _cancelled = true;
                _dialog.dispose();
            }
        });
        buttonPanel.add(cancelButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 15));
        return dialogPanel;
         */



        /* define OnClick event for changing the start time */
        Button buttonOkay = (Button) findViewById(R.id.bt_start_ok2);
        buttonOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismiss();
            }
        });

        // Clear list
        // _trackListModel.clear();
        /*

        _loadButton.setEnabled(false);
        _showButton.setEnabled(false);
        _cancelled = false;
         */
//        _descriptionBox.setText("");
//        _errorMessage = null;
        // Start new thread to load list asynchronously
        // new Thread(this).start();

        // Show dialog
  //      _dialog.setVisible(true);

        /* Install a timer to handle all activities */

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (_trackListModel.changed) {
                    _trackTable.notifyDataSetChanged();
                    _trackListModel.changed = false;
                }
                timerHandler.postDelayed(this, 100);
            }
        };
        timerHandler.postDelayed(timerRunnable, 100);
    }


    private int getViewWidth(int id) {
        TextView view = findViewById(id);
        return view.getWidth();
    }

    /**
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     */
    protected abstract String getColumnKey(int inColNum);
    /**
     * Set the description in the box
     * @param inDesc description to set, or null for no description
     */
    public void setDescription(String inDesc)
    {
        String text = inDesc;
        if (inDesc == null || inDesc.length() < 2) {
            text = "dialog.gpsies.nodescription";
        }
        _descriptionBox.setText(text);
    }

    /**
     * Sets an item in the list of places
     *
     * @param inPlace Index (starting at 0) of the data item to be selected or -1 if nothing
     */
    public void setPlace(int inPlace) {

   //     notifyDataSetChanged();
    }

}
