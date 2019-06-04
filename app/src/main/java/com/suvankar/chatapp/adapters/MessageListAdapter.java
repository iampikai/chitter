package com.suvankar.chatapp.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.suvankar.chatapp.models.MessageDataModel;
import com.suvankar.chatapp.models.UserDataModel;
import com.suvankar.chatapp.R;

import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 1;
    private static final int VIEW_TYPE_MESSAGE_SENT = 2;
    private static final int VIEW_TYPE_MESSAGE_OTHER = 3;

    private Context mContext;
    private UserDataModel currentUser;
    private List<MessageDataModel> mMessageList;

    public MessageListAdapter(Context context, List<MessageDataModel> messageList, UserDataModel currentUser) {
        this.mContext = context;
        this.mMessageList = messageList;
        this.currentUser = currentUser;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        MessageDataModel message = mMessageList.get(position);
        UserDataModel sender = message.getUser();

        if (sender.getEmail() != null) {

            if (sender.getEmail().equals(currentUser.getEmail())) {
                return VIEW_TYPE_MESSAGE_SENT;
            } else if (!sender.getEmail().equals(currentUser.getEmail())) {
                return VIEW_TYPE_MESSAGE_RECEIVED;
            } else {
                return VIEW_TYPE_MESSAGE_OTHER;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_message, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.received_message, parent, false);
            return new ReceivedMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_OTHER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.other_message, parent, false);
            return new OtherMessageHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageDataModel message = mMessageList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_OTHER:
                ((OtherMessageHolder) holder).bind(message);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout messageView, imageView;
        TextView messageText, nameText, nameImage;
        ImageView messageImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageView = itemView.findViewById(R.id.messageView);
            imageView = itemView.findViewById(R.id.imageView);
            nameText = itemView.findViewById(R.id.nameTextView);
            nameImage = itemView.findViewById(R.id.nameImageView);
            messageText = itemView.findViewById(R.id.messageTextView);
            messageImage = itemView.findViewById(R.id.photoImageView);
        }

        void bind(MessageDataModel message) {
            final UserDataModel sender = message.getUser();
            if (message.getPhotoUrl() == null) {
                imageView.setVisibility(View.GONE);
                messageView.setVisibility(View.VISIBLE);
                nameText.setText(sender.getName());
                messageText.setText(message.getText());
            } else {
                Glide.with(messageImage.getContext())
                        .load(message.getPhotoUrl())
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE))
                        .into(messageImage);
                messageView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                nameImage.setText(sender.getName());
            }
        }

        @Override
        public void onClick(View v) {

        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView messageText;
        ImageView messageImage;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageTextView);
            messageImage = itemView.findViewById(R.id.photoImageView);
        }

        void bind(MessageDataModel message) {
            if (message.getPhotoUrl() == null) {
                messageImage.setVisibility(View.GONE);
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getText());
            } else {
                Glide.with(messageImage.getContext())
                        .load(message.getPhotoUrl())
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE))
//                                .signature(String.valueOf(System.currentTimeMillis())))
                        .into(messageImage);
                messageText.setVisibility(View.GONE);
                messageImage.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {

        }
    }

    private class OtherMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        OtherMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageTextView);
        }

        void bind(MessageDataModel message) {
            messageText.setText(message.getText());
        }
    }
}
