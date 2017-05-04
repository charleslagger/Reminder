package com.embeddedlog.LightUpDroid;

import android.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class DeskClockFragment extends Fragment {

    public void onPageChanged(int page) {

    }

    public void setupFakeOverflowMenuButton(View menuButton) {
        final PopupMenu fakeOverflow = new PopupMenu(menuButton.getContext(), menuButton) {
            @Override
            public void show() {
                getActivity().onPrepareOptionsMenu(getMenu());
                super.show();
            }
        };
        fakeOverflow.inflate(R.menu.desk_clock_menu);
        fakeOverflow.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return getActivity().onOptionsItemSelected(item);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeOverflow.show();
            }
        });
    }
}
