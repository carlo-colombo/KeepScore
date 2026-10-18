package ovh.litapp.keepscore.android;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Minimal AppCompat replacement for the framework's {@code ListActivity}, which
 * has no AppCompat equivalent. It mirrors the {@code ListActivity} adapter and
 * list-view contract ({@code setListAdapter()}, {@code getListView()},
 * {@code onListItemClick()}) so the legacy activities keep working after the
 * ActionBarSherlock removal.
 */
public abstract class AppCompatListActivity extends AppCompatActivity {

    private ListAdapter listAdapter;
    private ListView listView;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View empty = findViewById(android.R.id.empty);
        listView = findViewById(android.R.id.list);
        if (listView == null) {
            throw new RuntimeException(
                    "Your content must have a ListView whose id attribute is 'android.R.id.list'");
        }
        if (empty != null) {
            listView.setEmptyView(empty);
        }
        listView.setOnItemClickListener((parent, view, position, id) ->
                onListItemClick((ListView) parent, view, position, id));
        if (listAdapter != null) {
            listView.setAdapter(listAdapter);
        }
    }

    public void setListAdapter(ListAdapter adapter) {
        listAdapter = adapter;
        if (listView != null) {
            listView.setAdapter(adapter);
        }
    }

    public ListAdapter getListAdapter() {
        return listAdapter;
    }

    public ListView getListView() {
        return listView;
    }

    /**
     * @see android.app.ListActivity#onListItemClick(ListView, View, int, long)
     */
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // no-op by default, matching ListActivity
    }
}
