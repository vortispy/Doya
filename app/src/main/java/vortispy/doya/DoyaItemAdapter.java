package vortispy.doya;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by vortispy on 2014/07/19.
 */
public class DoyaItemAdapter extends ArrayAdapter<DoyaData> {
    private LayoutInflater inflater;
    private AmazonS3Client s3Client;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

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
    public View getView(int position, View convertView, ViewGroup parent) {
        DoyaData item = (DoyaData) getItem(position);

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

        TextView textView = (TextView) convertView.findViewById(R.id.doyaPoint);
//        textView.setText(item.getDoyaPoint().toString());

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
}
