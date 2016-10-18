/*
* This java file handles the intro screen with the logos. It also creates the necessary folders
* on the phone. It also starts the select class and have a small delay, giving time for the user
* to read the logos
*/
package stream.sics.streamdas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File(getExternalFilesDir(null) + File.separator + "logs");


        if (!file.exists()) {
            file.mkdirs();
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(MainActivity.this, Select.class);
                startActivity(intent);
                finish();
            }
        }, 2500); // This is the number of ms to display the logos
    }
}
