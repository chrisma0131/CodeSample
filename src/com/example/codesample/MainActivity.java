package com.example.codesample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class MainActivity extends Activity implements ScrollViewListener
{
    public static ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();
    private static int smCounter = 0;
    private static String nxtUrl = "";
    private static int asyncCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Start initial asyncTask with most recent list of selfies
        new LoadInstagramImages(true).execute("https://api.instagram.com/v1/tags/selfie/media/recent?type=image?access_token=1450779186.1fb234f.98c98c61ea78411b845a44c6d085aa6d&client_id=6383ca016b5344b6b55ccc44bacfc3b0");
        ScrollViewAddition scrollView = (ScrollViewAddition) findViewById(R.id.Instagram);
        scrollView.setScrollViewListener(this);

    }

    @Override
    public void onScrollChanged(ScrollViewAddition scrollView, int x, int y, int xold, int yold) {

//        Taking the last son in the scrollview
        View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
        int bottom = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

//         bottom == 0, then the bottom has been reached and if the counter == 0, start a new task
        if (bottom == 0 && asyncCounter == 0)
        {
            new LoadInstagramImages(false).execute(nxtUrl);
        }
    }

    class LoadInstagramImages extends AsyncTask<String, Bitmap, String>
    {
        ProgressDialog progressDialog;
        private WeakReference<ImageView> imageViewReference;
        boolean noProgressDialog = false;

        private LoadInstagramImages(boolean noPd)
        {
            // Create a new asyncTask and decide if there should be a progressDialog
            this.noProgressDialog = noPd;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            // show progressDialog if true
            if (noProgressDialog)
            {
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Downloading...");
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }

            // Keeping track of how many tasks are being executed for thread management
            asyncCounter++;
            Log.d("Async Inc", "" + asyncCounter);
        }

        @Override
        protected String doInBackground(String... params)
        {
            String next_url = "";
            try
            {
//                 Create URL connection to download each picture
                URL url_example = new URL(params[0]);
                URLConnection tc;
                tc = url_example.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

                String line;
                while ((line = in.readLine()) != null)
                {
//                     Get JSON data from the link, extract the data
                    JSONObject ob = new JSONObject(line);

//                     Get the Data JSON array to parse for picture urls
                    JSONArray object = ob.getJSONArray("data");
                    JSONObject paginationObject = ob.getJSONObject("pagination");

//                     Get the next url for the next download
                    next_url = paginationObject.getString("next_url");
                    Log.d("Url", next_url);

//                     Parse through the array for image links to download and as they are downloaded, load them on the screen
                    for (int i = 0; i < object.length(); i++)
                    {

                        JSONObject jo = (JSONObject) object.get(i);
                        JSONObject imagesJsonObj = jo.getJSONObject("images");

//                         Standard resolution
                        JSONObject stdResJsonObject = imagesJsonObj.getJSONObject("standard_resolution");
                        String url = stdResJsonObject.get("url").toString();
                        Bitmap bmp = downloadBitmap(url);
                        publishProgress(bmp);
//                         Log.d("url", url);
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            catch (OutOfMemoryError e)
            {
                Toast.makeText(getApplicationContext(), "Out of Memory", Toast.LENGTH_SHORT).show();
            }

            return next_url;
        }

//         Method to download files from a url in a big, small, small sorted and return as Bitmaps
        private Bitmap downloadBitmap(String url)
        {
            final AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");
            final HttpGet getRequest = new HttpGet(url);
            try
            {
                HttpResponse response = httpClient.execute(getRequest);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK){
                    Log.w("ImageDownload", "Error " + statusCode + " while retrieving bitmap from " + url);
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity != null)
                {
                    InputStream inputStream = null;
                    try
                    {
                        inputStream = entity.getContent();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        if (smCounter == 0 || smCounter == 3)
                        {
                            int width = 500;
                            int height = 500;
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                            smCounter = 0;
                        } else
                        {
                            int width = 320;
                            int height = 320;
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        }

                        smCounter++;
                        WeakReference<Bitmap> weakBitmap = new WeakReference<Bitmap>(bitmap);

                        return weakBitmap.get();
                    }
                    finally
                    {
                        if (inputStream != null)
                        {
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            }
            catch (Exception e){
                getRequest.abort();
                Log.w("ImageDownloader", "Error while retrieving bitmap from " + url);
            }
            finally{
                    httpClient.close();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final Bitmap... bitmap)
        {
            super.onProgressUpdate(bitmap);
            TableLayout tableInstagram = (TableLayout) findViewById(R.id.tableInstagram);

//             Create Imageview for picture to be placed in and set onClickListener for it
            ImageView image = new ImageView(MainActivity.this);
            imageViewReference = new WeakReference<ImageView>(image);
            ( imageViewReference.get()).setImageBitmap(bitmap[0]);
            ( imageViewReference.get()).setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
//                     When an image is clicked, create a new dialog and put the image into it
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.image_layout);
                    dialog.setTitle("#Selfie");
                    ImageView image = (ImageView) dialog.findViewById(R.id.imDisplay);
                    image.setLayoutParams(new TableRow.LayoutParams(700, 700));
                    image.setImageBitmap(bitmap[0]);
                    Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);

//                     if button is clicked, close the custom dialog
                    dialogButton.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            });

//             Format that ImageView
            TableRow.LayoutParams imageParams = new TableRow.LayoutParams();
            imageParams.setMargins(10, 10, 10, 10);
            imageParams.gravity = Gravity.CENTER;
            (imageViewReference.get()).setLayoutParams(imageParams);

//             Format the TableRow and add the ImageView to it
            TableRow tr = new TableRow(MainActivity.this);
            tr.addView((imageViewReference.get()));
            tr.setBackgroundColor(Color.parseColor("#000000"));
            TableRow.LayoutParams lp = new TableRow.LayoutParams(getResources().getDisplayMetrics().widthPixels, TableRow.LayoutParams.WRAP_CONTENT);
            tr.setLayoutParams(lp);

//             Add the TableRow to the TableLayout
            tableInstagram.addView(tr);

        }

        @Override
        protected void onPostExecute(String next_url)
        {
            if (noProgressDialog && progressDialog != null)
            {
                progressDialog.dismiss();
            }

            // Get the next url to download images from and decrement the AsyncTask counter since the task is done
            nxtUrl = next_url;
            asyncCounter--;
            Log.d("Async Dec", "" + asyncCounter);
        }
    }
}
