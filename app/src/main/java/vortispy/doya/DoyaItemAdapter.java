package vortispy.doya;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;

/**
 * Created by vortispy on 2014/07/19.
 */
public class DoyaItemAdapter extends ArrayAdapter<DoyaData> {
    private LayoutInflater inflater;
    private Context context;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    final String LOCALHOST = "10.0.2.2";

    private String REDIS_HOST;
    private Integer REDIS_PORT;
    private String REDIS_PASSWORD;
    private String REDIS_SCORE_KEY;
    private String REDIS_REPORT_KEY;

    private class DoyaViewContainer{
        public ImageView imageView;
        public String key;
        public DoyaData doyaData;
        public DoyaViewContainer(ImageView imageView, String key, DoyaData doyaData){
            this.imageView = imageView;
            this.key = key;
            this.doyaData = doyaData;
        }

        public String getKey() {
            return key;
        }

        public ImageView getImageView() {
            return imageView;
        }

    }

    private class DoyaTextViewContainer{
        public TextView textView;
        public String objectKey;
        public DoyaData doyaData;

        public DoyaTextViewContainer(TextView textView, String objectKey, DoyaData doyaData){
            this.objectKey = objectKey;
            this.textView = textView;
            this.doyaData = doyaData;
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
        this.context = context;

        REDIS_HOST = context.getString(R.string.redis_host);
        REDIS_PASSWORD = context.getString(R.string.redis_password);
        REDIS_PORT = Integer.valueOf(context.getString(R.string.redis_port));
        REDIS_SCORE_KEY = context.getString(R.string.redis_score_key);
        REDIS_REPORT_KEY = context.getString(R.string.redis_report_key);


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
                item.getObjectKey(),
                item
                );
        if(item.getImageData() == null) {
            new S3GetImage().execute(doyaViewContainer);
        } else{
            imageView.setImageBitmap(item.getImageData());
        }
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
                item.getObjectKey(),
                item
        );
        if(item.getDoyaPoint() == null) {
            new JedisGetPoint().execute(doyaTextViewContainer);
        } else{
            textView.setText(item.getDoyaPoint().toString());
        }

        ImageView plusOne = (ImageView) convertView.findViewById(R.id.plus_button);
        ImageView minusOne = (ImageView) convertView.findViewById(R.id.minus_button);

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

        TextView report = (TextView) convertView.findViewById(R.id.report);

        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reportDialog(item.getObjectKey());
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
            String objectPath = container.getKey();
            try{
                URL imgURL = new URL(objectPath);
                InputStream inputStream = imgURL.openStream();
                Bitmap img = BitmapFactory.decodeStream(inputStream);
                result.setKey(objectPath);
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
                container.doyaData.setImageData(result.getBitmap());
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
        Integer point;
        String errorMessage = null;

        public void setPoint(Integer point) {
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

        public Integer getPoint() {
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
            jedis = new Jedis(REDIS_HOST, REDIS_PORT);

        }

        @Override
        protected JedisResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            JedisResult jedisResult = new JedisResult();
            DoyaTextViewContainer container = doyaTextViewContainers[0];
            jedisResult.setTextView(container.getTextView());
            jedisResult.setDoyaTextViewContainer(container);

            jedis.auth(REDIS_PASSWORD);
            try{
                Double point;
                point = jedis.zscore(REDIS_SCORE_KEY, container.getObjectKey());

                if(point == null){
                    Log.d("redis/zscore", "can not get score");
                    jedis.zadd(REDIS_SCORE_KEY, 0, container.getObjectKey());
                    point = 0.0;
                }

                jedisResult.setPoint(point.intValue());
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
                DoyaTextViewContainer container = jedisResult.getDoyaTextViewContainer();
                Integer point = jedisResult.getPoint();

                container.doyaData.setDoyaPoint(point);
                container
                        .getTextView()
                        .setText(point.toString());
            }
            jedis.close();
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
            jedis = new Jedis(REDIS_HOST, REDIS_PORT);

        }

        @Override
        protected JedisOneResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            DoyaTextViewContainer doyaTextViewContainer = doyaTextViewContainers[0];
            JedisOneResult result = new JedisOneResult();
            DoyaData doyaData = new DoyaData();
            result.setDoyaTextViewContainer(doyaTextViewContainer);

            jedis.auth(REDIS_PASSWORD);
            try{
                Integer point = jedis.zincrby(REDIS_SCORE_KEY, 1, doyaTextViewContainer.getObjectKey()).intValue();
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
            jedis.close();
        }
    }

    private class JedisMinusOne extends AsyncTask<DoyaTextViewContainer, Void, JedisOneResult>{
        Jedis jedis;

        @Override
        protected void onPreExecute() {
            jedis = new Jedis(REDIS_HOST, REDIS_PORT);

        }

        @Override
        protected JedisOneResult doInBackground(DoyaTextViewContainer... doyaTextViewContainers) {
            DoyaTextViewContainer doyaTextViewContainer = doyaTextViewContainers[0];
            JedisOneResult result = new JedisOneResult();
            DoyaData doyaData = new DoyaData();
            result.setDoyaTextViewContainer(doyaTextViewContainer);

            jedis.auth(REDIS_PASSWORD);
            try{
                Integer point = jedis.zincrby(REDIS_SCORE_KEY, -1, doyaTextViewContainer.getObjectKey()).intValue();
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
            jedis.close();
        }
    }

    protected void reportDialog(final String url){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog;
        builder.setMessage(R.string.report_dialog_message)
                .setTitle(R.string.report_dialog_title)
                .setPositiveButton(R.string.hai, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ViolateReport violateReport = new ViolateReport();
                        violateReport.execute(url);
                    }
                })
                .setNegativeButton(R.string.iie, null);
         dialog = builder.create();
        dialog.show();
    }

    private class ViolateReport extends AsyncTask<String, Void, JedisOneResult>{
        Jedis jedis;
        @Override
        protected void onPreExecute() {
            jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        }

        @Override
        protected JedisOneResult doInBackground(String... strings) {
            String url = strings[0];
            JedisOneResult result = new JedisOneResult();

            jedis.auth(REDIS_PASSWORD);

            try {
                Integer point = jedis.zadd(REDIS_REPORT_KEY, 1, url).intValue();
                Log.d("report", point.toString());
            } catch (Exception e){
                result.setErrorMessage(e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(JedisOneResult jedisOneResult) {
            if(jedisOneResult.getErrorMessage() != null){
                Log.d("jedis", jedisOneResult.getErrorMessage());
            } else {
                Toast toast = Toast.makeText(context, R.string.report_complete, Toast.LENGTH_SHORT);
                toast.show();
            }
            jedis.close();
        }
    }
}
