package com.example.hikescape;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FavoriteRouteAdapter extends RecyclerView.Adapter<FavoriteRouteAdapter.ViewHolder> {

    private final List<FavoriteRoute> favoriteRoutes;

    public FavoriteRouteAdapter(List<FavoriteRoute> favoriteRoutes) {
        this.favoriteRoutes = favoriteRoutes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteRoute favoriteRoute = favoriteRoutes.get(position);

        holder.usernameTextView.setText(favoriteRoute.getUsername());
        holder.routeNameTextView.setText(favoriteRoute.getRouteName());

        // Aquí puedes cambiar las imágenes según los datos, si es necesario.
        holder.profileImageView.setImageResource(R.drawable.perfil);
        holder.locationIconImageView.setImageResource(R.drawable.location_icon);
    }

    @Override
    public int getItemCount() {
        return favoriteRoutes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImageView;
        TextView usernameTextView;
        TextView routeNameTextView;
        ImageView locationIconImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            routeNameTextView = itemView.findViewById(R.id.routeNameTextView);
            locationIconImageView = itemView.findViewById(R.id.locationIconImageView);
        }
    }
}
