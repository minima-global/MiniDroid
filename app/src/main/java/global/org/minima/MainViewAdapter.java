package global.org.minima;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jraska.console.Console;

public class MainViewAdapter extends PagerAdapter{

    private Activity mContext;

    EditText mConsoleInput;

    public MainViewAdapter(Activity zContext){
        mContext = zContext;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = null;

        System.out.println("Position: "+position);

        if(position == 0) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_minima, collection, false);

        }else if(position == 1) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_newsfeed, collection, false);

        }else if(position == 2) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_terminal, collection, false);

            mConsoleInput = layout.findViewById(R.id.console_input);
            mConsoleInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    String text = mConsoleInput.getText().toString();
                    Console.writeLine(text);
                    mConsoleInput.setText("");

                    // Check if no view has focus:
                    View view = mContext.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    return false;
                }
            });

            //Don't give up focus
            mConsoleInput.setNextFocusDownId(mConsoleInput.getId());
        }

        //Add it to the viewgroup
        collection.addView(layout);

        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
