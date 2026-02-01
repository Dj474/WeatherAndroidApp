package com.example.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {

    private Context context;
    private List<WeatherItem> items;
    private boolean isHourly;

    public WeatherAdapter(Context context, List<WeatherItem> items, boolean isHourly) {
        this.context = context;
        this.items = items;
        this.isHourly = isHourly;
    }

    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;

        if (isHourly) {
            view = inflater.inflate(R.layout.item_hourly_weather, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_daily_weather, parent, false);
        }

        return new WeatherViewHolder(view, isHourly);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        WeatherItem item = items.get(position);

        holder.timeTextView.setText(item.getTime());
        holder.tempTextView.setText(String.format("%.0f°C", item.getTemperature()));
        holder.descriptionTextView.setText(item.getDescription());
        holder.iconImageView.setImageResource(item.getIconResId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView, tempTextView, descriptionTextView;
        ImageView iconImageView;

        public WeatherViewHolder(@NonNull View itemView, boolean isHourly) {
            super(itemView);

            if (isHourly) {
                timeTextView = itemView.findViewById(R.id.hourTime);
                tempTextView = itemView.findViewById(R.id.hourTemp);
                descriptionTextView = itemView.findViewById(R.id.hourDescription);
                iconImageView = itemView.findViewById(R.id.hourIcon);
            } else {
                timeTextView = itemView.findViewById(R.id.dayName);
                tempTextView = itemView.findViewById(R.id.dayTemp);
                descriptionTextView = itemView.findViewById(R.id.dayDescription);
                iconImageView = itemView.findViewById(R.id.dayIcon);
            }
        }
    }
}