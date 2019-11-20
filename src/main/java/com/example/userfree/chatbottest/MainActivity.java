package com.example.userfree.chatbottest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static java.util.Locale.US;

public class MainActivity extends AppCompatActivity implements AIListener {

    RecyclerView recyclerView;
    EditText editText;
    RelativeLayout addBtn;
    DatabaseReference ref;
    FirebaseRecyclerAdapter<ChatMessage, chat_rec> adapter;
    Boolean flagFab = true;
    TextToSpeech toSpeech;
    int result;
    private static final int REQUEST_SPEECH = 0;
    private AIService aiService;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> matches = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (matches.size() != 0) {
                    int mostLikelyThingHeard = matches.lastIndexOf(data);


                    if (String.valueOf(mostLikelyThingHeard).equals("go second activity")) {

                        startActivity(new Intent(MainActivity.this, Main2Activity.class));

                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    public void btnGo() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, Main2Activity.class);
        startActivityForResult(intent, REQUEST_SPEECH);

    }


    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        recyclerView = findViewById(R.id.recyclerView);
        editText = findViewById(R.id.editText);
        addBtn = findViewById(R.id.addBtn);

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);

        final AIConfiguration config = new AIConfiguration("0f51b6b06cf3454a80c2cf762957ef2a",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);


        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();

        toSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    result = toSpeech.setLanguage(US);
                    if (aiService.equals("are you there?")){
                        aiService.startListening();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Feature not supported in your device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ChatMessage chatMessage12 = new ChatMessage();


        aiService.startListening();
        aiService.setListener(this);


        addBtn.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString().trim();

                if (!message.equals("")) {

                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    ref.child("chat").push().setValue(chatMessage);

                    aiRequest.setQuery(message);
                    new AsyncTask<AIRequest, Void, AIResponse>() {

                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(AIResponse response) {
                            if (response != null) {

                                Result result = response.getResult();
                                String reply = result.getFulfillment().getSpeech();
                                ChatMessage chatMessage = new ChatMessage(reply, "bot");
                                ref.child("chat").push().setValue(chatMessage);
                            }
                        }
                    }.execute(aiRequest);
                } else {
                    aiService.startListening();


                }

                editText.setText("");


            }
        });


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = (ImageView) findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mic_white_24dp);


                if (s.toString().trim().length() != 0 && flagFab) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img);
                    flagFab = false;

                } else if (s.toString().trim().length() == 0) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, img1);
                    flagFab = true;

                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>
                (ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chat")) {
            @Override
            protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {

                if (model.getMsgUser().equals("user")) {


                    viewHolder.rightText.setText(model.getMsgText());

                    viewHolder.rightText.setVisibility(View.VISIBLE);
                    viewHolder.leftText.setVisibility(View.GONE);
                } else {
                    viewHolder.leftText.setText(model.getMsgText());

                    viewHolder.rightText.setVisibility(View.GONE);
                    viewHolder.leftText.setVisibility(View.VISIBLE);
                }
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);

                }

            }
        });

        recyclerView.setAdapter(adapter);


    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {


        final Result result = response.getResult();

        String message = result.getResolvedQuery();
        ChatMessage chatMessage0 = new ChatMessage(message, "user");
        ref.child("chat").push().setValue(chatMessage0);


        if (chatMessage0.getMsgText().equals("camera")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.lenovo.scg");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("compass")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.lenovo.compass");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("timer")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.george.eggtimer");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("file manager")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.lenovo.FileBrowser");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("email")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("second camera")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.hp.printercontrol");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("Instagram")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("open map")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("open yourself")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.example.userfree.chatbottest");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("send sms")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.messaging");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("settings")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.settings");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("Instagram")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("What's up Mobile application")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("gr.cosmote.whatsup");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("Youtube")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            startActivity(intent);
        }
        if (chatMessage0.getMsgText().equals("open the light") || chatMessage0.getMsgText().equals("light on")
                || chatMessage0.getMsgText().equals("open the lights") || chatMessage0.getMsgText().equals("lights on") ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = null; // Usually back camera is at 0 position.
                try {
                    assert camManager != null;
                    cameraId = camManager.getCameraIdList()[0];
                    camManager.setTorchMode(cameraId, true);   //Turn ON
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        if (chatMessage0.getMsgText().equals("close the light")  || chatMessage0.getMsgText().equals("close the lights")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = null; // Usually back camera is at 0 position.
                try {
                    assert camManager != null;
                    cameraId = camManager.getCameraIdList()[0];
                    camManager.setTorchMode(cameraId, false);   //Turn OFF
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        if (chatMessage0.getMsgText().equals("open Facebook")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.facebook.katana");
                startActivity(intent);
        }

        if (chatMessage0.getMsgText().equals("calendar")) {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.providers.calendar");
            if (intent != null) {
                startActivity(intent);

            } else {
                Toast.makeText(getApplicationContext(), "no app found", Toast.LENGTH_SHORT).show();
            }
        }


        final String reply = result.getFulfillment().getSpeech();
        final ChatMessage chatMessage = new ChatMessage(reply, "bot");
        ref.child("chat").push().setValue(chatMessage);

        toSpeech.speak(String.valueOf(reply), TextToSpeech.QUEUE_FLUSH, null, null);

       synchronized (aiService){
           try {
               aiService.wait(3000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }aiService.startListening();

    }


    @Override
    public void onError(ai.api.model.AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {


    }


}
