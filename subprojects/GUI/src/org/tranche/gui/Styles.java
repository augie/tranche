/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Abstraction for representing styles that are used throughout the data.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class Styles {

    // regular fonts
    public static final Font FONT_8PT = new Font("Default", Font.PLAIN, 8);
    public static final Font FONT_10PT = new Font("Default", Font.PLAIN, 10);
    public static final Font FONT_11PT = new Font("Default", Font.PLAIN, 11);
    public static final Font FONT_12PT = new Font("Default", Font.PLAIN, 12);
    public static final Font FONT_14PT = new Font("Default", Font.PLAIN, 14);
    public static final Font FONT_16PT = new Font("Default", Font.PLAIN, 16);
    public static final Font FONT_18PT = new Font("Default", Font.PLAIN, 18);
    // bold fonts
    public static final Font FONT_8PT_BOLD = new Font("Default", Font.BOLD, 8);
    public static final Font FONT_10PT_BOLD = new Font("Default", Font.BOLD, 10);
    public static final Font FONT_11PT_BOLD = new Font("Default", Font.BOLD, 11);
    public static final Font FONT_12PT_BOLD = new Font("Default", Font.BOLD, 12);
    public static final Font FONT_14PT_BOLD = new Font("Default", Font.BOLD, 14);
    public static final Font FONT_16PT_BOLD = new Font("Default", Font.BOLD, 16);
    public static final Font FONT_18PT_BOLD = new Font("Default", Font.BOLD, 18);
    public static final Font FONT_24PT_BOLD = new Font("Default", Font.BOLD, 24);
    // italic
    public static final Font FONT_10PT_ITALIC = new Font("Default", Font.ITALIC, 10);
    public static final Font FONT_11PT_ITALIC = new Font("Default", Font.ITALIC, 11);
    public static final Font FONT_18PT_ITALIC = new Font("Default", Font.ITALIC, 18);
    // the monospaced fonts
    public static final Font FONT_8PT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 8);
    public static final Font FONT_10PT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 10);
    public static final Font FONT_12PT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 12);
    // times new roman
    public static final Font FONT_28PT_TIMES_BOLD = new Font("Times New Roman", Font.BOLD, 28);
    // progress bar font
    public static final Font FONT_PROGRESSBAR_SMALL = FONT_8PT_BOLD;
    public static final Font FONT_DEFAULT = FONT_14PT;
    public static final Font FONT_TITLE = FONT_18PT_BOLD;
    public static final Font FONT_TITLE_1 = FONT_14PT_BOLD;
    // default colors
    public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color COLOR_LIGHT_BLUE = new Color((float) 0.80, (float) 0.80, (float) 1.0);
    public static final Color COLOR_EXTRA_LIGHT_BLUE = new Color((float) 0.95, (float) 0.95, (float) 1.0);
    public static final Color COLOR_DARK_RED = new Color(156, 0, 1);
    public static final Color COLOR_DARKER_RED = new Color((float) 0.9, 0, 0);
    public static final Color COLOR_BLUE = new Color(0, 0, 250);
    public static final Color COLOR_EXTRA_LIGHT_GRAY = new Color(228, 228, 228);
    public static final Color COLOR_MAROON = new Color(128, 0, 0);
    public static final Color COLOR_DARK_GREEN = new Color(19, 63, 40);
    public static final Color COLOR_DARKER_GREEN = new Color((float) 0, (float) 0.9, (float) 0);

    // border colors
    public static final Color COLOR_BORDER_DARK = new Color(100, 100, 100);
    public static final Color COLOR_BORDER = new Color(200, 200, 200);
    // colors for panels/frames/contentpanes
    public static Color COLOR_PANEL_BACKGROUND = new Color(235, 235, 235);
    public static Color COLOR_BACKGROUND = new Color(235, 235, 235);
    public static Color COLOR_BACKGROUND_LIGHT = new Color(250, 250, 250);
    public static Color COLOR_SELECTED_BACKGROUND = new Color(163, 184, 204);
    // colors for the progress bars
    public static Color COLOR_PROGRESS_BAR_FOREGROUND = Color.LIGHT_GRAY;
    public static Color COLOR_PROGRESS_BAR_BACKGROUND = Color.WHITE;
    public static Color COLOR_PROGRESS_BAR_TEXT_ENABLED = Color.BLACK;
    public static Color COLOR_PROGRESS_BAR_TEXT_DISABLED = Color.GRAY;
    // menu bar colors
    public static Color COLOR_MENU_BAR_BACKGROUND = Color.DARK_GRAY;
    public static Color COLOR_MENU_BAR_FOREGROUND = Color.WHITE;
    public static Color COLOR_MENU_BAR_SELECTION_BACKGROUND = Styles.COLOR_DARK_RED;
    public static Color COLOR_MENU_BAR_SELECTION_FOREGROUND = Color.WHITE;
    // trim color
    public static Color COLOR_TRIM = Styles.COLOR_DARK_RED;
    // tranche logo color
    public static int INT_TRANCHE_COLORS_TO_USE = 2;
    public static Color COLOR_TRANCHE_LOGO = new Color(153, 51, 51, 240);
    public static Color COLOR_TRANCHE_LOGO2 = new Color(225, 51, 51, 240);
    // frame icon image
    public static Image IMAGE_FRAME_ICON = null;


    static {
        try {
            IMAGE_FRAME_ICON = ImageIO.read(GenericFrame.class.getResourceAsStream("/org/tranche/gui/image/icon.jpg"));
        } catch (Exception e) {
            // do nothing
        }
    }
    // make the borders
    public static final Border BORDER_BLACK_1 = BorderFactory.createLineBorder(Color.BLACK, 1);
    public static final Border BORDER_BUTTON_TOP = BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK), BorderFactory.createEmptyBorder(4, 6, 4, 6));
    public static final Border BORDER_BUTTON = BorderFactory.createCompoundBorder(BORDER_BLACK_1, BorderFactory.createEmptyBorder(4, 6, 4, 6));
    public static final Border BORDER_GRAY_1 = BorderFactory.createLineBorder(Color.GRAY, 1);
    public static final Border BORDER_NONE = BorderFactory.createEmptyBorder();
    public static final Border BORDER_BLACK_1_PADDING_SIDE_2 = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(0, 2, 1, 2));
    // underline borders
    public static final Border UNDERLINE_BLACK = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK);
    public static final Border UNDERLINE_WHITE = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE);
    public static final Border UNDERLINE_DARK_GREEN = BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_DARK_GREEN);
    public static final Border UNDERLINE_MAROON = BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_MAROON);
    public static final Border UNDERLINE_NONE = BorderFactory.createMatteBorder(0, 0, 0, 0, Color.WHITE);
    public static final Border UNDERLINE_EXTRA_LIGHT_GRAY = BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_EXTRA_LIGHT_GRAY);
    public static final Border UNDERLINE_DARK_GREY = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY);
    // thickness of 3
    private static final byte thickness = 3;
    public static final Border UNDERLINE_BLACK_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, Color.BLACK);
    public static final Border UNDERLINE_WHITE_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, Color.WHITE);
    public static final Border UNDERLINE_DARK_GREEN_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, COLOR_DARK_GREEN);
    public static final Border UNDERLINE_MAROON_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, COLOR_MAROON);
    public static final Border UNDERLINE_EXTRA_LIGHT_GRAY_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, COLOR_EXTRA_LIGHT_GRAY);
    public static final Border UNDERLINE_DARK_GREY_THICK = BorderFactory.createMatteBorder(0, 0, thickness, 0, Color.DARK_GRAY);


    static {
        // set anti-aliasing
        System.setProperty("swing.aatext", "true");

        // set the styling for progress bars
        {
            UIManager.put("ProgressBar.background", COLOR_EXTRA_LIGHT_GRAY);
            UIManager.put("ProgressBar.foreground", Color.GRAY);
            UIManager.put("ProgressBar.selectionBackground", Color.DARK_GRAY);
            UIManager.put("ProgressBar.shadow", COLOR_TRANSPARENT);
            UIManager.put("ProgressBar.highlight", COLOR_TRANSPARENT);
            UIManager.put("ProgressBar.border", BORDER_NONE);
        }

        // set the styling for text fields
        {
            UIManager.put("TextField.border", Styles.BORDER_BLACK_1);
            UIManager.put("PasswordField.border", Styles.BORDER_BLACK_1);
        }

        // set the styling for buttons
        {
            UIManager.put("Button.background", Styles.COLOR_EXTRA_LIGHT_GRAY);
            UIManager.put("Button.focus", Styles.COLOR_EXTRA_LIGHT_GRAY);
            UIManager.put("Button.border", Styles.BORDER_BUTTON);
        }
    }
}
