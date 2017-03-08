package com.example.yanglh6.myapplication9;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {
    //  定义我们需要用到的 WebService 地址
    private static final String url = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather";
    private static final String id = "b3ee954a63d94069b85fa97db49dd056";

    //  定义消息类型
    private static final int UPDATE_CONTENT = 0;

    //  初始化对象
    EditText searchET;
    Button searchBTN;
    TextView city_name;
    TextView time;
    LinearLayout LL;
    LinearLayout layout;
    TextView lowestTem;
    TextView tem;
    TextView wet;
    TextView airQ;
    TextView wind;
    ListView listview;
    RecyclerView recyclerView;

    //  设置时间对象，用以判断二次查询间隔是否小于600ms
    long Time1 = -1000;
    long Time2 = -1000;

    SimpleAdapter myAdapter;
    WeatherAdapter myAdapter2;

    ArrayList<Map<String, Object>> data_list;
    ArrayList<Weather> weather_list = new ArrayList<>();

    String searchCity = "";
    String thelastCity = "";
    String temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchET = (EditText) findViewById(R.id.searchET);
        searchBTN = (Button) findViewById(R.id.search);
        listview = (ListView) findViewById(R.id.list);
        LL = (LinearLayout) findViewById(R.id.LL);
        layout = (LinearLayout) findViewById(R.id.layout);
        city_name = (TextView) findViewById(R.id.city_name);
        time = (TextView) findViewById(R.id.time);
        lowestTem = (TextView) findViewById(R.id.lowestTem);
        tem = (TextView) findViewById(R.id.tem);
        wet = (TextView) findViewById(R.id.wet);
        airQ = (TextView) findViewById(R.id.airQ);
        wind = (TextView) findViewById(R.id.wind);

        //   ListView 的设置
        data_list = new ArrayList<>();
        myAdapter = new SimpleAdapter(this, data_list, R.layout.item, new String[]{"theType", "theContent"},
                new int[]{R.id.theType, R.id.theContent});
        listview.setAdapter(myAdapter);

        //   RecylerView 的设置
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        //  搜索按钮点击事件
        searchBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Time2 = Time1;
                Time1 = (new Date()).getTime();
                if (Time1 - Time2 < 600) {
                    Toast.makeText(MainActivity.this, "您的点击速度过快，两次查询间隔须小于600ms~", Toast.LENGTH_SHORT).show();
                    return;
                }
                searchCity = searchET.getText().toString();
                if (searchCity.equals("")) {
                    Toast.makeText(MainActivity.this, "查询栏输入为空哦~", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (searchCity.equals(thelastCity)) return;
                LL.setVisibility(View.VISIBLE);
                listview.setVisibility(View.VISIBLE);

                //  判断是否有可用网络：使用 ConnectivityManager 获取手机所有连接管理对象，使用 manager 获取 NetworkInfo 对象，最后判断当前网络状态是否为连接状态即可。
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if ((networkInfo == null) || !networkInfo.isConnected()) {
                    Toast.makeText(MainActivity.this, "当前没有可用网络哦~", Toast.LENGTH_SHORT).show();
                    return;
                }
                data_list.clear();
                myAdapter.notifyDataSetChanged();
                sendRequestWithHttpURLConnection();
                return;
            }
        });
    }

    //  http 请求需要开启子线程，然后由子线程执行请求，所以我们之前所写代码都是在子线程中完成的，并且使用 XmlPullParser 进行解析从而得到我们想要的数
    private void sendRequestWithHttpURLConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //  使用 HttpURLConnection 新建一个 http 连接，新建一个 URL 对象，打开连接即可，并且设置访问方法以及时间设置
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) ((new URL(url).openConnection()));
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setRequestMethod("POST");

                    //  将我们需要请求的字段以流的形式写入 connection 之中，这一步相当于将需要的参数提交到网络连接，并且请求网络数据（类似于 html 中的表单操作，将 post 数据提交到服务器）
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes("theCityCode=" + URLEncoder.encode(searchCity, "utf-8") + "&theUserID="+id);
                    //  注意中文乱码解决

                    //  网页获取 xml 转化为字符串
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line);
                    }

                    //  Message消息传递
                    Message message = new Message();
                    message.what = UPDATE_CONTENT;
                    message.obj = parseXMLWithPull(response.toString());
                    handler.sendMessage(message);
                } catch (Exception e) {

                    e.printStackTrace();
                } finally {
                    //  关闭 connection
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    //  子线程中不能直接修改 UI 界面，需要 handler 进行UI 界面的修改
    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case UPDATE_CONTENT:
                    data_list.clear();
                    myAdapter.notifyDataSetChanged();
                    List<String> data = (List<String>) message.obj;
                    temp = data.get(0);
                    if (temp.contains("查询结果为空")) {
                        Toast.makeText(MainActivity.this, "当前输入城市不存在，请重新输入哦~", Toast.LENGTH_SHORT).show();
//                        listview.setVisibility(View.VISIBLE);
//                        LL.setVisibility(View.INVISIBLE);
//                        layout.setVisibility(View.INVISIBLE);
//                        recyclerView.setVisibility(View.INVISIBLE);
                        return;
                    }
                    if (temp.contains("高速访问")) {
                        Toast.makeText(MainActivity.this, "您的点击速度过快，两次查询间隔须小于600ms~", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (temp.contains("规定数量")) {
                        Toast.makeText(MainActivity.this, "免费用户24小时内访问超过规定数量50次啦~", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (data.size() >= 2) {
                        LL.setVisibility(View.VISIBLE);
                        layout.setVisibility(View.VISIBLE);
//                        listview.setVisibility(View.VISIBLE);
//                        recyclerView.setVisibility(View.VISIBLE);
                        city_name.setText(data.get(1));
                        thelastCity = data.get(1);
                        temp = data.get(3);
                        int spaceIndex = temp.indexOf(" ");
                        temp = temp.substring(spaceIndex + 1) + "更新";
                        time.setText(temp);
                        temp = data.get(4);

                        //  不断的将所需要的字符串进行分割处理，以用来更新 UI 界面
                        StringTokenizer stringTokenizer = new StringTokenizer(temp, "：；:;");
                        stringTokenizer.nextToken();
                        temp = stringTokenizer.nextToken();
                        if (temp.contains("暂无实况")) {
                            lowestTem.setText("暂无实况");
                            temp = data.get(5);
                            stringTokenizer = new StringTokenizer(temp, "：；:;");
                            stringTokenizer.nextToken();
                            temp = stringTokenizer.nextToken();
                            airQ.setText(temp);
                            Map<String, Object> map = new HashMap<>();
                            map.put("theType", "");
                            map.put("theContent", data.get(6));
                            data_list.add(map);
                            myAdapter.notifyDataSetChanged();
                        } else {
                            temp = stringTokenizer.nextToken();
                            lowestTem.setText(temp);
                            stringTokenizer.nextToken();
                            temp = stringTokenizer.nextToken();
                            wind.setText(temp);
                            stringTokenizer.nextToken();
                            temp = "湿度：" + stringTokenizer.nextToken();
                            wet.setText(temp);
                            temp = data.get(5);
                            stringTokenizer = new StringTokenizer(temp, "。");
                            stringTokenizer.nextToken();
                            temp = stringTokenizer.nextToken();
                            airQ.setText(temp);
                            temp = data.get(6);
                            stringTokenizer = new StringTokenizer(temp, ":：.。\n");
                            while (stringTokenizer.hasMoreElements()) {
                                Map<String, Object> map = new HashMap<>();
                                temp = stringTokenizer.nextToken();
                                map.put("theType", temp);
                                if (stringTokenizer.hasMoreElements()) {
                                    temp = stringTokenizer.nextToken();
                                    map.put("theContent", temp);
                                    data_list.add(map);
                                }
                            }
                            myAdapter.notifyDataSetChanged();

                            //  RecyclerView的数据获取和设置
                            weather_list.clear();
                            String first_day = (data).get(7);
                            String[] tag_first = first_day.split("[ ]");
                            String second_day = (data).get(12);
                            String[] tag_second = second_day.split("[ ]");
                            String third_day = (data).get(17);
                            String[] tag_third = third_day.split("[ ]");
                            String fourth_day = (data).get(22);
                            String[] tag_fourth = fourth_day.split("[ ]");
                            String fif_day = (data).get(27);
                            String[] tag_fif = fif_day.split("[ ]");

                            //  设置三个字符串组将五天的天气信息添加到其中
                            String[] date = new String[]{tag_first[0], tag_second[0], tag_third[0], tag_fourth[0], tag_fif[0]};
                            String[] weather_d = new String[]{tag_first[1], tag_second[1], tag_third[1], tag_fourth[1], tag_fif[1]};
                            String[] tempurature = new String[]{(data).get(8), (data).get(13),
                                    (data).get(18), (data).get(23), (data).get(28)};

                            //  按照顺序将五天的信息添加进 weather_list 中去
                            for (int i = 0; i < 5; i++) {
                                Weather temp2 = new Weather(date[i], weather_d[i], tempurature[i]);
//                                weather_list = new ArrayList<>();
                                temp2.date = date[i];
                                temp2.weather_description = weather_d[i];
                                temp2.temperature = tempurature[i];
                                weather_list.add(temp2);
                            }
                            myAdapter2 = new WeatherAdapter(MainActivity.this, weather_list);
                            recyclerView.setAdapter(myAdapter2);
                            myAdapter2.notifyDataSetChanged();
                        }
                        tem.setText(data.get(8));
                    } else {

                    }
                    break;
                default:
                    break;
            }
        }
    };

    public ArrayList<String> parseXMLWithPull(String xml) throws XmlPullParserException, IOException {
        //  首先获取 XmlPullParser 对象实例，然后设置需要解析的字符串，最后按照 tag 逐个获取所需要的 string
        //  获取实例
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        //  设置所需要解析的string
        parser.setInput(new StringReader(xml));

        int eventType = parser.getEventType();
        ArrayList<String> list = new ArrayList<>();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("string".equals(parser.getName())) {
                        String str = parser.nextText();
                        list.add(str);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    break;
                default:
                    break;
            }
            eventType = parser.next();
        }
        return list;
    }
}

//  第一次是用ksoap2来写的......

//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.FutureTask;
//
//import org.ksoap2.SoapEnvelope;
//import org.ksoap2.serialization.SoapObject;
//import org.ksoap2.serialization.SoapSerializationEnvelope;
//import org.ksoap2.transport.HttpTransportSE;
//import org.xmlpull.v1.XmlPullParserException;
//
//import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.ActionBar;
//import android.support.v4.app.Fragment;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.os.Build;
//
//public class MainActivity extends AppCompatActivity {
//    private EditText searchET;
//    private Button search;
//    final String NAMESPACE ="http://WebXml.com.cn/";
//    //webService地址
//    final String URL = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather";
//    final String METHOD_NAME = "getWeatherbyCityName";
//    final String SOAP_ACTION = "http://WebXml.com.cn/getWeatherbyCityName";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        searchET = (EditText)findViewById(R.id.searchET);
//        search = (Button)findViewById(R.id.search);
//
//
//
//        search.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                getWeather();
//
//            }
//        });
//    }
//
//    private  void getWeather(){
//        Thread thread = new Thread( new Runnable() {
//            //@Override
//            public void run() {
//                try {
//                    //获取输入的城市名称
//                    final TextView mTextWeather = (TextView) findViewById(R.id.weather);
//                    String City = searchET.getText().toString();
//
//                    final String NAMESPACE ="http://WebXml.com.cn/";
//                    //webService地址
//                    final String URL = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather";
//                    final String OPERATIOM_NAME = "getWeather";
//                    final String SOAP_ACTION = "http://WebXml.com.cn/getWeather"; 	//命名空间+方法名
//                    // 创建HttpTransportSE对象，传递WebService服务器地址
//                    HttpTransportSE ht = new HttpTransportSE(URL);
//                    //1.创建SoapObject 并指定访问的名称空间及方法名
//                    SoapObject sop = new SoapObject(NAMESPACE, OPERATIOM_NAME);
//                    //2.设置参数值
//                    sop.addProperty("theCityCode", City);
//                    sop.addProperty("theUserID", "f0606b9751484523a182a12c43e68f20");
//
//                    /**
//                     常量SoapEnvelope.VER10：对应于SOAP 1.0规范
//                     * 常量SoapEnvelope.VER11：对应于SOAP 1.1规范
//                     * 常量SoapEnvelope.VER12：对应于SOAP 1.2规范
//                     * 这样，无论要调用的webservice采用了哪一个SOAP规范，你都可以轻松应对。
//                     * */
//
//                    // 实例化SoapSerializationEnvelope，传入WebService的SOAP协议的版本号
//                    SoapSerializationEnvelope senp  = new SoapSerializationEnvelope(SoapEnvelope.VER11);
//                    //设置是否调用.net开发的webService
//                    senp.dotNet = true;
//                    //设置对象的bodyOut属性
//                    senp.setOutputSoapObject(sop);//senp.bodyOut = sop;
//                    ht.debug=true;
//                    //执行call方法 发送请求
//                    //ht.call(null, senp);
//                    ht.call(SOAP_ACTION, senp);
//
//                    //获取结果
//                    SoapObject obj = (SoapObject) senp.bodyIn;
//                    SoapObject detail = (SoapObject) obj.getProperty("getWeatherbyCityNameResult");
//                    StringBuilder sb = new StringBuilder();
//                    for(int i=0; i<detail.getPropertyCount(); i++){
//                        sb.append(detail.getProperty(i)).append("\r\n");
//                    }
//                    mTextWeather.setText(sb.toString());
//                    Log.i("MyTag", detail.toString());
//                    //效果 ： 会输出从网络获取的对应城市的天气信息
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (XmlPullParserException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        Log.i("MyTag", "start");
//        thread.start();
//    }
//}




