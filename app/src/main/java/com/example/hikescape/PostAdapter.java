package com.example.hikescape;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private DatabaseHelper databaseHelper;
    private Context context;
    private boolean isProfile; // Nuevo campo para indicar si estamos en el perfil

    // Modifica el constructor para aceptar el parámetro isProfile
    public PostAdapter(List<Post> postList, Context context, boolean isProfile) {
        this.postList = postList;
        this.databaseHelper = new DatabaseHelper(context);
        this.context = context;
        this.isProfile = isProfile; // Asignar el valor del parámetro
    }


    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar la vista de cada ítem del RecyclerView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Configurar datos dinámicos
        holder.userNameTextView.setText(post.getUserName());
        holder.postName.setText(post.getPostName());
        holder.postDescription.setText(post.getPostDescription());

        // Instancia de FireStoreHelper
        FireStoreHelper fireStoreHelper = new FireStoreHelper();

        // Obtener el username del post
        String username = post.getUserName();

        // Obtener la URL de la imagen de perfil desde Firestore
        fireStoreHelper.getProfileImageUrl(username, imageUrl -> {
            Glide.with(context)
                    .load(imageUrl != null ? imageUrl : R.drawable.perfil) // Si no hay URL, usa imagen por defecto
                    .placeholder(R.drawable.perfil)
                    .circleCrop()
                    .into(holder.profileImageView);
        });

        // Mostrar/ocultar botón de menú según la pantalla (perfil o feed)
        holder.menuButton.setVisibility(isProfile ? View.VISIBLE : View.GONE);

        // Cargar la imagen de la publicación
        String imageUri = post.getImageUri();
        Glide.with(holder.itemView.getContext())
                .load(imageUri != null && !imageUri.isEmpty() ? imageUri : R.drawable.ruta1)
                .placeholder(R.drawable.ruta1)
                .error(R.drawable.ruta2)
                .into(holder.imageView);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("PostAdapter", "Usuario no autenticado");
            return;
        }

// Obtén el nombre de usuario del usuario autenticado
        String userName = user.getDisplayName(); // IMPORTANTE: Asegúrate de que el usuario tenga este campo en Firebase Auth
        String routeName = post.getPostName(); // La ruta se identifica por su nombre

// Verificar si el usuario ya ha dado like
        fireStoreHelper.hasUserLikedRoute(routeName, userName, isLiked -> {
            holder.likeIcon.setImageResource(isLiked ? R.drawable.like_red : R.drawable.like);

            holder.likeIcon.setOnClickListener(v -> {
                if (isLiked) {
                    fireStoreHelper.unlikeRoute(routeName, userName, success -> {
                        if (success) {
                            holder.likeIcon.setImageResource(R.drawable.like);
                            post.setLiked(false);
                            post.decrementLikeCount();
                            Toast.makeText(v.getContext(), "Ya no te gusta esta ruta", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(v.getContext(), "Error al quitar el me gusta", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    fireStoreHelper.likeRoute(routeName, userName, success -> {
                        if (success) {
                            holder.likeIcon.setImageResource(R.drawable.like_red);
                            post.setLiked(true);
                            post.incrementLikeCount();
                            Toast.makeText(v.getContext(), "¡Te gusta esta ruta!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(v.getContext(), "Error al dar me gusta", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });




        // Configuración del ícono de comentario
        holder.commentIcon.setOnClickListener(v -> showCommentDialog(holder.itemView.getContext(), post));

        // Configurar el listener para el botón de tres puntos (eliminar publicación)
        holder.menuButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Eliminar publicación")
                    .setMessage("¿Estás seguro de que deseas eliminar esta publicación?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Obtener los valores necesarios del objeto Post
                        String routeDescription = post.getPostDescription();  // Usamos el método getPostDescription() de Post
                        String routeName2 = post.getPostName();                // Usamos el método getPostName() de Post

                        // Llamamos a FirestoreHelper para eliminar la ruta
                        fireStoreHelper.deleteRoute(routeDescription, routeName2, isSuccess -> {
                            if (isSuccess) {
                                postList.remove(position);
                                notifyItemRemoved(position);  // Actualizar el RecyclerView
                                Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Error al eliminar la publicación", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

    // Configuración del ícono de descarga
        holder.downloadIcon.setOnClickListener(v -> {
            // Guardar el PDF
            PDFGenerator.createPdf(post, v.getContext());

            // Abrir el PDF
            PDFGenerator.openPdf(v.getContext(), post.getPostName());

            // Mostrar un mensaje de éxito
            Toast.makeText(v.getContext(), "Ruta exportada a PDF", Toast.LENGTH_SHORT).show();
        });



    }

    private void showCommentDialog(Context context, Post post) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comment, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        ImageView postImageView = dialogView.findViewById(R.id.commentPostImageView);
        EditText commentEditText = dialogView.findViewById(R.id.commentEditText);
        Button postCommentButton = dialogView.findViewById(R.id.postCommentButton);

        Glide.with(context)
                .load(post.getImageUri())
                .placeholder(R.drawable.ruta1)
                .error(R.drawable.ruta2)
                .into(postImageView);

        postCommentButton.setOnClickListener(v -> {
            String comment = commentEditText.getText().toString().trim();
            if (!comment.isEmpty()) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                int userId = sharedPreferences.getInt("userId", -1);
                if (userId != -1) {
                    if (databaseHelper.insertComentario(post.getPostId(), userId, comment)) {
                        Toast.makeText(context, "Comentario agregado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, "Error al agregar comentario", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Por favor, escribe un comentario", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        TextView postName;
        TextView postDescription;
        ImageView saveIcon;
        ImageView imageView;
        ImageView likeIcon;
        ImageView commentIcon;
        ImageView profileImageView; // Foto de perfil
        ImageView menuButton; // Botón de tres puntos (eliminar publicación)
        ImageView downloadIcon;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            postName = itemView.findViewById(R.id.postName);
            postDescription = itemView.findViewById(R.id.postDescription);
            imageView = itemView.findViewById(R.id.postImageView);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            saveIcon = itemView.findViewById(R.id.saveIcon);
            commentIcon = itemView.findViewById(R.id.commentIcon);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            menuButton = itemView.findViewById(R.id.menuButton); // Inicializar el botón de tres puntos
            downloadIcon = itemView.findViewById(R.id.downloadIcon);

        }
    }

}
