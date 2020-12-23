package com.example.weather;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.weather.gson.Forecast;
import com.example.weather.gson.Weather;
import com.example.weather.service.AutoUpdateService;
import com.example.weather.utils.HttpUtil;
import com.example.weather.utils.Utility;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button nav;

    private Button add_fav;
    private Button my_fav;
    private Button cancel_fav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化

        weatherLayout = (ScrollView)findViewById(R.id.weather_layout);
        titleCity = (TextView)findViewById(R.id.title_city);
        titleUpdateTime = (TextView)findViewById(R.id.title_update_time);
        degreeText = (TextView)findViewById(R.id.degree_text);
        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        aqiText = (TextView)findViewById(R.id.aqi_text);
        pm25Text = (TextView)findViewById(R.id.pm25_text);
        comfortText = (TextView)findViewById(R.id.comfort_text);
        carWashText = (TextView)findViewById(R.id.car_wash_text);
        sportText = (TextView)findViewById(R.id.sport_text);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        nav = (Button)findViewById(R.id.nav_button);

        add_fav = (Button)findViewById(R.id.add_fav);
        my_fav = (Button)findViewById(R.id.my_fav);
        cancel_fav = (Button)findViewById(R.id.cancel_fav);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
        final String key = getIntent().getStringExtra("weather_id");
        final String weatherString = prefs.getString("weather",null);

        DBase dBase = new DBase(this,"Concern.db",null,1);
        final SQLiteDatabase db = dBase.getWritableDatabase();

        //添加关注
        add_fav.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                ContentValues contentValues = new ContentValues();
                Weather weather = Utility.handleWeatherResponse(weatherString);
                String name = weather.basic.cityName;
                String id = weather.basic.weatherId;
                contentValues.put("weatherId",id);
                contentValues.put("weatherName",name);
                if(db.insert("concern",null,contentValues)>0){
                    Toast.makeText(WeatherActivity.this, "关注成功！", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(WeatherActivity.this, "关注失败！", Toast.LENGTH_LONG).show();
                }
            }
        });

        //取消关注
        cancel_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Weather weather = Utility.handleWeatherResponse(weatherString);
                String id = weather.basic.weatherId;
                if(db.delete("concern","weatherId=?",new String[]{id+""})>0){
                    Toast.makeText(WeatherActivity.this, "取消成功！", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(WeatherActivity.this, "取消失败！", Toast.LENGTH_LONG).show();
                }
            }
        });

        //关注列表
        my_fav.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(WeatherActivity.this,MyConcern.class);
                startActivity(intent);
            }
        });

        //天气界面返回键
        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        String back = getIntent().getStringExtra("back");
        String send = getIntent().getStringExtra("send");
        if(back != null){
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }else{
            if(send != null){

                mWeatherId = getIntent().getStringExtra("weather_id");
                weatherLayout.setVisibility(View.INVISIBLE);
                requestWeather(mWeatherId);
            }else{
                //有缓存则直接解析
                if(weatherString != null){
                    Weather weather = Utility.handleWeatherResponse(weatherString);
                    mWeatherId = weather.basic.weatherId;
                    showWeatherInfo(weather);
                }else {
                    //无缓存则访问服务器
                    mWeatherId = getIntent().getStringExtra("weather_id");
                    weatherLayout.setVisibility(View.INVISIBLE);
                    requestWeather(mWeatherId);
                }
            }

        }



        //下拉刷新
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(){
                //SharedPreferences prefs = getSharedPreferences("data",MODE_PRIVATE);
                mWeatherId = prefs.getString("weatherID",null);
                requestWeather(mWeatherId);
                Toast.makeText(WeatherActivity.this, "数据刷新！", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 根据天气id查询天气
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //返回值保存为String类型
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.putString("weatherID",weatherId);
                            editor.apply();
                            showWeatherInfo(weather);

                        }else {
                            Toast.makeText(WeatherActivity.this,"获取天气失败,下拉刷新",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 处理展示Weather中数据
     */
    private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();

        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout,false);
            TextView dataText = (TextView)view.findViewById(R.id.date_text);
            TextView infoText = (TextView)view.findViewById(R.id.info_text);
            TextView maxText = (TextView)view.findViewById(R.id.max_text);
            TextView minText = (TextView)view.findViewById(R.id.min_text);
            dataText.setText(forecast.date);
            maxText.setText(forecast.temperature.max);
            infoText.setText(forecast.more.info);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度："+weather.suggestion.comfort.info;
        String carWash = "洗车指数："+weather.suggestion.carWash.info;
        String sport = "运动建议:"+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
}
