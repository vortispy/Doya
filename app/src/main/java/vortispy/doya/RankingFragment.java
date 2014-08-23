package vortispy.doya;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RankingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RankingFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class RankingFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    final String LOCALHOST = "10.0.2.2";
    private String REDIS_HOST;
    private Integer REDIS_PORT;
    private String REDIS_PASSWORD;

    private View v;

    private ListView listView;
    private DoyaRankItemAdapter doyaRankItemAdapter;
    private List<DoyaData> doyas = new ArrayList<DoyaData>();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RankingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RankingFragment newInstance(String param1, String param2) {
        RankingFragment fragment = new RankingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public RankingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        REDIS_HOST = getString(R.string.redis_host);
        REDIS_PASSWORD = getString(R.string.redis_password);
        REDIS_PORT = Integer.valueOf(getString(R.string.redis_port));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        GetRanking g = new GetRanking();
//        g.execute("point");

        View view = inflater.inflate(R.layout.fragment_ranking, container, false);

        listView = (ListView) view.findViewById(R.id.rankListView);
        doyaRankItemAdapter = new DoyaRankItemAdapter(
                getActivity(),
                android.R.layout.simple_list_item_1,
                doyas
        );

        listView.setAdapter(doyaRankItemAdapter);

        new GetRanking().execute("pictures");
        // Inflate the layout for this fragment

        v = inflater.inflate(R.layout.fragment_ranking, container, false);
        return view;
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

    public class GetRanking extends AsyncTask<String,String, List<DoyaData>> {
        Jedis jd = new Jedis(REDIS_HOST, REDIS_PORT);


        @Override
        protected List<DoyaData> doInBackground(String... strings) {
            jd.auth(REDIS_PASSWORD);
//            Set<Tuple> s = this.jd.zrevrangeWithScores(strings[0], 0, 10);
            List<DoyaData> doyaDatas = new ArrayList<DoyaData>();
            Set<String> s = this.jd.zrevrange(strings[0], 0, 10);
            for (String inner: s){
                DoyaData doyaData = new DoyaData();
                doyaData.setObjectKey(inner);
                doyaDatas.add(doyaData);
            }
            return doyaDatas;
        }

        @Override
        protected void onPostExecute(List<DoyaData> doyaDatas) {
            /*
            TextView t = (TextView) v.findViewById(R.id.rankView);
            String text = "";

            int rank = 1;
            for (Tuple anInner : s) {
                text += Integer.toString(rank)+ ": " + anInner.getElement()+ " " + anInner.getScore() + "point\n";
                rank++;
            }
            //t.setText(s.toString());
            t.setText(text);
            */
            doyas.clear();
            doyas.addAll(doyaDatas);
            doyaRankItemAdapter.notifyDataSetChanged();
        }
    }
}
