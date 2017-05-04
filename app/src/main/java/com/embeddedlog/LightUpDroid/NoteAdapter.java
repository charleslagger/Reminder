package com.embeddedlog.LightUpDroid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_BODY;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_COLOUR;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_FAVOURED;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_FONT_SIZE;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_HIDE_BODY;
import static com.embeddedlog.LightUpDroid.DataUtils.NOTE_TITLE;
import static com.embeddedlog.LightUpDroid.MainActivity.checkedArray;
import static com.embeddedlog.LightUpDroid.MainActivity.deleteActive;
import static com.embeddedlog.LightUpDroid.MainActivity.searchActive;
import static com.embeddedlog.LightUpDroid.MainActivity.setFavourite;

public class NoteAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    public NoteAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }


    @Override
    public JSONObject getItem(int position) {
        if (this.adapterData != null)
            return this.adapterData.optJSONObject(position);

        else
            return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_note, parent, false);

        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);


        JSONObject noteObject = getItem(position);

        if (noteObject != null) {

            String title = context.getString(R.string.note_title);
            String body = context.getString(R.string.note_body);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;

            try {
                title = noteObject.getString(NOTE_TITLE);
                body = noteObject.getString(NOTE_BODY);
                colour = noteObject.getString(NOTE_COLOUR);

                if (noteObject.has(NOTE_FONT_SIZE))
                    fontSize = noteObject.getInt(NOTE_FONT_SIZE);

                if (noteObject.has(NOTE_HIDE_BODY))
                    hideBody = noteObject.getBoolean(NOTE_HIDE_BODY);

                favoured = noteObject.getBoolean(NOTE_FAVOURED);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (favoured)
                favourite.setImageResource(R.drawable.ic_fav);

            else
                favourite.setImageResource(R.drawable.ic_unfav);


            if (searchActive || deleteActive)
                favourite.setVisibility(View.INVISIBLE);

            else
                favourite.setVisibility(View.VISIBLE);


            titleView.setText(title);

            if (hideBody)
                bodyView.setVisibility(View.GONE);

            else {
                bodyView.setVisibility(View.VISIBLE);
                bodyView.setText(body);
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            }

            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            relativeLayout.setBackground(roundedCard);

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that note to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
