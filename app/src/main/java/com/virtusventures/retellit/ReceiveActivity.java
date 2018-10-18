package com.virtusventures.retellit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.gson.Gson;
import com.sjl.foreground.Foreground;

import java.io.InputStream;
import java.util.Collection;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import hyogeun.github.com.colorratingbarlib.ColorRatingBar;

public class ReceiveActivity extends ConnectionsActivity {

    private static final  String TAG = "ReceiveActivity";

    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID =
            "com.google.location.nearby.apps.walkietalkie.manual.SERVICE_ID";
    private String mName;

    private State mState = State.UNKNOWN;

    @BindView(R.id.alert_layout)
    RelativeLayout alertLayout;

    @BindView(R.id.left_imageview)
    ImageView productImageView;

    @BindView(R.id.rating_bar)
    ColorRatingBar ratingBar;

    @BindView(R.id.price_textview)
    TextView priceTextView;

    @BindView(R.id.description_textview)
    TextView descriptionTextView;

    @BindView(R.id.name_textview)
    TextView nameTextView;

    @BindView(R.id.background_imageview)
    ImageView backgroundImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        Foreground.init(getApplication());
        ButterKnife.bind(this);
        mName = generateRandomName();
        setState(State.DISCOVERING);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        setState(State.UNKNOWN);

    }

    @Override
    public void onBackPressed(){

//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_HOME);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);

        if (alertLayout.getVisibility() == View.VISIBLE){
            alertLayout.setVisibility(View.INVISIBLE);
        }else {
            finish();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (getIntent().hasExtra("product"))
        {
            String productStr = extras.getString("product");
            Log.d(TAG, productStr);

            Gson gson = new Gson();
            ProductModel productModel = gson.fromJson(productStr, ProductModel.class);
            setProduct(productModel);

        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.i(TAG,"onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);//needed so that getIntent() doesn't return null inside onResume()
    }

    @Override
    protected String getName() {
        return mName;
    }

    /** {@see ConnectionsActivity#getServiceId()} */
    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    /** {@see ConnectionsActivity#getStrategy()} */
    @Override
    public Strategy getStrategy() {
        return STRATEGY;
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    public enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            logW("State set to " + state + " but already in that state");
            return;
        }

        logD("State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /** @return The current state. */
    private State getState() {
        return mState;
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State oldState, State newState) {

        // Update Nearby Connections to the new state.
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovering();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    stopDiscovering();
                } else if (isAdvertising()) {
                    // Continue to advertise, so others can still connect,
                    // but clear the discover runnable.
                }
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                // no-op
                break;
        }
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        if (!isConnecting()) {
            connectToEndpoint(endpoint);
        }
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        //Toast.makeText(this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT).show();
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        //Toast.makeText(this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT).show();
        setState(State.DISCOVERING);

    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
        }
    }

    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {

        try {
            String productStr = new String(payload.asBytes(), "UTF-8");
            Gson gson = new Gson();
            ProductModel productModel = gson.fromJson(productStr, ProductModel.class);
            Log.d(TAG ,productStr);
            if (Foreground.get().isForeground()) {
                setProduct(productModel);
            }else{
                showNotiifcation(productStr);
            }
        }
        catch (Exception e){

        }
    }

    private void setProduct(ProductModel productModel){

        Drawable drawable = loadDrawableFromAssets(this ,productModel.product_image);
        productImageView.setImageDrawable(drawable);
        nameTextView.setText(productModel.productname);
        priceTextView.setText(productModel.price);
        descriptionTextView.setText(productModel.long_description);
        ratingBar.setRating(productModel.star);
        backgroundImageView.setVisibility(View.INVISIBLE);
    }

    private Drawable loadDrawableFromAssets(Context context, String path)
    {
        InputStream stream = null;
        try
        {
            stream = context.getAssets().open(path);
            return Drawable.createFromStream(stream, null);
        }
        catch (Exception ignored) {} finally
        {
            try
            {
                if(stream != null)
                {
                    stream.close();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void showNotiifcation(String productStr)
    {
        //ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC ,100);
        //toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT ,300);

        Intent intent = new Intent(this, ReceiveActivity.class);
        intent.putExtra("product" ,productStr);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        int notificationId = 102;

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.O) {

            String channelId = "channel-retellit";
            String channelName = "Retellit";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(ReceiveActivity.this ,channelId)
                    .setContentIntent(pi)
                    .setChannelId(channelId)
                    .setContentTitle("Retellit.com | Now")
                    .setContentText("Softsoap Has been added to your Retail.com cart. Go to Retail.com app.")
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setAutoCancel(true)
                    .build();
        }
        else{

            notification = new Notification.Builder(ReceiveActivity.this)
                    .setContentIntent(pi)
                    .setContentTitle("Retellit.com | Now")
                    .setContentText("Softsoap Has been added to your Retail.com cart. Go to Retail.com app.")
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setAutoCancel(true)
                    .build();

        }

        try {
            notificationManager.notify(notificationId, notification);
        }
        catch (NullPointerException e){
            Log.e("tag" ,e.getLocalizedMessage());
        }
    }

    public void onOrder(View view)
    {
        alertLayout.setVisibility(View.VISIBLE);
    }
}
