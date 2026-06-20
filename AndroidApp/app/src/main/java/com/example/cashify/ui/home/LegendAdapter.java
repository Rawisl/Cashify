package com.example.cashify.ui.home;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.LegendItem;

import java.util.ArrayList;
import java.util.List;

public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.ViewHolder> {

    private List<LegendItem> list = new ArrayList<>();
    private OnItemClickListener listener;

    // Interface to handle legend item click events
    public interface OnItemClickListener {
        void onItemClick(LegendItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<LegendItem> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
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

        // Update the color of the circular indicator dynamically
        if (holder.colorCircle.getBackground() instanceof GradientDrawable) {
            GradientDrawable bgShape = (GradientDrawable) holder.colorCircle.getBackground();
            bgShape.setColor(item.getColor());
        }

        // Attach click listener to the entire row
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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