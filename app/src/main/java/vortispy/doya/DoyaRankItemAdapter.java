package vortispy.doya;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.Jedis;

/**
 * Created by vortispy on 2014/07/25.
 */
public class DoyaRankItemAdapter extends ArrayAdapter<DoyaData> {
    private LayoutInflater inflater;

    final String LOCALHOST = "10.0.2.2";

    private String REDIS_HOST;
    private Integer REDIS_PORT;
    private String REDIS_PASSWORD;

    Integer rankImageArray[] = {
        R.drawable.rank01,
        R.drawable.rank02,
        R.drawable.rank03,
        R.drawable.rank04,
        R.drawable.rank05,
        R.drawable.rank06,
        R.drawable.rank07,
        R.drawable.rank08,
        R.drawable.rank09,
        R.drawable.rank10,
    };

    public DoyaRankItemAdapter(Context context, int resource, List<DoyaData> items) {
        super(context, resource, items);

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        REDIS_HOST = context.getString(R.string.redis_host);
        REDIS_PASSWORD = context.getString(R.string.redis_password);
        REDIS_PORT = Integer.valueOf(context.getString(R.string.redis_port));

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DoyaData item = (DoyaData) getItem(position);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.item_rank, null);
        }

        DoyaViewContainer doyaViewContainer = new DoyaViewContainer(
                convertView,
                item
        );

        ImageView rankView = (ImageView) convertView.findViewById(R.id.rankView);
        TextView rankPointView = (TextView) convertView.findViewById(R.id.rankPointView);
        Resources resources = convertView.getResources();
        Drawable drawable = resources.getDrawable(rankImageArray[item.getDoyaRank()]);

        rankView.setImageDrawable(drawable);
        rankPointView.setText(item.getDoyaPoint().toString() + "points");

        if(item.getImageData() == null) {
            new S3GetImage().execute(doyaViewContainer);
        } else{
            ImageView imageView = (ImageView) convertView.findViewById(R.id.rankImageView);
            imageView.setImageBitmap(item.getImageData());
        }

        return convertView;
    }

    private class DoyaViewContainer{
        View convertView;
        DoyaData doyaData;

        public DoyaViewContainer(View convertView, DoyaData doyaData){
            this.convertView = convertView;
            this.doyaData = doyaData;
        }

        public DoyaData getDoyaData() {
            return doyaData;
        }

        public View getConvertView() {
            return convertView;
        }

    }

    private class S3TaskResult {
        String errorMessage = null;
        Uri uri = null;
        private List<String> pictureList = new ArrayList<String>();
        String key;
        Bitmap bitmap;
        DoyaViewContainer doyaViewContainer;

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Uri getUri() {
            return uri;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }

        public List<String> getPictureList() {
            return pictureList;
        }

        public void setKey(String key){
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public void setDoyaViewContainer(DoyaViewContainer doyaViewContainer) {
            this.doyaViewContainer = doyaViewContainer;
        }

        public DoyaViewContainer getDoyaViewContainer() {
            return doyaViewContainer;
        }
    }

    private class S3GetImage extends AsyncTask<DoyaViewContainer, Void, S3TaskResult> {

        @Override
        protected S3TaskResult doInBackground(DoyaViewContainer... doyaViewContainers) {
            S3TaskResult result = new S3TaskResult();
            DoyaViewContainer container = doyaViewContainers[0];
            String objectKey = container.getDoyaData().getObjectKey();


            try{
                URL imgUrl = new URL(objectKey);
                InputStream inputStream = imgUrl.openStream();
                Log.d("debug", imgUrl.toString());

                Bitmap img = BitmapFactory.decodeStream(inputStream);
                result.setKey(objectKey);
                result.setBitmap(img);
                result.setDoyaViewContainer(container);
            } catch (Exception exception) {
                result.setErrorMessage(exception.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(S3TaskResult result) {
            if (result.getErrorMessage() != null) {
                Log.d("debug", result.getErrorMessage());
            } else {
                DoyaViewContainer container = result.getDoyaViewContainer();
                ImageView imageView = (ImageView) container
                        .getConvertView()
                        .findViewById(R.id.rankImageView);

                imageView.setImageBitmap(result.getBitmap());
                container.getDoyaData().setImageData(result.getBitmap());
            }
        }
    }

}
