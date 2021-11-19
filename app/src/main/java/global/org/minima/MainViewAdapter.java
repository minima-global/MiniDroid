package global.org.minima;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jraska.console.Console;
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import org.minima.system.params.GlobalParams;

import java.util.ArrayList;
import java.util.Date;

import global.org.minima.news.NewsAdapter;
import global.org.minima.news.NewsModel;

public class MainViewAdapter extends PagerAdapter{

    private MinimaActivity mMinimaActivity;

    EditText mConsoleInput;

    NewsAdapter mNewsAdapter;

    public MainViewAdapter(MinimaActivity zContext){
        mMinimaActivity = zContext;

        mNewsAdapter = new NewsAdapter(mMinimaActivity);
        mNewsAdapter.add(new NewsModel("","Loading..","Please wait..", new Date(),""));
    }

    public void refreshRSSFeed(){
        //url of RSS feed
//        String urlString = "http://www.androidcentral.com/feed";
        String urlString = "https://medium.com/feed/@icminima";

        Parser parser = new Parser();
        parser.onFinish(new Parser.OnTaskCompleted() {
            @Override
            public void onTaskCompleted(ArrayList<Article> arrayList) {
                mNewsAdapter.clear();

                for(Article art : arrayList){
                    NewsModel cm = new NewsModel(art.getImage(),art.getTitle(), art.getDescription(), art.getPubDate(), art.getLink());
                    mNewsAdapter.add(cm);
                }

                mNewsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {
                System.out.println("ERROR rss load..!!");
            }
        });

        parser.execute(urlString);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(mMinimaActivity);
        ViewGroup layout = null;

        if(position == 0) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_minima, collection, false);

            TextView maintext = layout.findViewById(R.id.minima_maintext);
            maintext.setText("v"+GlobalParams.MINIMA_VERSION+"\n\nThe Complete Blockchain Solution\n\nFreedom");

        }else if(position == 1) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_newsfeed, collection, false);

            ListView newsfeed = (ListView)layout;
            newsfeed.setAdapter(mNewsAdapter);

            newsfeed.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    NewsModel nm = mNewsAdapter.getItem(position);

                    //Open the link in a browser
                    if(!nm.getLink().equals("")){
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nm.getLink()));
                        mMinimaActivity.startActivity(browserIntent);
                    }
                }
            });

        }else if(position == 2) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_terminal, collection, false);

            Console console = layout.findViewById(R.id.console_window);
            TextView ctv = console.findViewById(R.id.console_text);
            ctv.setTextSize(10);

            mConsoleInput = layout.findViewById(R.id.console_input);
            mConsoleInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    //Get the command and blank the field
                    String text = mConsoleInput.getText().toString();
                    mConsoleInput.setText("");

                    //Run it..
                    mMinimaActivity.runMinimaCommand(text);

//                    // Check if no view has focus:
//                    View view = mMinimaActivity.getCurrentFocus();
//                    if (view != null) {
//                        InputMethodManager imm = (InputMethodManager) mMinimaActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
//                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//                    }

                    return true;
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
