package comp5216.sydney.edu.au.mediarecording.adapter;


import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

import comp5216.sydney.edu.au.mediarecording.R;
import comp5216.sydney.edu.au.mediarecording.entity.MediaInfo;

public class MediaAdapter extends BaseAdapter {

    //Define the variables
    private Activity mActivity;

    private List<MediaInfo> mediaInfoList;

    public MediaAdapter(Activity activity, List<MediaInfo> mediaInfoList) {
        this.mActivity = activity;
        this.mediaInfoList = mediaInfoList;
    }

    @Override
    public int getCount() {
        return mediaInfoList.size();
    }

    @Override
    public Object getItem(int i) {
        return mediaInfoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mediaInfoList.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View itemView = mActivity.getLayoutInflater().inflate(R.layout.media_item, null);

        MediaInfo mediaInfo = mediaInfoList.get(i);
        ImageView imageView = itemView.findViewById(R.id.imageview);
        TextView itemCity = itemView.findViewById(R.id.city);
        TextView file_name = itemView.findViewById(R.id.file_name);
        TextView itemStatus = itemView.findViewById(R.id.status);

        //Determine the  photo and the video
        if (mediaInfo.getType() == 1) {
            imageView.setImageResource(R.drawable.photo);
        } else {
            imageView.setImageResource(R.drawable.video);
        }

        itemCity.setText(mediaInfo.getCity());
        itemStatus.setText(mediaInfo.getSyncStatus() + "%");

        String filename = mediaInfo.getUrl().split("/")[mediaInfo.getUrl().split("/").length - 1];
        file_name.setText(filename);

        return itemView;
    }
}
