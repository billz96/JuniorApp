package gr.example.zografos.vasileios.juniorapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MenuActivity extends AppCompatActivity {

    static final String PREF = "LoginPref" ;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        getSupportActionBar().hide();

        sharedpreferences = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String uname = sharedpreferences.getString("uname", "");

        // greed user
        TextView welcomeTxt = findViewById(R.id.welcomeTxt);
        welcomeTxt.setText(welcomeTxt.getText()+" "+uname+" !");

        // setup click listeners
        Button findPdfBtn = findViewById(R.id.findPdfBtn);
        findPdfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                // go to current user's pdfs page
                Intent myIntent = new Intent(MenuActivity.this, FindPDFActivity.class);
                MenuActivity.this.startActivity(myIntent);
            }
        });

        Button logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                // reset preferences
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("uname", "");
                editor.putString("token", "");
                editor.commit();

                // go to current user's download pdfs page
                Intent myIntent = new Intent(MenuActivity.this, MainActivity.class);
                MenuActivity.this.startActivity(myIntent);
            }
        });

        Button exitBtn = findViewById(R.id.exitBtn);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View b) {
                // close app
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1); // kill the app's process
            }
        });
    }
}
