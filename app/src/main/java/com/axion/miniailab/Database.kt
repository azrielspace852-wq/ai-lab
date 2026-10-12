package com.axion.miniailab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.util.UUID

 data class Dataset(val id: String, val name: String, val path: String, val content: String?, val size: Long, val isFolder: Boolean = false)

class DatasetDb(context: Context) : SQLiteOpenHelper(context, "mini_ai_lab.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE datasets(id TEXT PRIMARY KEY, name TEXT NOT NULL, path TEXT NOT NULL, content TEXT, content_ref TEXT, size INTEGER NOT NULL, is_folder INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX idx_datasets_path ON datasets(path)")
        db.execSQL("CREATE TABLE models(id TEXT PRIMARY KEY, name TEXT NOT NULL, file_path TEXT NOT NULL, created_at INTEGER NOT NULL, steps INTEGER NOT NULL, loss REAL NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized fun list(): List<Dataset> {
        val out = mutableListOf<Dataset>()
        readableDatabase.rawQuery("SELECT id,name,path,content,content_ref,size,is_folder FROM datasets ORDER BY path,name", null).use { c ->
            while (c.moveToNext()) out += Dataset(c.getString(0),c.getString(1),c.getString(2),if(c.isNull(3)) null else c.getString(3),c.getLong(5),c.getInt(6)!=0)
        }
        return out
    }
    @Synchronized fun add(name: String, path: String, content: String, largeFile: File? = null): Dataset {
        val id = UUID.randomUUID().toString(); val size = content.toByteArray(Charsets.UTF_8).size.toLong()
        val db = writableDatabase
        val cv = ContentValues().apply { put("id",id);put("name",name);put("path",path);put("content",if(largeFile==null) content else null);put("content_ref",largeFile?.absolutePath);put("size",size);put("is_folder",0);put("updated_at",System.currentTimeMillis()) }
        db.insertOrThrow("datasets",null,cv)
        return Dataset(id,name,path,if(largeFile==null)content else null,size)
    }
    @Synchronized fun delete(id: String) { writableDatabase.delete("datasets","id=?", arrayOf(id)) }
    @Synchronized fun get(id: String): Dataset? = list().firstOrNull { it.id==id }
    @Synchronized fun trainingText(): String = list().filter { !it.isFolder }.joinToString("\n") { d ->
        val raw = d.content ?: readableDatabase.rawQuery("SELECT content_ref FROM datasets WHERE id=?", arrayOf(d.id)).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) try { java.io.File(c.getString(0)).takeIf { it.isFile }?.readText(Charsets.UTF_8).orEmpty() } catch (_: Exception) { "" } else ""
        }
        JsonlTrainingText.extract(raw)
    }.trim()
    @Synchronized fun saveModel(id: String, name: String, file: File, steps: Int, loss: Double) {
        val cv=ContentValues().apply { put("id",id);put("name",name);put("file_path",file.absolutePath);put("created_at",System.currentTimeMillis());put("steps",steps);put("loss",loss) }
        writableDatabase.insertWithOnConflict("models",null,cv,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun models(): List<Triple<String,String,String>> { val out=mutableListOf<Triple<String,String,String>>();readableDatabase.rawQuery("SELECT id,name,file_path FROM models ORDER BY created_at DESC",null).use { c->while(c.moveToNext())out+=Triple(c.getString(0),c.getString(1),c.getString(2)) };return out }
}
