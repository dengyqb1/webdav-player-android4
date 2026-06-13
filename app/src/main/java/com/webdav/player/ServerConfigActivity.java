package com.webdav.player;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ServerConfigActivity extends Activity {

    private EditText urlInput;
    private EditText usernameInput;
    private EditText passwordInput;

    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_config);

        config = new Config(this);

        urlInput = (EditText) findViewById(R.id.serverUrlInput);
        usernameInput = (EditText) findViewById(R.id.usernameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);

        // Restore saved values
        urlInput.setText(config.getServerUrl());
        usernameInput.setText(config.getUsername());
        passwordInput.setText(config.getPassword());

        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndConnect();
            }
        });

        findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void saveAndConnect() {
        String url = urlInput.getText().toString().trim();
        String user = usernameInput.getText().toString().trim();
        String pass = passwordInput.getText().toString();

        if (url.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure URL ends with /
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        config.setServerUrl(url);
        config.setUsername(user);
        config.setPassword(pass);

        new TestConnectionTask().execute();
    }

    private class TestConnectionTask extends AsyncTask<Void, Void, Boolean> {
        private Exception error;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                DavClient client = new DavClient(
                        config.getServerUrl(),
                        config.getUsername(),
                        config.getPassword());
                client.list("/");
                return true;
            } catch (Exception e) {
                error = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(ServerConfigActivity.this,
                        "连接失败: " + (error != null ? error.getMessage() : "未知错误"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
