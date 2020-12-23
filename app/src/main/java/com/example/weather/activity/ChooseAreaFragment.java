package com.example.weather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.example.weather.MainActivity;
import com.example.weather.R;
import com.example.weather.WeatherActivity;
import com.example.weather.db.City;
import com.example.weather.db.County;
import com.example.weather.db.Province;
import com.example.weather.utils.HttpUtil;
import com.example.weather.utils.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;
    private Button backButton;
    private Button send;
    private ListView listView;
    private EditText input;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    //省级列表
    private List<Province> provinceList;
    //市级列表
    private List<City> cityList;
    //县级列表
    private List<County> countyList;
    //被选中的省份
    private Province selectedProvice;
    //选中的城市
    private City selectedCity;
    //当前选中级别
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_Button);
        listView = view.findViewById(R.id.list_view);
        send = view.findViewById(R.id.btn_send);
        input = view.findViewById(R.id.edt_input);
        //getContext返回当前View运行在哪个Activity Context中
        //为ListView设置适配器
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvice = provinceList.get(pos);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(pos);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(pos).getWeatherId();
                    //若碎片在MainActivity中，则逻辑不变
                    if(getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        //否则直接关闭导航栏，显示刷新页面
                        WeatherActivity weatherActivity = (WeatherActivity)getActivity();
                        //关闭导航栏
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String turn_to_weather = input.getText().toString().trim();
                String sp = "true";
                Intent intent = new Intent(getActivity(), com.example.weather.WeatherActivity.class);
                intent.putExtra("weather_id",turn_to_weather);
                intent.putExtra("send",sp);
                startActivity(intent);
                getActivity().finish();
            }
        });

        queryProvinces();
    }

    //查询所有省份，先从数据库中查找，若无再从服务器中查询
    private void queryProvinces() {
        //标题栏设置为中国
        titleText.setText("中国");
        //隐藏返回键
        backButton.setVisibility(View.GONE);
        //调用Litepal接口查询省级数据
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            //成功读取则显示
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            //动态更新ListView，使之不用刷新
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            //当前级别设置为省级
            currentLevel = LEVEL_PROVINCE;
        } else {
            //若不能在数据库中读出，则从服务器中查询
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    //查询选中的省，先从数据库，再从服务器
    private void queryCities() {
        titleText.setText(selectedProvice.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?", String.valueOf(selectedProvice.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvice.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }

    }

    //查询县级
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvice.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    //根据传入的地址和服务类型从服务器上查询省市县数据
    private void queryFromServer(String address, final String type) {
        //对话进度框
        showProgressDialog();
        //服务器上查询
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            //数据回调到onResponse
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    //解析服务器返回的数据，并存储于数据库下
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvice.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    //线程切换,到子线程中去加载城市编码
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //关闭进度对话框
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                //重新从数据库中加载数据
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    //显示进度
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
