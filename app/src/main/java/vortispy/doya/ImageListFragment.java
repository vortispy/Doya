package vortispy.doya;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ImageListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ImageListFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ImageListFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private List<String> pictures = new ArrayList<String>();
    private ListView mListView;

    private DoyaItemAdapter doyaAdapter;
    private List<DoyaData> doyas = new ArrayList<DoyaData>();

    private AmazonS3Client s3Client;
    private String s3Bucket;
    private String s3BucketPrefix;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImageListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageListFragment newInstance(String param1, String param2) {
        ImageListFragment fragment = new ImageListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public ImageListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        s3Bucket = getString(R.string.s3_bucket).toLowerCase(Locale.US);
        s3BucketPrefix = getString(R.string.s3_bucket_prefix).toLowerCase(Locale.US);
        doyaAdapter = new DoyaItemAdapter(getActivity(), android.R.layout.simple_list_item_1, doyas);

        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(
                        getString(R.string.aws_access_key),
                        getString(R.string.aws_secret_key)
                ));


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_image_list, container, false);
        mListView = (ListView)v.findViewById(R.id.list_view_s3);
//        mListView.setAdapter(adapter);

        mListView.setAdapter(doyaAdapter);

        // Inflate the layout for this fragment
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        new S3GetImageListTask().execute(s3Bucket);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    protected void displayErrorAlert(String title, String message) {

        AlertDialog.Builder confirm = new AlertDialog.Builder(getActivity());
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                getActivity().getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

//                        getActivity().finish();
                    }
                });

        confirm.show().show();
    }
    private class S3TaskResult {
        String errorMessage = null;
        Uri uri = null;
        private List<String> pictureList = new ArrayList<String>();
        private List<DoyaData> doyaDataList = new ArrayList<DoyaData>();
        String key;
        Bitmap bitmap;

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

        public List<DoyaData> getDoyaDataList() {
            return doyaDataList;
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
            dialog = createProgressDialog(getActivity(), "画像一覧を取得中...");
            dialog.show();
        }

        protected S3TaskResult doInBackground(String... params) {

            S3TaskResult result = new S3TaskResult();
            try {
                s3Client.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
                List<Bucket> buckets = s3Client.listBuckets();

                ObjectListing objectListing = s3Client.listObjects(params[0], s3BucketPrefix);

                List<S3ObjectSummary> summeries = objectListing.getObjectSummaries();
                for (S3ObjectSummary summery : summeries) {
                    if (!summery.getKey().equals(s3BucketPrefix)) {
                        result.getPictureList().add(summery.getKey());
                        DoyaData doyaData = new DoyaData();
                        doyaData.setObjectKey(summery.getKey());
                        result.getDoyaDataList().add(0, doyaData);
                    }
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

                doyaAdapter.notifyDataSetChanged();
            } else if (result.getPictureList().size() > 0) {
                doyas.clear();
                doyas.addAll(result.getDoyaDataList());
                doyaAdapter.notifyDataSetChanged();
            }
        }
    }

    protected class S3GetImageTask extends
            AsyncTask<String, Void, S3TaskResult> {

        ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = createProgressDialog(getActivity(), "画像を取得中...");
            dialog.show();
        }

        /**
         * 指定した画像を S3 から取得.
         * @param params [0]:バケット名, [1]:オブジェクト名
         */
        protected S3TaskResult doInBackground(String... params) {
            S3TaskResult result = new S3TaskResult();
            try {
                InputStream in = s3Client.getObject(params[0], params[1])
                        .getObjectContent();
                Bitmap img = BitmapFactory.decodeStream(in);
                result.setKey(params[0] + "/" + params[1]);
                result.setBitmap(img);
            } catch (Exception exception) {
                result.setErrorMessage(exception.getMessage());
            }
            return result;
        }

        /**
         * 非同期処理終了後に実行される.
         * 取得した画像ファイルをダイアログで表示する.
         */
        protected void onPostExecute(S3TaskResult result) {
            dialog.dismiss();
            if (result.getErrorMessage() != null) {
                displayErrorAlert("画像ファイルが取得できませんでした",
                        result.getErrorMessage());
            } else {
                ImageView imgView = new ImageView(getActivity());
                imgView.setImageBitmap(result.getBitmap());
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(result.getKey())
                        .setView(imgView)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                    }
                                }).create();
                dialog.show();
            }
        }
    }
}
