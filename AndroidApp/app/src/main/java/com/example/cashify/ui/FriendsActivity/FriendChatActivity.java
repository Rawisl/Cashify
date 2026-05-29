package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.workspace.ChatAdapter;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;

import java.util.ArrayList;

public class FriendChatActivity extends AppCompatActivity {
    public static final String EXTRA_FRIEND_UID = "friend_uid";
    public static final String EXTRA_FRIEND_NAME = "friend_name";
    public static final String EXTRA_FRIEND_AVATAR = "friend_avatar";

    private FriendChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private String friendUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_chat);

        friendUid = getIntent().getStringExtra(EXTRA_FRIEND_UID);
        String friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView imgFriendAvatar = findViewById(R.id.imgFriendAvatar);
        TextView tvFriendName = findViewById(R.id.tvFriendName);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        ImageButton btnSendMessage = findViewById(R.id.btnSendMessage);

        btnBack.setOnClickListener(v -> finish());
        tvFriendName.setText(friendName != null ? friendName : "Friend");
        ImageHelper.loadAvatar(friendAvatar, imgFriendAvatar, friendName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(new ArrayList<>(), null);
        rvChatMessages.setAdapter(chatAdapter);

        viewModel = new ViewModelProvider(this).get(FriendChatViewModel.class);
        viewModel.startListeningMessages(friendUid);
        viewModel.getChatMessages().observe(this, messages -> {
            chatAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                rvChatMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });
        viewModel.getLoadErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "T\u1ea3i tin nh\u1eafn th\u1ea5t b\u1ea1i");
                ToastHelper.show(this, message);
            }
        });
        viewModel.getSendErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "G\u1eedi tin nh\u1eafn th\u1ea5t b\u1ea1i");
                ToastHelper.show(this, message);
            }
        });

        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText() != null ? edtMessageInput.getText().toString() : "";
            if (text.trim().isEmpty()) return;
            viewModel.sendMessage(friendUid, text);
            edtMessageInput.setText("");
        });
    }
}
