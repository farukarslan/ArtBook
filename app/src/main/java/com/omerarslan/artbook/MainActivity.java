package com.omerarslan.artbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> nameArray;
    ArrayList<Integer> idArray;
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        nameArray = new  ArrayList<String>();
        idArray = new ArrayList<Integer>();

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nameArray);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, AddArt.class);
                intent.putExtra("artId", idArray.get(position));
                intent.putExtra("info", "old");
                startActivity(intent);
            }
        });

        getData();
    }

    public void getData(){
        try {
            SQLiteDatabase db = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

            Cursor cursor = db.rawQuery("SELECT * FROM arts", null);
            int nameIx = cursor.getColumnIndex("artname");
            int idIx = cursor.getColumnIndex("id");

            while (cursor.moveToNext()){
                nameArray.add(cursor.getString(nameIx));
                idArray.add((cursor.getInt(idIx)));
            }
            arrayAdapter.notifyDataSetChanged(); //yeni eklenen veriyi listede g??ster

            cursor.close();
        } catch ( Exception e){
            e.printStackTrace();
        }

    }

    //Bu activite'de hangi men??y?? g??sterece??iz
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Inflater = yap??lan bir xml'i(??rne??in menu) activite i??erisinde g??sterebilmek i??in
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.add_art, menu); //olu??turdu??umuz men??y?? aktivitemize ba??lad??k

        return super.onCreateOptionsMenu(menu);
    }

    //Herhangi bir item se??ilirse ne yap??lacak
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //t??klan??lan yer, burada bize item olarak veriliyor
        if (item.getItemId() == R.id.add_art_item){
            Intent intent = new Intent(MainActivity.this,AddArt.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}