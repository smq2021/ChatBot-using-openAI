package com.example.gptapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    public static final MediaType JSON = MediaType.get("application/json");

    OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setting up recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);


        sendButton.setOnClickListener((v) -> {

            String question = messageEditText.getText().toString().trim();
            addToChat(question,Message.sent_by_me);
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
            if (question.isEmpty()) {
                return;
            }
            messageEditText.setText("");



        });

    }

    void addToChat (String message, String sentBy){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());

            }
        });

    }

    void addResponse (String response){
        messageList.remove(messageList.size()-1);
        addToChat(response, Message.sent_by_bot);
    }

    void callAPI (String question){
        JSONObject jsonBody = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        JSONObject userMessage = new JSONObject();
        messageList.add(new Message("Typing... ", Message.sent_by_bot));
        try {
            jsonBody.put("temperature", 0);
              jsonBody.put("model", "gpt-4o-mini");
              systemMessage.put("role", "system");
              systemMessage.put("content", "You answer simple questions");
              userMessage.put("role", "user");
              userMessage.put("content", question);
            jsonArray.put(systemMessage);
            jsonArray.put(userMessage);

             jsonBody.put("messages", jsonArray);



        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

                Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("authorization", "API_KEY")
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                .build();



        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                addResponse("Failed to load response due to "+e.getMessage());

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
//                    JSONObject jsonObject = null;
                    String responseBody = response.body().string();
                    try {

                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray choices = jsonObject.getJSONArray("choices");

                        // Assuming you're only interested in the first choice
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");

                        // Get the content of the assistant's response
                        String result = message.getString("content");
                        addResponse(result.trim());


                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }


                }else{
                    addResponse("Failed to load response due to "+response.body().string());
                }

            }
        });


    }

}