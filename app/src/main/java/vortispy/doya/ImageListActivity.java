package vortispy.doya;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;
import java.util.List;

import vortispy.doya.R;

public class ImageListActivity extends Activity {

    private List<String> pictures = new ArrayList<String>();
    private ListView mListView;
    private ArrayAdapter<String> adapter;

    private AmazonS3Client s3Client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);

        adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, pictures);

        mListView = (ListView)findViewById(R.id.list_view_s3);
        mListView.setAdapter(adapter);

        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(
                        getString(R.string.aws_access_key),
                        getString(R.string.aws_secret_key)
                ));

        new S3GetImageListTask().execute(new Constants(
                getString(R.string.aws_access_key),
                getString(R.string.aws_secret_key)).getPictureBucket());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_list, menu);
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

    protected void displayErrorAlert(String title, String message) {

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                ImageListActivity.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        ImageListActivity.this.finish();
                    }
                });

        confirm.show().show();
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
            dialog = createProgressDialog(ImageListActivity.this, "画像一覧を取得中...");
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
}
