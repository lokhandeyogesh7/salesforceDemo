package com.salesforce.samples.smartsyncexplorer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class VisitReportAdapter extends RecyclerView.Adapter<VisitReportAdapter.MyViewHolder> implements Filterable {

    private List<VisitReportObject> moviesList;
    private List<VisitReportObject> productlistFilter;

    private static ProductListAdapter.ClickListener clickListener;

    public class MyViewHolder extends RecyclerView.ViewHolder implements VisitReportAdapter.ClickListener, View.OnClickListener, View.OnLongClickListener {
        public TextView tvVrExpenses, tvVrName, tvVrSubject, tvVrPlan, tvVrStatus;
        ImageView local;

        public MyViewHolder(View view) {
            super(view);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            tvVrName = view.findViewById(R.id.tvVrName);
            tvVrExpenses = view.findViewById(R.id.tvVrExpenses);
            tvVrSubject = view.findViewById(R.id.tvVrSubject);
            tvVrPlan = view.findViewById(R.id.tvVrPlan);
            tvVrStatus = view.findViewById(R.id.tvVrStatus);
            local = view.findViewById(R.id.ivLocal);
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

    public void setOnItemClickListener(VisitReportAdapter.ClickListener clickListener) {
        clickListener = clickListener;
    }

    public VisitReportAdapter(List<VisitReportObject> moviesList) {
        this.moviesList = moviesList;
        this.productlistFilter = moviesList;
    }

    @Override
    public VisitReportAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_visit_report, parent, false);

        return new VisitReportAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(VisitReportAdapter.MyViewHolder holder, int position) {
        VisitReportObject movie = productlistFilter.get(position);
        System.out.println("list is binded " + movie.getName());
        System.out.println("list is locally or not  " + movie.isLocallyCreated());
        if (movie.isLocallyCreated()) {
            holder.local.setVisibility(View.VISIBLE);
        }else {
            holder.local.setVisibility(View.GONE);
        }
        holder.tvVrName.setText("Visit Report ID : " + movie.getName());
        holder.tvVrExpenses.setText("Expenses : " + movie.getvRExpenses());
        holder.tvVrPlan.setText("Plan : " + movie.getvRRelatedPlan());
        holder.tvVrStatus.setText("Status : " + movie.getvRStatus());
        holder.tvVrSubject.setText("Subject : " + movie.getvRSubject());
    }

    @Override
    public int getItemCount() {
        return productlistFilter.size();
    }

    public List<VisitReportObject> getMoviesList() {
        return moviesList;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                System.out.println("performFiltering " + charString);
                if (charString.isEmpty()) {
                    productlistFilter = moviesList;
                } else {
                    List<VisitReportObject> filteredList = new ArrayList<>();
                    for (VisitReportObject row : moviesList) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }

                    productlistFilter = filteredList;
                    System.out.println("performFiltering " + productlistFilter);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = productlistFilter;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                //noinspection unchecked
                productlistFilter = (List<VisitReportObject>) filterResults.values;
                System.out.println(":= publishResults visitreport>> " + productlistFilter);
                notifyDataSetChanged();
            }
        };
    }


    public interface ClickListener {
        void onItemClick(int position, View v);

        void onItemLongClick(int position, View v);
    }
}
