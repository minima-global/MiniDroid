package global.org.minima;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jraska.console.Console;
import com.prof.rssparser.Article;
import com.prof.rssparser.Parser;

import org.minima.system.params.GlobalParams;
import org.minima.utils.MinimaLogger;

import java.util.ArrayList;
import java.util.Date;

import global.org.minima.news.NewsAdapter;
import global.org.minima.news.NewsModel;

public class MainViewAdapter extends PagerAdapter{

    private MinimaActivity mMinimaActivity;

    EditText mConsoleInput;

    EditText mICInput;
    TextView mICData;
    TextView mICPing;

    //The IC User
    String mICUser = "";

    NewsAdapter mNewsAdapter;

    public MainViewAdapter(MinimaActivity zContext){
        mMinimaActivity = zContext;

        mNewsAdapter = new NewsAdapter(mMinimaActivity);
        mNewsAdapter.add(new NewsModel("","Loading..","Please wait..", new Date(),""));

        //Do we do the intro..
        SharedPreferences pref = mMinimaActivity.getApplicationContext().getSharedPreferences("MinimaPref", 0); // 0 - for private mode

        //Get the IC USer
        mICUser = pref.getString("icuser","");
    }

    public void updateICData(final String ICRewards, final String ICPing){

        Runnable update = new Runnable() {
            @Override
            public void run() {
                mICData.setText(ICRewards);
                mICPing.setText(ICPing);
            }
        };

        mMinimaActivity.runOnUiThread(update);
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
                    NewsModel cm = new NewsModel(art.getImage(),art.getTitle(), art.getContent(), art.getPubDate(), art.getLink());
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

//            //Get the button
//            Button status = (Button)layout.findViewById(R.id.minima_status);
//            status.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//
//                    //Get some data..
//                    String statustext = mMinimaActivity.runCommandSync("status");
//
//                    new AlertDialog.Builder(mMinimaActivity)
//                            .setTitle("Minima Status")
//                            .setMessage(statustext)
//                            .setIcon(R.drawable.ic_minima_new)
//                            .show();
//                }
//            });

        }else if(position == 2) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_newsfeed, collection, false);

            ListView newsfeed = (ListView)layout;
            newsfeed.setAdapter(mNewsAdapter);

            newsfeed.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int zItemPosition, long l) {
                    NewsModel nm = mNewsAdapter.getItem(zItemPosition);

                    //Open the link in a browser
                    if(!nm.getLink().equals("")){
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nm.getLink()));
                        mMinimaActivity.startActivity(browserIntent);
                    }
                }
            });

        }else if(position == 1) {
            layout = (ViewGroup) inflater.inflate(R.layout.view_ic_new, collection, false);

            //Get the edit text
            mICInput = (EditText)layout.findViewById(R.id.ic_input);
            mICInput.setText(mICUser);

            //Get the output window..
            mICData = (TextView)layout.findViewById(R.id.ic_data);
            mICPing = (TextView)layout.findViewById(R.id.ic_lastping);

            //And now run a Minima Command..
            if(!mICUser.equals("")) {
                mMinimaActivity.runICCommand("incentivecash uid:" + mICUser);
            }else{
                mICData.setText("Please set you Incentive Cash Node ID");
            }

            //Get the button..
            Button update = layout.findViewById(R.id.ic_update);
            update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Hide keyboard
                    View icview = mMinimaActivity.getCurrentFocus();
                    if (icview != null) {
                        InputMethodManager imm = (InputMethodManager) mMinimaActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(icview.getWindowToken(), 0);
                    }

                    //Get the text
                    mICUser = mICInput.getText().toString().trim();

                    //Store it..
                    SharedPreferences pref = mMinimaActivity.getApplicationContext().getSharedPreferences("MinimaPref", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("icuser",mICUser);
                    editor.commit();

                    //And now run a Minima Command..
                    mMinimaActivity.runICCommand("incentivecash uid:"+mICUser);

                    //Small message
                    Toast.makeText(mMinimaActivity,"IC Details Updated", Toast.LENGTH_SHORT).show();
                }
            });

        }else if(position == 3) {
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
        return 4;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
