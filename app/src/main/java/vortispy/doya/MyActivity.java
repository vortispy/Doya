package vortispy.doya;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class MyActivity extends Activity {
    private class DoyaPlus{
        int point = 0;
        int id;

        DoyaPlus(int id){
            this.id = id;
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
    }

    int nowId = 0;
    final DoyaPlus doyas[] = {new DoyaPlus(R.drawable.dog1),new DoyaPlus(R.drawable.dog2),new DoyaPlus(R.drawable.dog3)};

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
        point.setText(String.valueOf(doyas[nowId].getPoint()));

        Button nextButton = (Button) findViewById(R.id.button);
        nextButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                nextId();
                image.setImageResource(doyas[nowId].getId());
                point.setText(String.valueOf(doyas[nowId].getPoint()));
            }
        });

        Button plusButton = (Button) findViewById(R.id.plus);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doyas[getNowId()].upPoint();
                point.setText(String.valueOf(doyas[nowId].getPoint()));
            }
        });

        Button minusButton = (Button) findViewById(R.id.minus);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doyas[getNowId()].downPoint();
                point.setText(String.valueOf(doyas[nowId].getPoint()));
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
}
