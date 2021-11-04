package global.org.minima.news;

import java.util.Date;

public class NewsModel {

    public  String  mImageURL;
    public  String  mTitle;
    public  String  mDescrition;
    public  Date    mDate;

    public NewsModel(String zImage, String zTitle, String zDescription, Date zDate){
        mImageURL   = zImage;
        mTitle      = zTitle;
        mDescrition = zDescription;
        mDate       = zDate;
    }

    public String getImageID(){
        return mImageURL;
    }

    public String getTitle(){
        return mTitle;
    }

    public String getDescription(){
        return mDescrition;
    }

    public Date getDate(){
        return mDate;
    }
}
