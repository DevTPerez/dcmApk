package com.example.dcmapk

import android.os.Environment
import android.util.Log
import java.io.File

fun deleteFilesFromMoviesDirectory() {
    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)

    if (moviesDir.exists() && moviesDir.isDirectory) {
        moviesDir.listFiles()?.forEach { file ->
            deleteRecursive(file)
        }
    } else {
        Log.d("DeleteFile", "Movies directory does not exist or is not a directory.")
    }
}

fun deleteRecursive(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        fileOrDirectory.listFiles()?.forEach { child ->
            deleteRecursive(child)
        }
    }
    val deleted = fileOrDirectory.delete()
    Log.d("DeleteFile", "File/Directory ${fileOrDirectory.name} deleted: $deleted")
}