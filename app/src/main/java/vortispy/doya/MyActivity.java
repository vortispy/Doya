package vortispy.doya;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import redis.clients.jedis.Jedis;


public class MyActivity extends FragmentActivity implements ActionBar.TabListener{
    private String REDIS_HOST;
    private Integer REDIS_PORT;
    private String REDIS_PASSWORD;
    private String REDIS_FILE_LIST_KEY;
    private String REDIS_SCORE_KEY;


    private final int REQUEST_GALLERY = 0;

    private String imei;
    private String upload_sha;

    private ViewPager mViewPager;
    private AppSectionsPagerAdapter appSectionsPagerAdapter;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_frame);


        appSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(appSectionsPagerAdapter);
        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);


        actionBar.addTab(actionBar
            .newTab()
            .setText("Image List")
            .setTabListener(this));
        actionBar.addTab(actionBar
            .newTab()
            .setText("Ranking")
            .setTabListener(this));

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                getActionBar().setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        REDIS_HOST = getString(R.string.redis_host);
        REDIS_PASSWORD = getString(R.string.redis_password);
        REDIS_PORT = Integer.valueOf(getString(R.string.redis_port));
        REDIS_FILE_LIST_KEY = getString(R.string.redis_file_list_key);
        REDIS_SCORE_KEY = getString(R.string.redis_score_key);

        upload_sha = getString(R.string.redis_upload_sha);

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId();


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

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int i) {
            switch (i) {
                case 0:
                    // The first section of the app is the most interesting -- it offers
                    // a launchpad into the other demonstrations in this example application.
                    return new ImageListFragment();
                case 1:
                    return new RankingFragment();

                default:
                    // The other sections of the app are dummy placeholders.
                    return new ImageListFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section " + (position + 1);
        }
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
        Jedis jedis;

        protected void onPreExecute() {
            dialog = new ProgressDialog(MyActivity.this);
            dialog.setMessage(MyActivity.this
                    .getString(R.string.uploading));
            dialog.setCancelable(false);
            dialog.show();

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
                Integer now = (int) new Date().getTime();
                if (statusCode == 200) {
                    List<String> keys = new ArrayList<String>();
                    List<String> args = new ArrayList<String>();

                    keys.add(imei);
                    keys.add(resBody);
                    args.add(now.toString());
                    String jedis_ret = (String) jedis.evalsha(upload_sha, keys, args);
                    if (jedis_ret.equals("black")){
                        result.setErrorMessage("YOU ARE IN BLACK LIST");
                    }
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
