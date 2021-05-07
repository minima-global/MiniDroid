package global.org.minima.intro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import global.org.minima.R;

public class IntroductionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intromain);

        //Get the viewpager
        ViewPager pager = (ViewPager)findViewById(R.id.intro_viewpager);
        pager.setAdapter(new IntroPageAdapter(this));

//        TextView maintext = (TextView)findViewById(R.id.intro_text1);
//        maintext.setText("The Complete Blockchain Solution\n\n" +
//                "Let's set your phone up..");
//
//        Button next = (Button)findViewById(R.id.intro1_next);
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent next = new Intent(IntroductionActivity.this, Introduction2Activity.class);
//                startActivity(next);
//            }
//        });

    }

}
