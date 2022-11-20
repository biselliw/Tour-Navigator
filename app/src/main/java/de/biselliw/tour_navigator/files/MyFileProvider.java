package de.biselliw.tour_navigator.files;

import androidx.core.content.FileProvider;
import de.biselliw.tour_navigator.R;

public class MyFileProvider extends FileProvider {
    public MyFileProvider() {
        super(R.xml.file_paths);
    }
}
