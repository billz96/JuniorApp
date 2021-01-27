package gr.example.zografos.vasileios.juniorapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.io.IOException;
import org.json.JSONException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static final String URL_STR = "https://3nt-demo-backend.azurewebsites.net/Access/Login";
    static final String PREF = "LoginPref" ;
    SharedPreferences sharedpreferences;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        sharedpreferences = getSharedPreferences(PREF, Context.MODE_PRIVATE);

        btn = findViewById(R.id.loginBtn);
        btn.setClickable(true);
        btn.setEnabled(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Test credentials: Username: TH1234 Password: 3NItas1!

                TextView uname = MainActivity.this.findViewById(R.id.unameTxt);
                TextView pwd = MainActivity.this.findViewById(R.id.pwdTxt);

                // validate credentials
                String result = MainActivity.this.validateInput(uname.getText().toString(), pwd.getText().toString());
                if (result.equals("")) {
                    MainActivity.this.authenticateUser(uname.getText().toString(), pwd.getText().toString());
                } else {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    String checkUsername(String uname) {
        // Regex to check valid username.
        String regex = "^[A-Za-z]\\w{5,29}$";

        // Compile the ReGex
        Pattern pattern = Pattern.compile(regex);

        // Pattern class contains matcher() method
        // to find matching between given username
        // and regular expression.
        Matcher m = pattern.matcher(uname);

        // Return if the password
        // matched the ReGex
        if (m.matches()) {
            return "";
        } else {
            return "The username consists of 6 to 30 characters inclusive,\n" +
                    "can only contain alphanumeric characters and underscores,\n" +
                    "the first character of the username must be an alphabetic character, i.e., either lowercase character\n" +
                    "[a – z] or uppercase character [A – Z]";
        }
    }

    String checkPassword(String pwd) {
        // Regex to check valid password.
        String regex = "^(?=.*[0-9])"
                + "(?=.*[a-z])(?=.*[A-Z])"
                + "(?=.*[@#$%^&+=!])"
                + "(?=\\S+$).{8,20}$";

        // Compile the ReGex
        Pattern pattern = Pattern.compile(regex);

        // Pattern class contains matcher() method
        // to find matching between given password
        // and regular expression.
        Matcher m = pattern.matcher(pwd);

        // Return if the password
        // matched the ReGex
        if (m.matches()) {
            return "";
        } else {
            return "Password contains at least 8 characters and at most 20 characters,\n" +
                    "at least one digit,\n" +
                    "at least one upper case alphabet,\n" +
                    "at least one lower case alphabet,\n" +
                    "at least one special character which includes !@#$%&*()-+=^!\n" +
                    "and doesn’t contain any white space\n";
        }
    }

    String validateInput(String uname, String pwd) {
        String message = "";
        String result;

        if (uname.length() == 0 || pwd.length() == 0)
            return "Username and password are required";

        result = checkUsername(uname);
        if (result.length() > 0)
            message += result;

        result = checkPassword(pwd);
        if (result.length() > 0)
            message += result;

        return message;
    }

    void authenticateUser(final String uname, final String pwd) {

        Thread thread = new Thread() {

            public void run() {
                Looper.prepare();

                try {
                    //prepare post params and connection
                    URL url = new URL(URL_STR);
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");

                    JSONObject jsonParams = new JSONObject();
                    jsonParams.put("UserName", uname);
                    jsonParams.put("Password", pwd);

                    OutputStream outStream = conn.getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outStream));
                    bufferedWriter.write(jsonParams.toString());
                    bufferedWriter.flush();

                    int statusCode = conn.getResponseCode();

                    InputStream inputStream = conn.getInputStream();

                    if (statusCode == 200) {

                        // get the server's response
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        int data = inputStreamReader.read();
                        String result = "";
                        while (data != -1) {
                            char current = (char) data;
                            result += current;
                            data = inputStreamReader.read();
                        }

                        JSONObject jsonResp = new JSONObject(result);

                        conn.disconnect();

                        // save username and token
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString("uname", uname);
                        editor.putString("token", jsonResp.getString("access_token"));
                        editor.commit();

                        // go to Menu page
                        Intent myIntent = new Intent(MainActivity.this, MenuActivity.class);
                        MainActivity.this.startActivity(myIntent);
                    } else {
                        conn.disconnect();
                        Toast.makeText(MainActivity.this, "POST request failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Looper.loop();
            }
        };

        thread.start();
    }
}
