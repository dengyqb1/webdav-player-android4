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
    private EditText portInput;
    private EditText usernameInput;
    private EditText passwordInput;

    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_config);

        config = new Config(this);

        urlInput = (EditText) findViewById(R.id.serverUrlInput);
        portInput = (EditText) findViewById(R.id.portInput);
        usernameInput = (EditText) findViewById(R.id.usernameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);

        // Restore saved values
        urlInput.setText(config.getServerUrl());
        int savedPort = config.getPort();
        if (savedPort > 0) {
            portInput.setText(String.valueOf(savedPort));
        }
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
        String rawUrl = urlInput.getText().toString().trim();
        String portStr = portInput.getText().toString().trim();
        String user = usernameInput.getText().toString().trim();
        String pass = passwordInput.getText().toString();

        if (rawUrl.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate and normalize URL
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://" + rawUrl;
        }

        // Basic URL validation
        int protoEnd = rawUrl.indexOf("://");
        if (protoEnd < 0 || rawUrl.indexOf("://") + 3 >= rawUrl.length()) {
            Toast.makeText(this, "服务器地址格式错误，请输入完整的URL", Toast.LENGTH_LONG).show();
            return;
        }

        int port = 0;
        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
                if (port <= 0 || port > 65535) {
                    Toast.makeText(this, "端口必须在 1-65535 之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "端口格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        config.setServerUrl(rawUrl);
        config.setPort(port);
        config.setUsername(user);
        config.setPassword(pass);

        new TestConnectionTask().execute();
    }

    private class TestConnectionTask extends AsyncTask<Void, Void, Boolean> {
        private Exception error;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String fullUrl = config.getFullUrl();
                if (fullUrl == null || fullUrl.isEmpty()) {
                    error = new Exception("服务器地址未设置");
                    return false;
                }

                DavClient client = new DavClient(
                        fullUrl,
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
                String msg = error != null ? error.getMessage() : "未知错误";
                // Make error messages more user-friendly
                if (msg.contains("No address associated")) {
                    msg = "无法连接服务器，请检查地址是否正确";
                } else if (msg.contains("Connection refused")) {
                    msg = "连接被拒绝，请检查端口是否正确";
                } else if (msg.contains("connect timed out")) {
                    msg = "连接超时，请检查网络或服务器地址";
                } else if (msg.contains("protocol not found")) {
                    msg = "地址格式错误，请确保以 http:// 或 https:// 开头";
                }
                Toast.makeText(ServerConfigActivity.this,
                        "连接失败: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}