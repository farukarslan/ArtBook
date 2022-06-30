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

                Cursor cursor = db.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)}); //Selection argümanları

                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    artNameText.setText(cursor.getString(artNameIx));
                    painterNameText.setText(cursor.getString(painterNameIx));
                    yearText.setText(cursor.getString(yearIx));

                    //Görseli byte dizisi olarak kaydettik. Yine o şekilde alıcaz ve görsel haline getireceğiz.
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
        //Android API23 öncesinde izin isteme yoktu. Compat'da eğer sistem API23'den düşükse
        //izin istemeden çalıştırıyor. API23'den büyükse izin istiyor.

        //erişim izin verilmediyse
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else{
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //galeriyi açıp resim toplamak için
            startActivityForResult(intentToGallery, 2);
        }
    }

    //izin istendiğinde bunun sonucunda ne olacağı belirtilen metod

    @Override                                                                         // grantResults = verilen sonuçlar int dizisi şeklinde geliyor
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // İzin verildiğinde tekrar resme tıklamayı gerektirmeden direkt galeriyi açması için
        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery, 2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // İzin verilip, galeriye gittikten sonra ne olacak, görseli alıp ne yapacağız
    @Override                                                                //geri gelen veri
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null){

            Uri imageData = data.getData(); //Bize URI veriyor.(Nereye kayıtlı olduğunun yolu)

            //Unhandled exception hatası verdi. Bu hata çıkarabilir demek. O yüzden try-catch bloğu içerisine aldık.
            try {

                //yeni versiyonlar için yeni metod ile yapılacak kısım
                if (Build.VERSION.SDK_INT >= 28){
                    //URI'ı Bitmap'e çevirmek için geliştirilmiş bir sınıf
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                }else{
                    //SDK VERSION 28'den öncekilerde bu şekilde yapılıyor yeni versiyonlarda tedavülden kalktı
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

        //Göreseli veriye çevirme işlemi (byte'a çevirdikten sonra veritabanına kaydedebiliriz)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();   // outputStream = veriyi byte array şeklinde almamızı sağlıyor
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);     //dönüştürülecek görsel şekli, kalitesi belirttik
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
            MainActivity'de onCreate içerisinde veriler çekilip listeleniyor. Yeni kayıt yapıldıktan sonra bu activity
            finish yapılıp kapatıldığı için oraya geri dönüldüğünde onCreate tekrar çağırılmıyor, onResume çağırılıyor.
            Bu yüzden yeni kayıttan sonra geri döndüğümüzde o kayıt gözükmüyor. Burada intent ile diğer activity'ye
            gidersek yeni kayıtlar da gözükür fakat öbür activity'yi bitirmeden sürekli yeni açıldığından dolayı
            arkada bir sürü activity bırakır. Geri geldiğimizde teker teker onlara döneriz.
         */

        Intent intent = new Intent(AddArt.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Daha önceki bütün activity'leri kapatacak.
        startActivity(intent);

        //finish(); // aktiviteyi komple bitir. Bu sayede kayıt yapıldıktan sonra önceki aktiviteye geri dönecek

    }

    //Gerçekten görsellerin küçük olduğuna emin olmamız lazım. SQLite 1MB üstü dosyalarda çökebiliyor.
    //Bunun için yeni bir metod yazacağız
    //Resmin uzun kenarını alıp maximumSize'a eşitliyor ve kısa kenarı da resmin oranını bozmadan aynı oranda küçültüyor

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

        //Bitmap'in kendi fonksiyonunu kullanarak yeni bir bitmap oluşturduk.
        return Bitmap.createScaledBitmap(image, width, height,true);
                                        //hangi görseli küçülteceğiz, boyutlar, filtre yapıp yapmayacağı
    }
}