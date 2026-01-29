package com.example.taskmatee;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiChatActivity extends AppCompatActivity {
    private EditText etMessage;
    private ImageButton btnSend;
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini_chat);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        rvChat = findViewById(R.id.rvChat);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        // Using your original API key provided earlier
        String apikey = "AIzaSyBKK0_GPpFas5ovetj6HLfERqwJ0VKuq88";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apikey;
        RequestQueue queue = Volley.newRequestQueue(this);

        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (messageText.isEmpty()) {
                return;
            }

            addMessage(messageText, ChatMessage.TYPE_USER);
            etMessage.setText("");

            addMessage("Gemini is thinking...", ChatMessage.TYPE_GEMINI);
            int thinkingPosition = chatMessages.size() - 1;

            try {
                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                
                part.put("text", messageText);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                JsonObjectRequest req = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        requestBody,
                        response -> {
                            try {
                                String replyText = response.getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");
                                
                                chatMessages.set(thinkingPosition, new ChatMessage(replyText, ChatMessage.TYPE_GEMINI));
                                chatAdapter.notifyItemChanged(thinkingPosition);
                                rvChat.scrollToPosition(thinkingPosition);
                            } catch (JSONException e) {
                                updateError(thinkingPosition, "Parsing error. Check logcat.");
                            }
                        },
                        error -> {
                            String errorMsg = "Connection failed.";
                            if (error.networkResponse != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    JSONObject data = new JSONObject(responseBody);
                                    errorMsg = data.getJSONObject("error").getString("message");
                                } catch (Exception e) {
                                    errorMsg = "Error Code: " + error.networkResponse.statusCode;
                                }
                            }
                            updateError(thinkingPosition, errorMsg);
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> header = new HashMap<>();
                        header.put("Content-Type", "application/json");
                        return header;
                    }
                };
                queue.add(req);

            } catch (JSONException e) {
                updateError(thinkingPosition, "Request failed.");
            }
        });
    }

    private void addMessage(String text, int type) {
        chatMessages.add(new ChatMessage(text, type));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
    }

    private void updateError(int position, String error) {
        chatMessages.set(position, new ChatMessage("Error: " + error, ChatMessage.TYPE_GEMINI));
        chatAdapter.notifyItemChanged(position);
    }
}
