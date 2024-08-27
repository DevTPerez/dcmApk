package com.example.dcmapk

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private var clientSelected: String = ""
    private var locateSelected: String = "Selecione Localização"
    private var moduleSelected: String = "Selecione o Módulo"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inicializa SharedPreferences
        val checkBoxLandscape = findViewById<CheckBox>(R.id.checkbox_landscape)
        val checkBoxPortrait = findViewById<CheckBox>(R.id.checkbox_portrait)

        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val videoViewOption = sharedPreferences.getString("videoViewOption", "")


        checkBoxLandscape.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxPortrait.isChecked = false
                editor.putString("videoViewOption", "Landscape")
                editor.apply()
            }
        }

        checkBoxPortrait.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxLandscape.isChecked = false
                editor.putString("videoViewOption", "Portrait")
                editor.apply()
            }
        }




        // Configuração da ListView de Cliente
        val listViewCliente: ListView = findViewById(R.id.listview_cliente)
        val clienteList = ClientItems.items
        val clienteAdapter = ArrayAdapter(this, R.layout.list_item_layout, clienteList)
        listViewCliente.adapter = clienteAdapter

        // Configuração da ListView de Localização
        val locateListView: ListView = findViewById(R.id.listview_localizacao)
        // Configuração da ListView de Módulo
        val moduleListView: ListView = findViewById(R.id.listview_modulo)

        listViewCliente.setOnItemClickListener { _, _, position, _ ->
            clientSelected = clienteList[position]
            listViewCliente.smoothScrollToPosition(position)
        }

        val selectClientButton = findViewById<Button>(R.id.btn_select_client)
        selectClientButton.setOnClickListener {


            when (clientSelected) {
                "widex" -> {

                    val storeItems = widexStoreItems.items
                    val storeAdapter =
                        ArrayAdapter(this, R.layout.list_item_layout, storeItems)
                    locateListView.adapter = storeAdapter

                    val screenItems = WidexScreenItems.items
                    val screenAdapter =
                        ArrayAdapter(this, R.layout.list_item_layout, screenItems)
                    moduleListView.adapter = screenAdapter
                }

                "dcmadmin" -> {

                    val storeItems = plmStoreList.items
                    val storeAdapter =
                        ArrayAdapter(this, R.layout.list_item_layout, storeItems)
                    locateListView.adapter = storeAdapter

                    val screenItems = plmScreenItems.items
                    val screenAdapter =
                        ArrayAdapter(this, R.layout.list_item_layout, screenItems)
                    moduleListView.adapter = screenAdapter
                }
            }
        }

        locateListView.setOnItemClickListener { _, _, position, _ ->
            locateSelected = locateListView.adapter.getItem(position) as String
            locateListView.smoothScrollToPosition(position)
        }

        moduleListView.setOnItemClickListener { _, _, position, _ ->
            moduleSelected = moduleListView.adapter.getItem(position) as String
            moduleListView.smoothScrollToPosition(position)
        }

        // Carregar as seleções padrão do SharedPreferences
        val (storedLocate, storedModule, storedClient) = PreferencesManager.loadPreferences(this)

        // Verificar se as seleções padrão são nulas
        if (storedLocate == null || storedModule == null || storedClient == null) {
            // Se forem nulas, definir as seleções padrão como "defaultStore" e "defaultScreen"
            locateSelected = "Selecione Localização"
            moduleSelected = "Selecione o Módulo"
            clientSelected = ""
        } else {
            // Se não forem nulas, armazenar as seleções carregadas
            locateSelected = storedLocate
            moduleSelected = storedModule
            clientSelected = storedClient
        }

        val buttonConfig = findViewById<Button>(R.id.btn_save_selection)
        buttonConfig.setOnClickListener {

            PreferencesManager.savePreferences(this, locateSelected, moduleSelected, clientSelected)
            Log.d(
                "Inicialização",
                "locateSelected: $locateSelected, moduleSelected: $moduleSelected, clientSelected: $clientSelected"
            )
            val watingIntent = Intent(this, WaitingScreenActivity::class.java)

            sendHeartbeatRequest(context = this@SettingsActivity)

            startActivity(watingIntent)
            finish()
        }
    }

    fun AppCompatActivity.setOrientation(orientation: Int) {
        requestedOrientation = orientation
    }

}
