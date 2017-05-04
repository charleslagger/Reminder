package com.embeddedlog.LightUpDroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.embeddedlog.LightUpDroid.ColorPicker.ColorPickerDialog;

import static com.embeddedlog.LightUpDroid.ColorPicker.ColorPickerSwatch.OnColorSelectedListener;
import static com.embeddedlog.LightUpDroid.DataUtils.NEW_NOTE_REQUEST;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_BODY;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_COLOUR;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_FONT_SIZE;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_HIDE_BODY;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_REQUEST_CODE;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_TITLE;


public class EditActivity extends ActionBarActivity implements Toolbar.OnMenuItemClickListener {

    private EditText titleEdit, bodyEdit;
    private RelativeLayout relativeLayoutEdit;
    private Toolbar toolbar;
    private MenuItem menuHideBody;

    private InputMethodManager imm;
    private Bundle bundle;

    private String[] colourArr;
    private int[] colourArrResId;
    private int[] fontSizeArr;
    private String[] fontSizeNameArr;

    private String colour = "#FFFFFF";
    private int fontSize = 18;
    private Boolean hideBody = false;

    private AlertDialog fontDialog, saveChangesDialog;
    private ColorPickerDialog colorPickerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        colourArr = getResources().getStringArray(R.array.colours);

        colourArrResId = new int[colourArr.length];
        for (int i = 0; i < colourArr.length; i++)
            colourArrResId[i] = Color.parseColor(colourArr[i]);

        fontSizeArr = new int[] {14, 18, 22};
        fontSizeNameArr = getResources().getStringArray(R.array.fontSizeNames);

        setContentView(R.layout.activity_edit);

        toolbar = (Toolbar)findViewById(R.id.toolbarEdit);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        bodyEdit = (EditText)findViewById(R.id.bodyEdit);
        relativeLayoutEdit = (RelativeLayout)findViewById(R.id.relativeLayoutEdit);
        ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView);

        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!bodyEdit.isFocused()) {
                    bodyEdit.requestFocus();
                    bodyEdit.setSelection(bodyEdit.getText().length());
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);

                    return true;
                }

                return false;
            }
        });

        bundle = getIntent().getExtras();

        if (bundle != null) {
            if (bundle.getInt(NOTE_REQUEST_CODE) != NEW_NOTE_REQUEST) {
                colour = bundle.getString(NOTE_COLOUR);
                fontSize = bundle.getInt(NOTE_FONT_SIZE);
                hideBody = bundle.getBoolean(NOTE_HIDE_BODY);

                titleEdit.setText(bundle.getString(NOTE_TITLE));
                bodyEdit.setText(bundle.getString(NOTE_BODY));
                bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                if (hideBody)
                    menuHideBody.setTitle(R.string.action_show_body);
            }

            else if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST) {
                titleEdit.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
        }

        initDialogs(this);
    }

    protected void initToolbar() {
        toolbar.setTitle("");

        toolbar.setNavigationIcon(R.drawable.ic_action_add_note);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        toolbar.inflateMenu(R.menu.menu_edit);

        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null)
            menuHideBody = menu.findItem(R.id.action_hide_show_body);
    }

    protected void initDialogs(Context context) {

        colorPickerDialog = ColorPickerDialog.newInstance(R.string.dialog_note_colour,
                colourArrResId, Color.parseColor(colour), 3,
                isTablet(this) ? ColorPickerDialog.SIZE_LARGE : ColorPickerDialog.SIZE_SMALL);


        colorPickerDialog.setOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {

                String selectedColourAsString = String.format("#%06X", (0xFFFFFF & color));

                for (String aColour : colourArr)
                    if (aColour.equals(selectedColourAsString))
                        colour = aColour;

                relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
            }
        });


        fontDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_font_size)
                .setItems(fontSizeNameArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        fontSize = fontSizeArr[which];
                        bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        saveChangesDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_save_changes)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (!isEmpty(titleEdit))
                            saveChanges();
                        else
                            toastEditTextCannotBeEmpty();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (bundle != null && bundle.getInt(NOTE_REQUEST_CODE) ==
                                NEW_NOTE_REQUEST) {

                            Intent intent = new Intent();
                            intent.putExtra("request", "discard");

                            setResult(RESULT_CANCELED, intent);

                            imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                            dialog.dismiss();
                            finish();
                            overridePendingTransition(0, 0);
                        }
                    }
                })
                .create();
    }


    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.action_note_colour) {
            colorPickerDialog.show(getFragmentManager(), "colourPicker");
            return true;
        }

        if (id == R.id.action_font_size) {
            fontDialog.show();
            return true;
        }
    if (id == R.id.action_hide_show_body) {

            if (!hideBody) {
                hideBody = true;
                menuHideBody.setTitle(R.string.action_show_body);


                Toast toast = Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_note_body_hidden),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            else {
                hideBody = false;
                menuHideBody.setTitle(R.string.action_hide_body);


                Toast toast = Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_note_body_showing),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            return true;
        }

        return false;
    }


    protected void saveChanges() {
        Intent intent = new Intent();

        intent.putExtra(NOTE_TITLE, titleEdit.getText().toString());
        intent.putExtra(NOTE_BODY, bodyEdit.getText().toString());
        intent.putExtra(NOTE_COLOUR, colour);
        intent.putExtra(NOTE_FONT_SIZE, fontSize);
        intent.putExtra(NOTE_HIDE_BODY, hideBody);

        setResult(RESULT_OK, intent);

        imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

        finish();
        overridePendingTransition(0, 0);
    }


    @Override
    public void onBackPressed() {

        if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST)
            saveChangesDialog.show();


        else {

            if (!isEmpty(titleEdit)) {
                if (!(titleEdit.getText().toString().equals(bundle.getString(NOTE_TITLE))) ||
                    !(bodyEdit.getText().toString().equals(bundle.getString(NOTE_BODY))) ||
                    !(colour.equals(bundle.getString(NOTE_COLOUR))) ||
                    fontSize != bundle.getInt(NOTE_FONT_SIZE) ||
                    hideBody != bundle.getBoolean(NOTE_HIDE_BODY)) {

                    saveChanges();
                }

                else {
                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                    finish();
                    overridePendingTransition(0, 0);
                }
            }

            else
                toastEditTextCannotBeEmpty();
        }
    }

    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    protected void toastEditTextCannotBeEmpty() {
        Toast toast = Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.toast_edittext_cannot_be_empty),
                Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            if (imm != null && titleEdit != null)
                imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (colorPickerDialog != null && colorPickerDialog.isDialogShowing())
            colorPickerDialog.dismiss();

        if (fontDialog != null && fontDialog.isShowing())
            fontDialog.dismiss();

        if (saveChangesDialog != null && saveChangesDialog.isShowing())
            saveChangesDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }
}
