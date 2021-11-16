package global.org.minima.news;

import java.util.Date;

public class NewsModel {

    public  String  mImageURL;
    public  String  mTitle;
    public  String  mDescrition;
    public  Date    mDate;
    public String   mLink;

    public NewsModel(String zImage, String zTitle, String zDescription, Date zDate, String zLink){
        mImageURL   = zImage;
        mTitle      = zTitle;
        mDescrition = zDescription;
        mDate       = zDate;
        mLink       = zLink;
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

    public String getLink(){
        return mLink;
    }
}
