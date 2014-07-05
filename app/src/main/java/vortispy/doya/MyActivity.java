package vortispy.doya;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import redis.clients.jedis.Jedis;


public class MyActivity extends Activity {
    /*
    private class DoyaPlus{
        int point = 0;
        int id;
        String name;

        DoyaPlus(int id, String name){
            this.id = id;
            this.name = name;
        }
        public int getId(){
            return id;
        }
        public int getPoint(){
            return point;
        }
        public void upPoint(){
            point++;
        }
        public void downPoint(){
            point--;
        }
        public String getName(){
            return this.name;
        }
    }
*/
    private List<String> pictures = new ArrayList<String>();
    private ListView mListView;
    private ArrayAdapter<String> adapter;

    private AmazonS3Client s3Client;

    final String LOCALHOST = "10.0.2.2";// "10.0.2.2" is PC localhost

    private final int REQUEST_GALLERY = 0;
    int nowId = 0;
    //final DoyaPlus doyas[] = {new DoyaPlus(R.drawable.dog1, "dog1"),new DoyaPlus(R.drawable.dog2, "dog2"),new DoyaPlus(R.drawable.dog3, "dog3")};
/*
    //protected AsyncJedis asjd;
    protected int getNowId(){
        return nowId;
    }
    protected void nextId(){
        nowId = nowId < doyas.length-1 ? nowId+1: 0;
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_frame);

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
/*
        adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, pictures);
        final ImageView image = (ImageView) findViewById(R.id.imageView);
        image.setImageResource(R.drawable.dog1);
        final TextView point = (TextView) findViewById(R.id.pointView);

        asjd = new AsyncJedis(point);
        asjd.execute("get", doyas[nowId].getName());

        Button nextButton = (Button) findViewById(R.id.button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextId();
                image.setImageResource(doyas[nowId].getId());
                asjd = new AsyncJedis(point);
                asjd.execute("get", doyas[nowId].getName());
            }
        });

        Button plusButton = (Button) findViewById(R.id.plus);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //doyas[getNowId()].upPoint();
                asjd = new AsyncJedis(point);
                asjd.execute("incr", doyas[nowId].getName());
            }
        });

        Button minusButton = (Button) findViewById(R.id.minus);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //doyas[getNowId()].downPoint();
                asjd = new AsyncJedis(point);
                asjd.execute("decr", doyas[nowId].getName());
            }
        });

        Button addImage = (Button) findViewById(R.id.addImageButton);
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent i = new Intent(getApplicationContext(), PickPhotoActivity.class);
//                startActivity(i);
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_GALLERY);
            }
        });

        final Button ranking = (Button) findViewById(R.id.ranking);
        ranking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), Ranking.class);
                startActivity(i);
            }
        });

        Button showInBrowser = (Button) findViewById(R.id.show_in_browser_button);
        showInBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new S3GeneratePresignedUrlTask().execute();
            }
        });

        Button getPhotoList = (Button) findViewById(R.id.photo_list_button);
        getPhotoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), ImageListActivity.class);
                startActivity(i);
            }
        });
*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*
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
                // 選択した画像を表示
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(img);
                // upload image
                new S3PutObjectTask().execute(exifData);
            }else{
                Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            }
        }
        */
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }
/*
    public class AsyncJedis extends AsyncTask<String, String, String>{
        Jedis jd;
        TextView textView;
        public AsyncJedis(TextView textView){
            super();

            this.textView = textView;
        }

        @Override
        protected String doInBackground(String... strings) {
            this.jd = new Jedis(LOCALHOST);
            String ret = "";
            String key = "point";
            String method = strings[0];
            if (method.equals("set")){
                this.jd.zadd(key, Double.valueOf(strings[2]), strings[1]);
                ret = this.jd.set(strings[1], strings[2]);
            }else if(!this.jd.exists(strings[1])){
                this.jd.zadd(key, 0, strings[1]);
                this.jd.set(strings[1], "0");
            }

            if (method.equals("get")) {
                ret = this.jd.get(strings[1]);
            }else if (method.equals("incr")){
                this.jd.incr(strings[1]);
                this.jd.zincrby(key, 1, strings[1]);
                ret = this.jd.get(strings[1]);
            }else if (method.equals("decr")){
                this.jd.decr(strings[1]);
                this.jd.zincrby(key, -1, strings[1]);
                ret = this.jd.get(strings[1]);
            }
            return ret;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            this.textView.setText(s);
            this.jd.disconnect();
        }
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

                        MyActivity.this.finish();
                    }
                });

        confirm.show().show();
    }

    private class S3PutObjectTask extends AsyncTask<Uri, Void, S3TaskResult> {

        ProgressDialog dialog;
        Constants cons;

        protected void onPreExecute() {
            dialog = new ProgressDialog(MyActivity.this);
            dialog.setMessage(MyActivity.this
                    .getString(R.string.uploading));
            dialog.setCancelable(false);
            dialog.show();
            cons = new Constants(getString(R.string.aws_access_key), getString(R.string.aws_secret_key));
        }

        protected S3TaskResult doInBackground(Uri... uris) {

            if (uris == null || uris.length != 1) {
                return null;
            }

            // The file location of the image selected.
            Uri selectedImage = uris[0];


            ContentResolver resolver = getContentResolver();
            String fileSizeColumn[] = {OpenableColumns.SIZE};

            Cursor cursor = resolver.query(selectedImage,
                    fileSizeColumn, null, null, null);

            cursor.moveToFirst();

            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            // If the size is unknown, the value stored is null.  But since an int can't be
            // null in java, the behavior is implementation-specific, which is just a fancy
            // term for "unpredictable".  So as a rule, check if it's null before assigning
            // to an int.  This will happen often:  The storage API allows for remote
            // files, whose size might not be locally known.
            String size = null;
            if (!cursor.isNull(sizeIndex)) {
                // Technically the column stores an int, but cursor.getString will do the
                // conversion automatically.
                size = cursor.getString(sizeIndex);
            }

            cursor.close();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(resolver.getType(selectedImage));
            if(size != null){
                metadata.setContentLength(Long.parseLong(size));
            }

            S3TaskResult result = new S3TaskResult();

            // Put the image data into S3.
            try {
                s3Client.createBucket(cons.getPictureBucket());

                PutObjectRequest por = new PutObjectRequest(
                        cons.getPictureBucket(), cons.PICTURE_NAME,
                        resolver.openInputStream(selectedImage),metadata);
                s3Client.putObject(por);
            } catch (Exception exception) {

                result.setErrorMessage(exception.getMessage());
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

    private class S3GeneratePresignedUrlTask extends
            AsyncTask<Void, Void, S3TaskResult> {
        Constants cons;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            cons = new Constants(getString(R.string.aws_access_key), getString(R.string.aws_secret_key));
        }

        protected S3TaskResult doInBackground(Void... voids) {

            S3TaskResult result = new S3TaskResult();

            try {
                // Ensure that the image will be treated as such.
                ResponseHeaderOverrides override = new ResponseHeaderOverrides();
                override.setContentType("image/jpeg");

                // Generate the presigned URL.

                // Added an hour's worth of milliseconds to the current time.
                Date expirationDate = new Date(
                        System.currentTimeMillis() + 3600000);
                GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
                        cons.getPictureBucket(), cons.PICTURE_NAME);
                urlRequest.setExpiration(expirationDate);
                urlRequest.setResponseHeaders(override);

                URL url = s3Client.generatePresignedUrl(urlRequest);

                result.setUri(Uri.parse(url.toURI().toString()));

            } catch (Exception exception) {

                result.setErrorMessage(exception.getMessage());
            }

            return result;
        }

        protected void onPostExecute(S3TaskResult result) {

            if (result.getErrorMessage() != null) {

                displayErrorAlert(
                        MyActivity.this
                                .getString(R.string.browser_failure_title),
                        result.getErrorMessage());
            } else if (result.getUri() != null) {

                // Display in Browser.
                startActivity(new Intent(Intent.ACTION_VIEW, result.getUri()));
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
    private ProgressDialog createProgressDialog(Context context, String msg) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(msg);
        dialog.setCancelable(false);
        return dialog;
    }

    private class S3GetImageListTask extends
            AsyncTask<String, Void, S3TaskResult> {

        ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = createProgressDialog(MyActivity.this, "画像一覧を取得中...");
            dialog.show();
        }

        protected S3TaskResult doInBackground(String... params) {

            S3TaskResult result = new S3TaskResult();
            try {
                ObjectListing objectListing = s3Client.listObjects(
                        new ListObjectsRequest().withBucketName(params[0]));
                List<S3ObjectSummary> summeries = objectListing.getObjectSummaries();
                for (S3ObjectSummary summery : summeries) {
                    result.getPictureList().add(summery.getKey());
                }
            } catch (Exception exception) {
                result.setErrorMessage(exception.getMessage());
            }
            return result;
        }

        protected void onPostExecute(S3TaskResult result) {
            dialog.dismiss();
            if (result.getErrorMessage() != null) {
                displayErrorAlert("画像一覧を取得できませんでした",
                        result.getErrorMessage());
            } else if (result.getPictureList().size() > 0) {
                pictures.clear();
                pictures.addAll(result.getPictureList());
                adapter.notifyDataSetChanged();
            }
        }
    }
    */
}
