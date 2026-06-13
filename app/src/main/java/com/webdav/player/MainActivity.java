package com.webdav.player;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends ListActivity {

    private static final int REQUEST_CONFIG = 1;

    private Config config;
    private DavClient davClient;
    private FileAdapter adapter;
    private ProgressBar progressBar;
    private String currentPath = "/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = new Config(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        adapter = new FileAdapter(this);
        setListAdapter(adapter);

        if (!config.isConfigured()) {
            startActivityForResult(
                    new Intent(this, ServerConfigActivity.class), REQUEST_CONFIG);
        } else {
            connectAndList();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIG && resultCode == RESULT_OK) {
            connectAndList();
        } else if (!config.isConfigured()) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivityForResult(
                    new Intent(this, ServerConfigActivity.class), REQUEST_CONFIG);
            return true;
        } else if (item.getItemId() == R.id.menu_parent) {
            navigateUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        FileItem item = adapter.getItem(position);
        if (item.isDir) {
            currentPath = item.path;
            connectAndList();
        } else if (MediaUtils.isMedia(item.name)) {
            String url = davClient.getDownloadUrl(item.path);
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_URL, url);
            intent.putExtra(PlayerActivity.EXTRA_TITLE, item.name);
            intent.putExtra(PlayerActivity.EXTRA_IS_VIDEO, MediaUtils.isVideo(item.name));
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentPath.equals("/")) {
            super.onBackPressed();
        } else {
            navigateUp();
        }
    }

    private void navigateUp() {
        if (currentPath.equals("/")) return;
        int last = currentPath.lastIndexOf('/');
        if (last <= 0) {
            currentPath = "/";
        } else {
            currentPath = currentPath.substring(0, last);
            if (!currentPath.startsWith("/")) currentPath = "/" + currentPath;
        }
        connectAndList();
    }

    private void connectAndList() {
        String url = config.getServerUrl();
        String user = config.getUsername();
        String pass = config.getPassword();
        davClient = new DavClient(url, user, pass);
        new ListFilesTask().execute(currentPath);
    }

    private class ListFilesTask extends AsyncTask<String, Void, List<FileItem>> {
        private Exception error;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<FileItem> doInBackground(String... params) {
            try {
                String path = params[0];
                List<DavClient.DavEntry> entries = davClient.list(path);
                List<FileItem> items = new ArrayList<FileItem>();
                for (DavClient.DavEntry e : entries) {
                    if (e.name == null || e.name.isEmpty()) continue;
                    if (e.path.equals(path) || e.path.equals(currentPath)) continue;
                    items.add(new FileItem(e.name, e.path, e.isDir, e.size));
                }
                Collections.sort(items, new Comparator<FileItem>() {
                    @Override
                    public int compare(FileItem a, FileItem b) {
                        if (a.isDir && !b.isDir) return -1;
                        if (!a.isDir && b.isDir) return 1;
                        return a.name.compareToIgnoreCase(b.name);
                    }
                });
                return items;
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<FileItem> items) {
            progressBar.setVisibility(View.GONE);
            if (items == null) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_connect) + ": " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            adapter.setItems(items);
        }
    }

    static class FileItem {
        String name;
        String path;
        boolean isDir;
        long size;

        FileItem(String name, String path, boolean isDir, long size) {
            this.name = name;
            this.path = path;
            this.isDir = isDir;
            this.size = size;
        }
    }

    static class FileAdapter extends BaseAdapter {

        private final Context context;
        private final LayoutInflater inflater;
        private List<FileItem> items = new ArrayList<FileItem>();

        FileAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        void setItems(List<FileItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public FileItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_file, parent, false);
            }
            FileItem item = items.get(position);
            TextView nameView = (TextView) convertView.findViewById(R.id.fileName);
            TextView infoView = (TextView) convertView.findViewById(R.id.fileInfo);

            nameView.setText(item.name);
            if (item.isDir) {
                infoView.setText("📁 文件夹");
                nameView.setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.ic_menu_view, 0, 0, 0);
            } else {
                infoView.setText(MediaUtils.formatSize(item.size));
                int icon = MediaUtils.isVideo(item.name) || MediaUtils.isAudio(item.name)
                        ? android.R.drawable.ic_media_play
                        : android.R.drawable.ic_menu_info_details;
                nameView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
            }
            return convertView;
        }
    }
}
