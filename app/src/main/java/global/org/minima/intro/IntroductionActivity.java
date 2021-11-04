package global.org.minima.intro;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import global.org.minima.R;

public class IntroductionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_viewpager);

        //Get the viewpager
        ViewPager pager = (ViewPager)findViewById(R.id.intro_viewpager);
        pager.setAdapter(new IntroPageAdapter(this));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(pager, true);

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
