package com.salesforce.samples.smartsyncexplorer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.MyViewHolder> implements Filterable {

    private List<AttachmentObject> moviesList;
    private List<AttachmentObject> productlistFilter;

    private static ClickListener clickListener;

    public class MyViewHolder extends RecyclerView.ViewHolder implements ClickListener, View.OnClickListener, View.OnLongClickListener {
        public TextView title;

        public MyViewHolder(View view) {
            super(view);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            title = view.findViewById(R.id.tvName);
        }

        @Override
        public void onItemClick(int position, View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }

        @Override
        public void onItemLongClick(int position, View v) {
            clickListener.onItemLongClick(getAdapterPosition(), v);
        }

        @Override
        public void onClick(View view) {
            clickListener.onItemClick(getAdapterPosition(), view);
        }

        @Override
        public boolean onLongClick(View view) {
            clickListener.onItemLongClick(getAdapterPosition(), view);
            return false;
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        AttachmentAdapter.clickListener = clickListener;
    }

    public AttachmentAdapter(List<AttachmentObject> moviesList) {
        this.moviesList = moviesList;
        this.productlistFilter = moviesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_product, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        AttachmentObject movie = productlistFilter.get(position);
        System.out.println("list is binded "+movie.getAttachmentTitle());
        holder.title.setText(movie.getAttachmentTitle());
    }

    @Override
    public int getItemCount() {
        return productlistFilter.size();
    }

    public List<AttachmentObject> getMoviesList() {
        return moviesList;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                System.out.println("performFiltering "+charString);
                if (charString.isEmpty()) {
                    productlistFilter = moviesList;
                } else {
                    List<AttachmentObject> filteredList = new ArrayList<>();
                    for (AttachmentObject row : moviesList) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }

                    productlistFilter = filteredList;
                    System.out.println("performFiltering "+productlistFilter);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = productlistFilter;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                //noinspection unchecked
                productlistFilter = (List<AttachmentObject>) filterResults.values;
                System.out.println(":= publishResults >> "+productlistFilter);
                notifyDataSetChanged();
            }
        };
    }


    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }


}