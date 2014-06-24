package vortispy.doya;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import redis.clients.jedis.Jedis;


public class MyActivity extends Activity {
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

    int nowId = 0;
    final DoyaPlus doyas[] = {new DoyaPlus(R.drawable.dog1, "dog1"),new DoyaPlus(R.drawable.dog2, "dog2"),new DoyaPlus(R.drawable.dog3, "dog3")};

    protected AsyncJedis asjd;
    protected int getNowId(){
        return nowId;
    }
    protected void nextId(){
        nowId = nowId < doyas.length-1 ? nowId+1: 0;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        final ImageView image = (ImageView) findViewById(R.id.imageView);
        image.setImageResource(R.drawable.dog1);
        final TextView point = (TextView) findViewById(R.id.pointView);

        for (DoyaPlus doya : doyas) {
            asjd = new AsyncJedis(point);
            asjd.execute("set", doya.getName(), "0");
        }

        point.setText(String.valueOf(doyas[nowId].getPoint()));

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

    public class AsyncJedis extends AsyncTask<String, String, String>{
        Jedis jd;
        TextView textView;
        public AsyncJedis(TextView textView){
            super();

            this.textView = textView;
        }

        @Override
        protected String doInBackground(String... strings) {
            this.jd = new Jedis("10.0.2.2");
            String ret = "";
            String method = strings[0];
            if (method.equals("get")) {
                ret = this.jd.get(strings[1]);
            }else if (method.equals("set")){
                ret = this.jd.set(strings[1], strings[2]);
            }else if (method.equals("incr")){
                this.jd.incr(strings[1]);
                ret = this.jd.get(strings[1]);
            }else if (method.equals("decr")){
                this.jd.decr(strings[1]);
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

}
