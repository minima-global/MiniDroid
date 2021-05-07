package global.org.minima.intro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import global.org.minima.R;

public class Introduction2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);

        TextView maintext = (TextView)findViewById(R.id.intro_text1);
        maintext.setText("page 2");



    }

}
