package copy.com.android.ddms;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Utility class to help using Table objects.
 *
 */
public final class TableHelper {
    /**
     * Create a TableColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param sample_text A sample text to figure out column width if preference
     *            value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     * @return The TableColumn object that was created
     */
    public static TableColumn createTableColumn(Table parent, String header,
            int style, String sample_text, final String pref_name
            /*final IPreferenceStore prefs*/) {

        // create the column
        TableColumn col = new TableColumn(parent, style);

              col.setText(sample_text);
            col.pack();

        // set the header
        col.setText(header);


        return col;
    }

    /**
     * Create a TreeColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param sample_text A sample text to figure out column width if preference
     *            value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     */
    public static void createTreeColumn(Tree parent, String header, int style,
            String sample_text, final String pref_name) {
        // create the column
        TreeColumn col = new TreeColumn(parent, style);

            col.setText(sample_text);
            col.pack();

        // set the header
        col.setText(header);

    }

    /**
     * Create a TreeColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param width the width of the column if the preference value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     */
    public static void createTreeColumn(Tree parent, String header, int style,
            int width, final String pref_name) {

        // create the column
        TreeColumn col = new TreeColumn(parent, style);

             col.setWidth(width);

        col.setText(header);

     }
}
