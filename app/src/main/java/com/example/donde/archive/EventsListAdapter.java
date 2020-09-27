package com.example.donde.archive;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.donde.R;

import java.util.List;

public class EventsListAdapter extends RecyclerView.Adapter<EventsListAdapter.EventViewHolder> {
    private List<EventsListModel> eventsListModels;
    private OnEventListItemClicked onEventListItemClicked;


    public EventsListAdapter(OnEventListItemClicked onEventListItemClicked) {
        this.onEventListItemClicked = onEventListItemClicked;
    }

    public void setEventsListModels(List<EventsListModel> eventsListModels) {
        Log.e("EventListAdapter", "in setEventListModels");

        this.eventsListModels = eventsListModels;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.e("EventListAdapter", "in onCreateViewHolder");

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_event_item,
                parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Log.e("EventListAdapter", "in onBindViewHolder");

        holder.listName.setText(eventsListModels.get(position).getName());
        holder.listDesc.setText(eventsListModels.get(position).getDescription());
        holder.listLocationName.setText(eventsListModels.get(position).getLocation_name());
//        holder.listTimeStarting.setText(eventsListModels.get(position).getTime_starting().toString());
        holder.listCreatorUserName.setText(eventsListModels.get(position).getCreator_username());
    }

    @Override
    public int getItemCount() {
        Log.e("EventListAdapter", "in getItemCount");

        if (eventsListModels == null) {
            return 0;
        } else {
            return eventsListModels.size();
        }
    }

    public interface OnEventListItemClicked {
        void onItemClicked(int position);
    }

    public class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView listName;
        private TextView listDesc;
        private TextView listLocationName;
        private TextView listLocation;
        private TextView listCreatorUserName;
        private TextView listTimeCreated;
        private TextView listTimeStarting;
        private Button listGotoEvent;

        public EventViewHolder(@NonNull View itemView) {

            super(itemView);
            Log.e("EventListAdapter", "in EventViewHolder");

            listName = itemView.findViewById(R.id.list_name);
            listDesc = itemView.findViewById(R.id.list_desc);
            listLocationName = itemView.findViewById(R.id.list_location_name);
            listLocation = itemView.findViewById(R.id.list_location);
            listCreatorUserName = itemView.findViewById(R.id.list_creator_username);
            listTimeCreated = itemView.findViewById(R.id.list_time_created);
            listTimeStarting = itemView.findViewById(R.id.list_time_starting);
            listGotoEvent = itemView.findViewById(R.id.list_goto_event);

            listGotoEvent.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onEventListItemClicked.onItemClicked(getAdapterPosition());
        }
    }
}