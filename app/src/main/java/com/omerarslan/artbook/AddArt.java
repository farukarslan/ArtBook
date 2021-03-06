package com.omerarslan.artbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddArt extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText artNameText, painterNameText, yearText;
    Button button;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_art);

        imageView = findViewById(R.id.imageView);
        artNameText = findViewById(R.id.artNameText);
        painterNameText = findViewById(R.id.painterNameText);
        yearText = findViewById(R.id.yearText);
        button = findViewById(R.id.button);

        db = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.matches("new")){
            artNameText.setText("");
            painterNameText.setText("");
            yearText.setText("");
            button.setVisibility(View.VISIBLE);
            imageView.setEnabled(true);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimage);
            imageView.setImageBitmap(selectImage);

        } else{
            int artId = intent.getIntExtra("artId",1);
            button.setVisibility(View.INVISIBLE);
            imageView.setEnabled(false);
            artNameText.setKeyListener(null);
            painterNameText.setKeyListener(null);
            yearText.setKeyListener(null);

            try {

                Cursor cursor = db.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)}); //Selection arg??manlar??

                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    artNameText.setText(cursor.getString(artNameIx));
                    painterNameText.setText(cursor.getString(painterNameIx));
                    yearText.setText(cursor.getString(yearIx));

                    //G??rseli byte dizisi olarak kaydettik. Yine o ??ekilde al??caz ve g??rsel haline getirece??iz.
                    byte [] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }

                cursor.close();

            } catch (Exception e){

            }
        }

    }
    public void selectImage(View view){
        //Android API23 ??ncesinde izin isteme yoktu. Compat'da e??er sistem API23'den d??????kse
        //izin istemeden ??al????t??r??yor. API23'den b??y??kse izin istiyor.

        //eri??im izin verilmediyse
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else{
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //galeriyi a????p resim toplamak i??in
            startActivityForResult(intentToGallery, 2);
        }
    }

    //izin istendi??inde bunun sonucunda ne olaca???? belirtilen metod

    @Override                                                                         // grantResults = verilen sonu??lar int dizisi ??eklinde geliyor
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // ??zin verildi??inde tekrar resme t??klamay?? gerektirmeden direkt galeriyi a??mas?? i??in
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery, 2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // ??zin verilip, galeriye gittikten sonra ne olacak, g??rseli al??p ne yapaca????z
    @Override                                                                //geri gelen veri
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null){

            Uri imageData = data.getData(); //Bize URI veriyor.(Nereye kay??tl?? oldu??unun yolu)

            //Unhandled exception hatas?? verdi. Bu hata ????karabilir demek. O y??zden try-catch blo??u i??erisine ald??k.
            try {

                //yeni versiyonlar i??in yeni metod ile yap??lacak k??s??m
                if (Build.VERSION.SDK_INT >= 28){
                    //URI'?? Bitmap'e ??evirmek i??in geli??tirilmi?? bir s??n??f
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                }else{
                    //SDK VERSION 28'den ??ncekilerde bu ??ekilde yap??l??yor yeni versiyonlarda tedav??lden kalkt??
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                    imageView.setImageBitmap(selectedImage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view) {
        String artName = artNameText.getText().toString();
        String painterName = painterNameText.getText().toString();
        String year = yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        //G??reseli veriye ??evirme i??lemi (byte'a ??evirdikten sonra veritaban??na kaydedebiliriz)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();   // outputStream = veriyi byte array ??eklinde almam??z?? sa??l??yor
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);     //d??n????t??r??lecek g??rsel ??ekli, kalitesi belirttik
        byte[] byteArray = outputStream.toByteArray();

        try {

            db = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)"); //SQLite'da verileri blob olarak kaydediyoruz.

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = db.compileStatement(sqlString); //sqlString'i al ve compile et
            sqLiteStatement.bindString(1, artName);
            sqLiteStatement.bindString(2, painterName);  //SQL sorgusunda '?' olan yerlere gelecek ifadeler
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        } catch (Exception e){

        }
        /*
            MainActivity'de onCreate i??erisinde veriler ??ekilip listeleniyor. Yeni kay??t yap??ld??ktan sonra bu activity
            finish yap??l??p kapat??ld?????? i??in oraya geri d??n??ld??????nde onCreate tekrar ??a????r??lm??yor, onResume ??a????r??l??yor.
            Bu y??zden yeni kay??ttan sonra geri d??nd??????m??zde o kay??t g??z??km??yor. Burada intent ile di??er activity'ye
            gidersek yeni kay??tlar da g??z??k??r fakat ??b??r activity'yi bitirmeden s??rekli yeni a????ld??????ndan dolay??
            arkada bir s??r?? activity b??rak??r. Geri geldi??imizde teker teker onlara d??neriz.
         */

        Intent intent = new Intent(AddArt.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Daha ??nceki b??t??n activity'leri kapatacak.
        startActivity(intent);

        //finish(); // aktiviteyi komple bitir. Bu sayede kay??t yap??ld??ktan sonra ??nceki aktiviteye geri d??necek

    }

    //Ger??ekten g??rsellerin k??????k oldu??una emin olmam??z laz??m. SQLite 1MB ??st?? dosyalarda ????kebiliyor.
    //Bunun i??in yeni bir metod yazaca????z
    //Resmin uzun kenar??n?? al??p maximumSize'a e??itliyor ve k??sa kenar?? da resmin oran??n?? bozmadan ayn?? oranda k??????lt??yor

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1 ) { //resim yatay ise
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        }else{
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        //Bitmap'in kendi fonksiyonunu kullanarak yeni bir bitmap olu??turduk.
        return Bitmap.createScaledBitmap(image, width, height,true);
                                        //hangi g??rseli k??????ltece??iz, boyutlar, filtre yap??p yapmayaca????
    }
}