package com.salesforce.samples.smartsyncexplorer;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PriceBookEntryAdapter extends RecyclerView.Adapter<PriceBookEntryAdapter.MyViewHolder> {

    private List<ProductObject> moviesList;


    public class MyViewHolder extends RecyclerView.ViewHolder  {
        public TextView title;

        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tvName);
        }

    }

    public PriceBookEntryAdapter(List<ProductObject> moviesList) {
        this.moviesList = moviesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_text, parent, false);

        return new MyViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        //ProductObject movie = productlistFilter.get(position);
       /* System.out.println("list is binded "+movie.getName());
        holder.title.setText(movie.getName());*/
    }

    @Override
    public int getItemCount() {
        return moviesList.size();
    }

    public List<ProductObject> getMoviesList() {
        return moviesList;
    }

}