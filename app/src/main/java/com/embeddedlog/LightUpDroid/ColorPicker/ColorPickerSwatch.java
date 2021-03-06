package com.embeddedlog.LightUpDroid.ColorPicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.embeddedlog.LightUpDroid.R;


public class ColorPickerSwatch extends FrameLayout implements View.OnClickListener {
    private int mColor;
    private ImageView mSwatchImage;
    private ImageView mCheckmarkImage;
    private OnColorSelectedListener mOnColorSelectedListener;


    public interface OnColorSelectedListener {
        public void onColorSelected(int color);
    }

    public ColorPickerSwatch(Context context, int color, boolean checked,
                             OnColorSelectedListener listener) {

        super(context);
        mColor = color;
        mOnColorSelectedListener = listener;

        LayoutInflater.from(context).inflate(R.layout.color_picker_swatch, this);
        mSwatchImage = (ImageView) findViewById(R.id.color_picker_swatch);
        mCheckmarkImage = (ImageView) findViewById(R.id.color_picker_checkmark);
        setColor(color);
        setChecked(checked);
        setOnClickListener(this);
    }


    protected void setColor(int color) {
        Drawable[] colorDrawable = new Drawable[]
                {getContext().getResources().getDrawable(R.drawable.color_picker_swatch)};

        mSwatchImage.setImageDrawable(new ColorStateDrawable(colorDrawable, color));
    }

    private void setChecked(boolean checked) {
        if (checked) {
            mCheckmarkImage.setVisibility(View.VISIBLE);
        } else {
            mCheckmarkImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (mOnColorSelectedListener != null)
            mOnColorSelectedListener.onColorSelected(mColor);
    }
}
