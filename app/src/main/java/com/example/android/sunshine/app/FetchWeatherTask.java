package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private static final String APPID_PARAM = "APPID";
    private static final String OPEN_WEATHER_API_KEY = BuildConfig.OPEN_WEATHER_MAP_API_KEY;
    private static final String FORECAST_URLBASE = "http://api.openweathermap.org/data/2.5/forecast/daily";
    private static final String QUERY_PARAM = "q";
    private static final String MODE_PARAM = "mode";
    private static final String UNITS_PARAM = "units";
    private static final String NUMDAYS_PARAM = "cnt";
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    @Override
    protected void onPostExecute(String[] result) {
        if(result != null){
            ForecastFragment.mForecastAdapter.clear();
            for(String res : result){
                ForecastFragment.mForecastAdapter.add(res);
            }
        }
    }

    @Override
    protected String[] doInBackground(String... params) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String[] forecast = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String query = params[0];
            final String mode = "json";
            final String unit = "metric";
            final Integer numDays = 7;
            Uri uri = Uri.parse(FORECAST_URLBASE).buildUpon().
                    appendQueryParameter(QUERY_PARAM, query).
                    appendQueryParameter(MODE_PARAM, mode).
                    appendQueryParameter(UNITS_PARAM, unit).
                    appendQueryParameter(NUMDAYS_PARAM, numDays.toString()).
                    appendQueryParameter(APPID_PARAM, OPEN_WEATHER_API_KEY)
                    .build();

            Log.v(LOG_TAG, "Built Uri" + uri.toString());

            URL url = new URL(uri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            try {
                return forecast = new WeatherDataParser().getWeatherDataFromJson(buffer.toString(), numDays);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }
}
