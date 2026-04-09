package com.example.cashify.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.model.LegendItem;

import java.util.ArrayList;
import java.util.List;

public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.ViewHolder> {

    private List<LegendItem> list = new ArrayList<>();

    //interface để nhận diện click vô legend
    public interface OnItemClickListener {
        void onItemClick(LegendItem item);
    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<LegendItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chart_legend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LegendItem item = list.get(position);

        holder.tvName.setText(item.getName());
        holder.tvAmount.setText(item.getFormattedAmount());

        GradientDrawable bgShape = (GradientDrawable) holder.colorCircle.getBackground();
        if (bgShape != null) {
            bgShape.setColor(item.getColor());
        }

        //Gắn sự kiện click vào cả cái dòng đó
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View colorCircle;
        TextView tvName, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            tvName = itemView.findViewById(R.id.tv_category_legend_name);
            tvAmount = itemView.findViewById(R.id.tv_legend_money_amount);
        }
    }
}