package de.biselliw.tour_navigator.tim_prune.function.search;

import de.biselliw.tour_navigator.App;

/**
 * Function to load track information from any source,
 * subclassed for special cases like wikipedia or OSM
 */
public abstract class GenericDownloaderFunction /* extends GenericFunction */ implements Runnable
{
	/** error message */
	protected String _errorMessage = null;

    /** list model */
    TrackListModel _trackListModel = null;

	/**
	 * Constructor
	 * @param inApp App object
	 */
	public GenericDownloaderFunction(App inApp, TrackListModel inTrackListModel) {
//		super(inApp);
        _trackListModel = inTrackListModel;

    }

	/**
	 * Begin the function
	 */
	public void begin()
	{
		/* Initialise dialog, show empty list
		if (_dialog == null)
		{
			_dialog = new JDialog(_parentFrame, I18nManager.getText(getNameKey()), true);
			_dialog.setLocationRelativeTo(_parentFrame);
			_dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			// add closing listener
			_dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					_cancelled = true;
				}
			});
			_dialog.getContentPane().add(makeDialogComponents());
			_dialog.pack();
		}
		* /

		// Clear list
		_trackListModel.clear();
		_loadButton.setEnabled(false);
		_showButton.setEnabled(false);
		_cancelled = false;
		_descriptionBox.setText("");
		_errorMessage = null;
		// Start new thread to load list asynchronously
		new Thread(this).start();

		// Show dialog
		_dialog.setVisible(true);

		 */
	}

    public String getErrorMessage() {
            return _errorMessage;
    }
}
