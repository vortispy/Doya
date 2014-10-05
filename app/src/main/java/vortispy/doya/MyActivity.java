package vortispy.doya;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import redis.clients.jedis.Jedis;


public class MyActivity extends Activity {
    private List<String> pictures = new ArrayList<String>();
    private ListView mListView;
    private ArrayAdapter<String> adapter;

    private AmazonS3Client s3Client;

    final String LOCALHOST = "10.0.2.2";// "10.0.2.2" is PC localhost

    private String REDIS_HOST;
    private Integer REDIS_PORT;
    private String REDIS_PASSWORD;
    private String REDIS_FILE_LIST_KEY;
    private String REDIS_SCORE_KEY;


    private final int REQUEST_GALLERY = 0;
    int nowId = 0;

    private String s3Bucket;
    private String s3BucketPrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_frame);

        s3Bucket = getString(R.string.s3_bucket).toLowerCase(Locale.US);
        s3BucketPrefix = getString(R.string.s3_bucket_prefix).toLowerCase(Locale.US);

        final ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar
            .newTab()
            .setText("Image List")
            .setTabListener(
                    new MainTabListener<ImageListFragment>(
                            this,
                            "ImageList",
                            ImageListFragment.class
                    )
            ));
        actionBar.addTab(actionBar
            .newTab()
            .setText("Ranking")
            .setTabListener(
                    new MainTabListener<RankingFragment>(
                            this,
                            "Ranking",
                            RankingFragment.class
                    )
            ));
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(
                        getString(R.string.aws_access_key),
                        getString(R.string.aws_secret_key)
                ));

        REDIS_HOST = getString(R.string.redis_host);
        REDIS_PASSWORD = getString(R.string.redis_password);
        REDIS_PORT = Integer.valueOf(getString(R.string.redis_port));
        REDIS_FILE_LIST_KEY = getString(R.string.redis_file_list_key);
        REDIS_SCORE_KEY = getString(R.string.redis_score_key);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.my, menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                return true;
            case R.id.new_picture:
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_GALLERY);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
           if(requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            final Uri exifData = data.getData();
            final InputStream ins;
            try {
                ins = getContentResolver().openInputStream(exifData);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            final Bitmap img  = BitmapFactory.decodeStream(ins);

            if (img != null) {
                // upload image
                new S3PutObjectTask().execute(img);
            }else{
                Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    protected boolean s3doesKeyExist(String key){
        try{
            s3Client.getObject(s3Bucket, key);
        } catch(AmazonServiceException e){
            String errorCode = e.getErrorCode();
            if(!errorCode.equals("NoSuchKey")){
                throw e;
            }
            return false;
        }
        return true;
    }

    // Display an Alert message for an error or failure.
    protected void displayAlert(String title, String message) {

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                MyActivity.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

        confirm.show().show();
    }

    protected void displayErrorAlert(String title, String message) {

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                MyActivity.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

//                        MyActivity.this.finish();
                    }
                });

        confirm.show().show();
    }

    public int minimum(int a, int b){
        if(a <= b){
            return a;
        }
        return b;
    }

    private Bitmap squareBitmap(Bitmap bitmap){
        int minwh = minimum(bitmap.getWidth(), bitmap.getHeight());
        return ThumbnailUtils.extractThumbnail(bitmap, minwh, minwh);
    }

    private Bitmap resizeSquareBitmap(Bitmap bitmap, int x){
        Bitmap square = squareBitmap(bitmap);
        return ThumbnailUtils.extractThumbnail(square, x, x);
    }

    private byte[] reduceBitmapSize(Bitmap bitmap, int maxSize){
        int byteLength;
        int quality = 100;
        byte[] reduced;
        do{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            reduced = baos.toByteArray();
            byteLength = reduced.length;
            quality -= 5;
        }while(maxSize < byteLength);
        if(quality < 50){
            return null;
        }
        return reduced;
    }

    private class S3PutObjectTask extends AsyncTask<Bitmap,Void,S3TaskResult> {

        ProgressDialog dialog;
        String objectKey;
        Jedis jedis;

        protected void onPreExecute() {
            dialog = new ProgressDialog(MyActivity.this);
            dialog.setMessage(MyActivity.this
                    .getString(R.string.uploading));
            dialog.setCancelable(false);
            dialog.show();

            objectKey = s3BucketPrefix + UUID.randomUUID().toString();
            jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        }

        protected S3TaskResult doInBackground(Bitmap... uris) {

            jedis.auth(REDIS_PASSWORD);

            S3TaskResult result = new S3TaskResult();

            if (uris == null || uris.length != 1) {
                return null;
            }

            // The file location of the image selected.
            int imageX = 600;
            int maxImageSize = 1024 * 1024;
            Bitmap selectedImage = uris[0];

            Bitmap square = resizeSquareBitmap(selectedImage, imageX);
            byte[] bytes = reduceBitmapSize(square, maxImageSize);
            if (bytes == null) {
                result.setErrorMessage("Too large image size, or valid image");
                return result;
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://ds-s3-uploader.herokuapp.com/upload");
            HttpURLConnection httpURLConnection = null;
            int statusCode;
            String resBody;
            try {
                URL url = new URL("http://ds-s3-uploader.herokuapp.com/upload");
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.getOutputStream().write(bytes);
                statusCode = httpURLConnection.getResponseCode();
                int resLength = httpURLConnection.getContentLength();
                byte[] res = new byte[resLength];
                httpURLConnection.getInputStream().read(res);
                resBody = new String(res, "UTF-8");
                Log.d("httpCon", resBody);
                long now = new Date().getTime();
                if (statusCode == 200) {
                    jedis.zadd(REDIS_SCORE_KEY, 0, resBody);
                    jedis.zadd(REDIS_FILE_LIST_KEY, now, resBody);
                } else{
                    result.setErrorMessage("Sippai");
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                result.setErrorMessage(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                result.setErrorMessage(e.getMessage());
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }

            return result;
        }

        protected void onPostExecute(S3TaskResult result) {

            dialog.dismiss();

            if (result.getErrorMessage() != null) {

                displayErrorAlert(
                        MyActivity.this
                                .getString(R.string.upload_failure_title),
                        result.getErrorMessage());
            }
        }
    }

    private class S3TaskResult {
        String errorMessage = null;
        Uri uri = null;
        private List<String> pictureList = new ArrayList<String>();

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
    }
}
