package vortispy.doya;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Set;

import redis.clients.jedis.Jedis;
import vortispy.doya.R;

public class Ranking extends Activity {
    final String LOCALHOST = "10.0.2.2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        GetRanking g = new GetRanking();
        g.execute("point");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ranking, menu);
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

    public class GetRanking extends AsyncTask<String,String, Set<String>>{
        Jedis jd = new Jedis(LOCALHOST);
        @Override
        protected Set<String> doInBackground(String... strings) {
            Set<String> s = this.jd.zrevrange(strings[0], 0, 10);
            return s;
        }

        @Override
        protected void onPostExecute(Set<String> s) {
            TextView t = (TextView) findViewById(R.id.rankView);
            String text = "";

            int rank = 1;
            for (String anInner : s) {
                text += Integer.toString(rank)+ ": " + anInner + "\n";
                rank++;
            }
            //t.setText(s.toString());
            t.setText(text);
        }
    }
}
