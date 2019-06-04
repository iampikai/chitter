package com.suvankar.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.suvankar.chatapp.ChatActivity;
import com.suvankar.chatapp.models.UserDataModel;
import com.suvankar.chatapp.R;

import java.util.List;

public class ThreadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<UserDataModel> users;
    Context context;

    public ThreadListAdapter(Context context, List<UserDataModel> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.thread_list_item, viewGroup, false);
        return new UserHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((UserHolder) viewHolder).bind(users.get(i));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private class UserHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.user_name);
            itemView.setOnClickListener(this);
        }

        void bind(UserDataModel user) {
            textView.setText(user.getName());
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            Intent intent = new Intent(context, ChatActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(ChatActivity.RECIPIENT_NAME, users.get(position).getName());
            bundle.putString(ChatActivity.RECIPIENT_ID, users.get(position).getId());
            intent.putExtras(bundle);
            context.startActivity(intent);
        }
    }
}