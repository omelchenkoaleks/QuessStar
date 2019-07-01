package com.omelchenkoaleks.quessstar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.autofill.FieldClassification;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private String mUrlConnection = "http://www.posh24.se/kandisar";

    private Button mButton_0;
    private Button mButton_1;
    private Button mButton_2;
    private Button mButton_3;

    private ImageView mSrarImageView;

    private ArrayList<String> mUrls;
    private ArrayList<String> mNames;
    private ArrayList<Button> mButtons;

    private int mNumberOfQuestion;
    private int mNumberOfRightAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton_0 = findViewById(R.id.button_0);
        mButton_1 = findViewById(R.id.button_1);
        mButton_2 = findViewById(R.id.button_2);
        mButton_3 = findViewById(R.id.button_3);
        mButtons = new ArrayList<>();
        mButtons.add(mButton_0);
        mButtons.add(mButton_1);
        mButtons.add(mButton_2);
        mButtons.add(mButton_3);

        mSrarImageView = findViewById(R.id.star_image_view);

        mUrls = new ArrayList<>();
        mNames = new ArrayList<>();

        getContent();

        playGame();
    }

    public void onClickAnswer(View view) {
        Button button = (Button) view;
        String tag = button.getTag().toString();
        if (Integer.parseInt(tag) == mNumberOfRightAnswer) {
            Toast.makeText(this, "Верно", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "Неверно! Правильный ответ: " + mNames.get(mNumberOfQuestion),
                    Toast.LENGTH_SHORT).show();
        }

        playGame();
    }

    private void playGame() {
        generateQuestion();
        DownloadImageTask task = new DownloadImageTask();
        try {
            Bitmap bitmap = task.execute(mUrls.get(mNumberOfQuestion)).get();
            if (bitmap != null) {
                mSrarImageView.setImageBitmap(bitmap);
                // нужен цикл, чтобы кнопкам указать текст:
                for (int i = 0; i < mButtons.size(); i++) {
                    if (i == mNumberOfRightAnswer) {
                        mButtons.get(i).setText(mNames.get(mNumberOfQuestion));
                    } else {
                        int wrongAnswer = generateWrongAnswer();
                        mButtons.get(i).setText(mNames.get(wrongAnswer));
                    }
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void generateQuestion() {
        mNumberOfQuestion = (int) (Math.random() * mNames.size());
        // указываем на какой кнопке будет неправильный ответ:
        mNumberOfRightAnswer = (int) (Math.random() * mButtons.size());
    }

    private int generateWrongAnswer() {
        return (int) (Math.random() * mNames.size());
    }

    private void getContent() {
        DownloadContentTask contentTask = new DownloadContentTask();
        try {
            String content = contentTask.execute(mUrlConnection).get();

            /* ---------- SPLIT CONTENT ---------- */

            String start = "<p class=\"link\">Topp 100 kändisar</p>";
            String finish = "<div class=\"col-xs-12 col-sm-6 col-md-4\">";
            Pattern pattern = Pattern.compile(start + "(.*?)" + finish);
            Matcher matcher = pattern.matcher(content);
            String splitContent = "";
            while (matcher.find()) {
                splitContent = matcher.group(1);
            }

            /* ТЕПЕРЬ НУЖНО ПОЛУЧИТЬ ДВА МАССИВА:
                   1, БУДЕТ ХРАНИТЬ ИМЕНА
                         2, БУДЕТ ХРАНИТЬ КАРТИНКИ */
            Pattern patternImage = Pattern.compile("<img src=\"(.*?)\"");
            Pattern patternName = Pattern.compile("alt=\"(.*?)\"/>");
            Matcher matcherImage = patternImage.matcher(splitContent);
            Matcher matcherName = patternName.matcher(splitContent);

            while (matcherImage.find()) {
                mUrls.add(matcherImage.group(1));
            }

            while (matcherName.find()) {
                mNames.add(matcherName.group(1));
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class DownloadContentTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder result = new StringBuilder();
            URL url = null;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    result.append(line);
                    line = bufferedReader.readLine();
                }
                return result.toString();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url = null;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                return bitmap;
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }
    }
}
