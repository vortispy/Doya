package vortispy.doya;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

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

    final String LOCALHOST = "10.0.2.2";// "10.0.2.2" is PC localhost

    private final int REQUEST_GALLERY = 0;
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
            }else{
                Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            }
        }
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

}
