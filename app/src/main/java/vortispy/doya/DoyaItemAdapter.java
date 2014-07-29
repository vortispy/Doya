package vortispy.doya;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.Jedis;

/**
 * Created by vortispy on 2014/07/19.
 */
public class DoyaItemAdapter extends ArrayAdapter<DoyaData> {
    private LayoutInflater inflater;
    private AmazonS3Client s3Client;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    final String LOCALHOST = "10.0.2.2";
    final String JEDIS_KEY = "pictures";

    private class DoyaViewContainer{
        public ImageView imageView;
        public String bucket, prefix, key;
        public DoyaViewContainer(ImageView imageView, String bucket, String prefix, String key){
            this.imageView = imageView;
            this.bucket = bucket;
            this.prefix = prefix;
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public String getBucket() {
            return bucket;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private class DoyaTextViewContainer{
        public TextView textView;
        public String objectKey;

        public DoyaTextViewContainer(TextView textView, String objectKey){
            this.objectKey = objectKey;
            this.textView = textView;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public TextView getTextView() {
            return textView;
        }
    }

    public DoyaItemAdapter(Context context, int resource, List<DoyaData> items) {
        super(context, resource, items);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(
                        context.getString(R.string.aws_access_key),
                        context.getString(R.string.aws_secret_key))
        );

        this.requestQueue = Volley.newRequestQueue(context);
        this.imageLoader = new ImageLoader(requestQueue, new BitmapCache());
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final DoyaData item = (DoyaData) getItem(position);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.item_container, null);
        }

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.doyaImage);

        final DoyaViewContainer doyaViewContainer = new DoyaViewContainer(
                imageView,
                getContext().getString(R.string.s3_bucket).toLowerCase(Locale.US),
                getContext().getString(R.string.s3_bucket_prefix).toLowerCase(Locale.US),
                item.getObjectKey()
                );
        new S3GetImage().execute(doyaViewContainer);
        /*
        imageLoader.get(item.url, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d("debug", "Volley Error");

            }

            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                if (imageContainer.getBitmap() != null) {
                    imageView.setImageBitmap(imageContainer.getBitmap());
                }
            }
        });
        */

        final TextView textView = (TextView) convertView.findViewById(R.id.doyaPoint);
//        textView.setText(item.getDoyaPoint().toString());
        final DoyaTextViewContainer doyaTextViewContainer = new DoyaTextViewContainer(
                textView,
                item.getObjectKey()
        );
        new JedisGetPoint().execute(doyaTextViewContainer);

        Button plusOne = (Button) convertView.findViewById(R.id.plus_button);
        Button minusOne = (Button) convertView.findViewById(R.id.minus_button);

        plusOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new JedisPlusOne().execute(doyaTextViewContainer);
            }
        });

        minusOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new JedisMinusOne().execute(doyaTextViewContainer);
            }
        });


        return convertView;
    }

    public class BitmapCache implements ImageLoader.ImageCache {

        private LruCache<String, Bitmap> mCache;

        public BitmapCache() {
            int maxSize = 10 * 1024 * 1024;
            mCache = new LruCache<String, Bitmap>(maxSize) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getRowBytes() * value.getHeight();
                }
            };
        }

        @Override
        public Bitmap getBitmap(String url) {
            return mCache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            mCache.put(url, bitmap);
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

    private class S3GetImage extends AsyncTask<DoyaViewContainer, Void, S3TaskResult>{

        @Override
        protected S3TaskResult doInBackground(DoyaViewContainer... doyaViewContainers) {
            S3TaskResult result = new S3TaskResult();
            DoyaViewContainer container = doyaViewContainers[0];
            String bucket = container.getBucket();
            String objectPath = container.getPrefix() + container.getKey();
            try{
                InputStream inputStream = s3Client.getObject(bucket, container.getKey()).getObjectContent();
                Bitmap img = BitmapFactory.decodeStream(inputStream);
                result.setKey(bucket + "/" + objectPath);
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
               result
                       .getDoyaViewContainer()
                       .getImageView()
                       .setImageBitmap(result.getBitmap());
            }
        }
    }

    private class JedisResult{
        TextView textView;
        DoyaTextViewContainer doyaTextViewContainer;
        Double point;
        String errorMessage = null;

        public void setPoint(Double point) {
            this.point = point;
        }

        public void setTextView(TextView textView) {
            this.textView = textView;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public void setDoyaTextViewContainer(DoyaTextViewContainer doyaTextViewContainer) {
            this.doyaTextViewContainer = doyaTextViewContainer;
        }

        public TextView getTextView() {
            return textView;
        }

        public Double getPoint() {
            return point;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public DoyaTextViewContainer getDoyaTextViewContainer() {
            return doyaTextViewContainer;
        }
    }

    private class JedisGetPoint extends AsyncTask<DoyaTextViewContainer, Void, JedisResult>{
        Jedis jedis;

        @Override
        protected void onPreExecute() {
            jedis = new Jedis(LOCALHOST);
        }

        @Override
        protected JedisResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            JedisResult jedisResult = new JedisResult();
            DoyaTextViewContainer container = doyaTextViewContainers[0];
            jedisResult.setTextView(container.getTextView());
            jedisResult.setDoyaTextViewContainer(container);
            try{
                Double point;
                point = jedis.zscore(JEDIS_KEY, container.getObjectKey());
//                point = jedis.zscore("img", "img_url1");
                if(point == null){
                    jedis.zadd(JEDIS_KEY, 0, container.getObjectKey());
                    point = jedis.zscore(JEDIS_KEY, container.getObjectKey());
                }

                jedisResult.setPoint(point);
            } catch (Exception e){
                jedisResult.setErrorMessage("Jedis Error");
            }

            return jedisResult;
        }

        @Override
        protected void onPostExecute(JedisResult jedisResult) {
            if (jedisResult.getErrorMessage() != null){
                Log.d("jedisGetPoint", jedisResult.getErrorMessage());
            } else {
                jedisResult
                        .getDoyaTextViewContainer()
                        .getTextView()
                        .setText(jedisResult.getPoint().toString());
            }
        }
    }

    private class JedisOneResult extends JedisResult{
        DoyaData doyaData;

        public void setDoyaData(DoyaData doyaData) {
            this.doyaData = doyaData;
        }

        public DoyaData getDoyaData() {
            return doyaData;
        }
    }

    private class JedisPlusOne extends AsyncTask<DoyaTextViewContainer, Void, JedisOneResult>{
        Jedis jedis;

        @Override
        protected void onPreExecute() {
            jedis = new Jedis(LOCALHOST);
        }

        @Override
        protected JedisOneResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            DoyaTextViewContainer doyaTextViewContainer = doyaTextViewContainers[0];
            JedisOneResult result = new JedisOneResult();
            DoyaData doyaData = new DoyaData();
            result.setDoyaTextViewContainer(doyaTextViewContainer);
            try{
                Integer point = jedis.zincrby(JEDIS_KEY, 1, doyaTextViewContainer.getObjectKey()).intValue();
                doyaData.setDoyaPoint(point);
                result.setDoyaData(doyaData);
            } catch (Exception e){
                result.setErrorMessage("JedisPlusOne Error");
            }
            return result;
        }

        @Override
        protected void onPostExecute(JedisOneResult jedisOneResult) {
            if (jedisOneResult.getErrorMessage() != null){
                Log.d("jedisPlus", jedisOneResult.getErrorMessage());
            } else {
                String point = jedisOneResult.getDoyaData().getDoyaPoint().toString();
                jedisOneResult
                        .getDoyaTextViewContainer()
                        .getTextView()
                        .setText(point);
            }
        }
    }

    private class JedisMinusOne extends AsyncTask<DoyaTextViewContainer, Void, JedisOneResult>{
        Jedis jedis;

        @Override
        protected void onPreExecute() {
            jedis = new Jedis(LOCALHOST);
        }

        @Override
        protected JedisOneResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            DoyaTextViewContainer doyaTextViewContainer = doyaTextViewContainers[0];
            JedisOneResult result = new JedisOneResult();
            DoyaData doyaData = new DoyaData();
            result.setDoyaTextViewContainer(doyaTextViewContainer);
            try{
                Integer point = jedis.zincrby(JEDIS_KEY, -1, doyaTextViewContainer.getObjectKey()).intValue();
                doyaData.setDoyaPoint(point);
                result.setDoyaData(doyaData);
            } catch (Exception e){
                result.setErrorMessage("JedisMinusOne Error");
            }
            return result;
        }

        @Override
        protected void onPostExecute(JedisOneResult jedisOneResult) {
            if (jedisOneResult.getErrorMessage() != null){
                Log.d("jedisMinus", jedisOneResult.getErrorMessage());
            } else {
                String point = jedisOneResult.getDoyaData().getDoyaPoint().toString();
                jedisOneResult
                        .getDoyaTextViewContainer()
                        .getTextView()
                        .setText(point);
            }
        }
    }
}
