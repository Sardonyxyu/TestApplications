package me.sardonyxyu.test.testapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MainActivity extends Activity{
    private Context ctx;
    private XunFeiYuYin feiYuYin = new XunFeiYuYin();
    private ListView lv_function;
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position == 0) {//驾车
                startActivity(new Intent(MainActivity.this, CarNavigationActivity.class));
            } else if (position == 1) {//步行
                startActivity(new Intent(MainActivity.this, WalkPathPlanningActivity.class));
            }

        }
    };
    private String[] examples = new String[]
            {"驾车导航", "步行导航"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=12345678");
//        feiYuYin.setBoBao(MainActivity.this, "加油啊");

        initView();
    }

    private void initView() {
        lv_function = (ListView) findViewById(R.id.lv_function);
        lv_function.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, examples));
        setTitle("路线规划");
        lv_function.setOnItemClickListener(mItemClickListener);
    }

}
