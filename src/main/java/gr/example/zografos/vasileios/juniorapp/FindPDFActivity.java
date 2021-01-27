package gr.example.zografos.vasileios.juniorapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class FindPDFActivity extends AppCompatActivity {

    static final String URL_STR = "https://3nt-demo-backend.azurewebsites.net/Access/Books";
    static final String PREF = "LoginPref" ;
    SharedPreferences sharedpreferences;
    AtomicReference<FindPDFActivity> me;
    AtomicBoolean isDone = new AtomicBoolean(false);
    AtomicReferenceArray<Boolean> downloadsDone;
    AtomicReferenceArray<String> response;
    public static LinearLayout realList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pdf);

        getSupportActionBar().hide();

        sharedpreferences = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        final String token = sharedpreferences.getString("token", "");

        me = new AtomicReference<>(this);

        final JSONArray[] books = new JSONArray[1];

        realList = findViewById(R.id.realList2);

        Thread t = new Thread() {

            @Override
            public void run() {

                Looper.prepare();

                // fetch user's downloaded pdfs
                try {
                    //prepare post params and connection
                    URL url = new URL(URL_STR);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setReadTimeout(10000 /* milliseconds */ );
                    conn.setConnectTimeout(15000 /* milliseconds */ );
                    conn.setRequestProperty("Authorization", "Bearer "+token);
                    conn.connect();

                    int statusCode = conn.getResponseCode();

                    if (statusCode == 200) {

                        // get the server's response
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        String lines = "";
                        while ((line = br.readLine()) != null) {
                            lines += (line + "\n");
                        }
                        br.close();

                        books[0] = new JSONArray(lines);
                        conn.disconnect();

                        isDone.set(true);
                    } else {
                        Toast.makeText(me.get(), "GET Request FAILED with code: " + statusCode, Toast.LENGTH_SHORT).show();
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

        t.start();

        try {
            while (!isDone.get()) {/* do nothing */}

            if (books[0].length() > 0) {
                // create a list of text views with a btn for download and pdf's characteristics
                downloadsDone = new AtomicReferenceArray<Boolean>(books[0].length());
                response = new AtomicReferenceArray<String>(books[0].length());

                for (int i = 0; i < books[0].length(); i++) {
                    TextView pdf = new TextView(me.get());
                    final JSONObject book = new JSONObject(books[0].get(i).toString());
                    downloadsDone.set(i, false);
                    response.set(i, "");

                    String infoTxt = "Title:\n"+"\""+book.getString("title")+"\""+"\n";
                    infoTxt += ("Release Date: "+"\""+book.getString("date_released")+"\"");
                    pdf.setText(infoTxt);
                    pdf.setTextSize(24.0f);

                    Button btn = new Button(me.get());
                    btn.setTextSize(24.0f);
                    btn.setAllCaps(false);
                    btn.setText("Download");
                    btn.setBackgroundColor(Color.parseColor("#ff0099cc"));
                    btn.setTextColor(Color.WHITE);
                    final int finalI = i;
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View b) {
                            Toast.makeText(me.get(), "Downloading...", Toast.LENGTH_SHORT).show();

                            Thread t = new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        Looper.prepare();
                                        FindPDFActivity.this.downloadPDF(book.getString("pdf_url"), finalI);
                                        Looper.loop();
                                    } catch (JSONException e) {
                                        e.getStackTrace();
                                    }
                                }
                            };

                            t.start();

                            Thread t2 = new Thread() {
                                @Override
                                public void run() {
                                    while (!downloadsDone.get(finalI)) {/* wait */}

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(me.get(), "Download completed!", Toast.LENGTH_SHORT).show();

                                            File sdcard = Environment.getExternalStorageDirectory();
                                            String uniqueId = UUID.randomUUID().toString();
                                            File file = new File(sdcard + "/xyz/download/", uniqueId);

                                            MimeTypeMap map = MimeTypeMap.getSingleton();
                                            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                                            String mimetype = map.getMimeTypeFromExtension(ext);

                                            if (mimetype == null)
                                                mimetype = "*/*";

                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            Uri data = Uri.fromFile(file);

                                            intent.setDataAndType(data, mimetype);

                                            startActivity(intent);
                                        }
                                    });
                                }
                            };

                            t2.start();
                        }
                    });

                    realList.addView(pdf);
                    realList.addView(btn);

                    if (i != books[0].length() - 1) {
                        TextView space = new TextView(me.get());
                        space.setTextSize(24.0f);
                        realList.addView(space);
                    }
                }
            } else {
                TextView view = new TextView(me.get());
                view.setTextSize(24.0f);
                view.setText("No results.");
                realList.addView(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void downloadPDF(final String url, int i) {

        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(10000 /* milliseconds */ );
            conn.setConnectTimeout(15000 /* milliseconds */ );
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();

            // Get the server response
            int statusCode = conn.getResponseCode();

            if (statusCode == 200) {

                // get the server's response
                // get the server's response
                BufferedReader br = new BufferedReader(new InputStreamReader(urlObj.openStream()));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                String resp = br.toString();
                conn.disconnect();

                downloadsDone.set(i, true);
                response.set(i, resp);
                // open pdf
                // store pdf
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
