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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MyActivity extends Activity {
    private List<String> pictures = new ArrayList<String>();
    private ListView mListView;
    private ArrayAdapter<String> adapter;

    private AmazonS3Client s3Client;

    final String LOCALHOST = "10.0.2.2";// "10.0.2.2" is PC localhost

    private final int REQUEST_GALLERY = 0;
    int nowId = 0;

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
                new S3PutObjectTask().execute(exifData);
            }else{
                Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
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
                if(!s3Client.doesBucketExist(cons.getBucket())){
                    s3Client.createBucket(cons.getBucket());
                }

                PutObjectRequest por = new PutObjectRequest(
                        cons.getBucket(), cons.filename,
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
