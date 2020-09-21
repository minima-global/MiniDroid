package global.org.minima;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class MinimaPageAdapter extends PagerAdapter {

    private Context mContext;

    TextView mTextIP = null;
    Button btnMini   = null;
    TextView mLogs   = null;

    public MinimaPageAdapter(Context zContext){
        mContext = zContext;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        System.out.println("Minima instantiateItem "+collection+" "+position);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if(position == 0) {
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.activity_start, collection, false);
            collection.addView(layout);

            //get the Important bits...
            mTextIP = layout.findViewById(R.id.iptext_minidapp);
            mTextIP.setText("Synchronising.. please wait..");

            //The Button to open the local browser
            btnMini = layout.findViewById(R.id.btn_minidapp);

            // btn open 127.0.0.1:21000 `Minidapp`
            btnMini.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:9004/"));
                    mContext.startActivity(intent);
                }
            });
            btnMini.setVisibility(View.INVISIBLE);

            return layout;
        }

        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.activity_logs, collection, false);
        collection.addView(layout);
        mLogs = layout.findViewById(R.id.logstext);
        mLogs.setTypeface(Typeface.MONOSPACE);

        return layout;
    }

    public TextView getTextIP(){
        return mTextIP;
    }

    public TextView getTextLogs(){
        return mLogs;
    }

    public Button getStartButton(){
        return btnMini;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        System.out.println("Minima  destroyItem "+collection+" "+position);

        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
