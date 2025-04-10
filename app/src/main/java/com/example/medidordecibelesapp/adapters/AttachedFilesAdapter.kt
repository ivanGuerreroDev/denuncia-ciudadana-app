package com.example.medidordecibelesapp.adapters

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medidordecibelesapp.R
import java.text.DecimalFormat

class AttachedFilesAdapter(
    private val context: Context,
    private val onDeleteClickListener: (Int, Uri) -> Unit
) : RecyclerView.Adapter<AttachedFilesAdapter.FileViewHolder>() {

    private val filesList = mutableListOf<Uri>()

    fun addFiles(files: List<Uri>) {
        val startPosition = filesList.size
        filesList.addAll(files)
        notifyItemRangeInserted(startPosition, files.size)
    }

    fun removeFile(position: Int) {
        filesList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, filesList.size - position)
    }

    fun removeFile(uri: Uri) {
        val position = filesList.indexOf(uri)
        if (position != -1) {
            removeFile(position)
        }
    }

    fun getFiles(): List<Uri> = filesList.toList()

    fun clear() {
        val size = filesList.size
        filesList.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attached_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileUri = filesList[position]
        holder.bind(fileUri, position)
    }

    override fun getItemCount(): Int = filesList.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileTypeIcon: ImageView = itemView.findViewById(R.id.fileTypeIcon)
        private val fileName: TextView = itemView.findViewById(R.id.fileName)
        private val fileSize: TextView = itemView.findViewById(R.id.fileSize)
        private val btnDeleteFile: ImageButton = itemView.findViewById(R.id.btnDeleteFile)

        fun bind(fileUri: Uri, position: Int) {
            // Obtener nombre del archivo
            val name = getFileName(fileUri)
            fileName.text = name

            // Obtener tamaño del archivo
            val size = getFileSize(fileUri)
            fileSize.text = formatFileSize(size)

            // Establecer icono según el tipo de archivo
            fileTypeIcon.setImageResource(getFileTypeIcon(name))

            // Configurar botón de eliminación
            btnDeleteFile.setOnClickListener {
                onDeleteClickListener(position, fileUri)
            }
        }

        private fun getFileName(uri: Uri): String {
            var result: String? = null
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (columnIndex != -1) {
                            result = cursor.getString(columnIndex)
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != -1 && cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
            return result ?: "Archivo desconocido"
        }

        private fun getFileSize(uri: Uri): Long {
            var size: Long = 0
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
                
                // Si no se pudo obtener el tamaño de los metadatos, intentar abriendo un stream
                if (size == 0L) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        size = inputStream.available().toLong()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return size
        }

        private fun formatFileSize(size: Long): String {
            if (size <= 0) return "Desconocido"
            
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            
            if (digitGroups > units.size - 1) {
                digitGroups = units.size - 1
            }
            
            val formatter = DecimalFormat("#,##0.#")
            return formatter.format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }

        private fun getFileTypeIcon(fileName: String): Int {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            
            return when {
                // Imágenes
                listOf("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension) -> 
                    android.R.drawable.ic_menu_gallery
                
                // Documentos
                listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf").contains(extension) -> 
                    android.R.drawable.ic_menu_edit
                
                // Audio
                listOf("mp3", "wav", "ogg", "flac", "aac", "m4a").contains(extension) -> 
                    android.R.drawable.ic_lock_silent_mode_off
                
                // Video
                listOf("mp4", "3gp", "mkv", "avi", "mov", "wmv").contains(extension) -> 
                    android.R.drawable.ic_media_play
                
                // Valor por defecto para otros tipos de archivo
                else -> android.R.drawable.ic_dialog_info
            }
        }
    }
}