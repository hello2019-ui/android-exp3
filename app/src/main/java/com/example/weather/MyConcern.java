package com.example.weather;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;


public class MyConcern extends AppCompatActivity  {

    ArrayAdapter simpleAdapter;
    ListView MyConcernList;
    private List<String> city_nameList = new ArrayList<>();
    private List<String> city_codeList = new ArrayList<>();
    ImageView back;

    private void InitConcern() {       //进行数据填装
        DBase dbHelper = new DBase(this,"Concern.db",null,1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor  = db.rawQuery("select * from concern",null);
        while(cursor.moveToNext()){
            String city_code = cursor.getString(cursor.getColumnIndex("weatherId"));
            String city_name = cursor.getString(cursor.getColumnIndex("weatherName"));
            city_codeList.add(city_code);
            city_nameList.add(city_name);
        }
    }

    public void RefreshList(){
        city_nameList.removeAll(city_nameList);
        city_codeList.removeAll(city_codeList);
        simpleAdapter.notifyDataSetChanged();
        DBase dbHelper = new DBase(this,"Concern.db",null,1);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor  = db.rawQuery("select * from Concern",null);
        while(cursor.moveToNext()){
            String city_code = cursor.getString(cursor.getColumnIndex("weatherId"));
            String city_name = cursor.getString(cursor.getColumnIndex("weatherName"));
            city_codeList.add(city_code);
            city_nameList.add(city_name);
        }
    }


    @Override
    protected void onStart(){
        super.onStart();
        RefreshList();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.concern);
        MyConcernList = findViewById(R.id.MyConcernList);
        back = (ImageView) findViewById(R.id.concern_back);
        InitConcern();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MyConcern.this,WeatherActivity.class);
                startActivity(intent);
                finish();
            }
        });

        simpleAdapter = new ArrayAdapter(MyConcern.this,android.R.layout.simple_list_item_1,city_nameList);

        MyConcernList.setAdapter(simpleAdapter);
        MyConcernList.setOnItemClickListener(new AdapterView.OnItemClickListener(){      //配置ArrayList点击按钮
            @Override
            public void  onItemClick(AdapterView<?> parent, View view , int position , long id){
                String tran = city_codeList.get(position);
                String back = "true";
                Intent intent = new Intent(MyConcern.this, com.example.weather.WeatherActivity.class);
                intent.putExtra("weather_id",tran);
                intent.putExtra("back",back);
                startActivity(intent);
                finish();
            }
        });
    }

}
