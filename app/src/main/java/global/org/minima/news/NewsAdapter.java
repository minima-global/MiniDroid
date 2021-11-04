package global.org.minima.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import global.org.minima.R;

public class NewsAdapter extends ArrayAdapter<NewsModel> {

    public NewsAdapter(Context context) {
        super(context, R.layout.news_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.news_item, parent, false);
            holder = new ViewHolder(convertView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NewsModel model = getItem(position);

        //Use Picasso to load the image
        if(model.getImageID().equals("")){
            holder.imageView.setImageResource(R.drawable.ic_minima_new);
        }else{
            try{
                Picasso.get().load(model.getImageID()).resize(256,256).centerCrop().into(holder.imageView);
            }catch (Exception exc){

            }
        }

        holder.tvTitle.setText(model.getTitle());
        holder.tvSubtitle.setText(model.getDescription());
        holder.tvDate.setText(model.getDate().toString());

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvDate;

        ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.image);
            tvTitle = (TextView) view.findViewById(R.id.text_title);
            tvSubtitle = (TextView) view.findViewById(R.id.text_subtitle);
            tvDate = (TextView) view.findViewById(R.id.text_date);
        }
    }

}
