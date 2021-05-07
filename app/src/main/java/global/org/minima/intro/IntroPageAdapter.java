package global.org.minima.intro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import global.org.minima.R;

public class IntroPageAdapter extends PagerAdapter {

    private IntroductionActivity mContext;

    public IntroPageAdapter(IntroductionActivity context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = null;
        if(position == 0){
            layout = (ViewGroup) inflater.inflate(R.layout.activity_intro, collection, false);

        }else if(position == 1){
            layout = (ViewGroup) inflater.inflate(R.layout.activity_intro2, collection, false);

            Button next = (Button)layout.findViewById(R.id.intro2_battery);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    String packageName = mContext.getPackageName();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    mContext.startActivity(intent);
                }
            });

        }else if(position == 2){
            layout = (ViewGroup) inflater.inflate(R.layout.activity_intro3, collection, false);

        }else if(position == 3){
            layout = (ViewGroup) inflater.inflate(R.layout.activity_intro4, collection, false);

            Button start = (Button)layout.findViewById(R.id.intro4_start);
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mContext.finish();
                }
            });
        }

        //Add it to the viewgroup
        collection.addView(layout);

        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }


    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
