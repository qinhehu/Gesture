package com.example.qinhe.gesture;

import android.app.Application;
import android.content.res.Resources;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by QinHe on 2017/2/20.
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.imageView.setImageResource(R.drawable.default_user);
        holder.unReadView.setVisibility(View.VISIBLE);
        holder.content.setText("Test "+position);
    }

    @Override
    public int getItemCount() {
        return 30;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final View unReadView;
        private final TextView content;
//        private final CstSideslip cstSideslip;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image);
            unReadView = itemView.findViewById(R.id.view_unread);
            content = (TextView) itemView.findViewById(R.id.txt_content);
//            cstSideslip = (CstSideslip) itemView.findViewById(R.id.sideslip);
//            cstSideslip.setMobiListListener(new IOnListListener() {
//                @Override
//                public void onRead() {
//                    if(unReadView.getVisibility() == View.VISIBLE){
//                        unReadView.setVisibility(View.GONE);
//                    }else{
//                        unReadView.setVisibility(View.VISIBLE);
//                    }
//                }
//
//                @Override
//                public void onDelete() {
//
//                }
//            });
        }
    }
}
