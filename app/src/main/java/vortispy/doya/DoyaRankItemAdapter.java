package vortispy.doya;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by vortispy on 2014/07/25.
 */
public class DoyaRankItemAdapter extends ArrayAdapter<DoyaData> {
    private LayoutInflater inflater;
    private AmazonS3Client s3Client;

    final String LOCALHOST = "10.0.2.2";

    public DoyaRankItemAdapter(Context context, int resource, List<DoyaData> items) {
        super(context, resource, items);

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(
                        context.getString(R.string.aws_access_key),
                        context.getString(R.string.aws_secret_key))
        );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DoyaData item = (DoyaData) getItem(position);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.item_rank, null);
        }

        DoyaViewContainer doyaViewContainer = new DoyaViewContainer(
                convertView,
                item,
                getContext().getString(R.string.s3_bucket).toLowerCase(Locale.US),
                getContext().getString(R.string.s3_bucket_prefix).toLowerCase(Locale.US)
        );

        new S3GetImage().execute(doyaViewContainer);

        return convertView;
    }

    private class DoyaViewContainer{
        View convertView;
        DoyaData doyaData;
        String bucket, prefix;
        public DoyaViewContainer(View convertView, DoyaData doyaData, String bucket, String prefix){
            this.convertView = convertView;
            this.doyaData = doyaData;
            this.bucket = bucket;
            this.prefix = prefix;
        }

        public DoyaData getDoyaData() {
            return doyaData;
        }

        public View getConvertView() {
            return convertView;
        }

        public String getBucket() {
            return bucket;
        }

        public String getPrefix() {
            return prefix;
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
            String bucket = container.getBucket();
            String objectKey = container.getDoyaData().getObjectKey();
            String objectPath = container.getPrefix() + objectKey;
            try{
                InputStream inputStream = s3Client.getObject(bucket, objectKey).getObjectContent();
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
                ImageView imageView = (ImageView) result
                        .getDoyaViewContainer()
                        .getConvertView()
                        .findViewById(R.id.rankImageView);
                imageView.setImageBitmap(result.getBitmap());
            }
        }
    }
}
