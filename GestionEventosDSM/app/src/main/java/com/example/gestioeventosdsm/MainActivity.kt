package com.example.gestioeventosdsm

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.gestioeventosdsm.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    // Holds configuration for the Navigation Component (top-level destinations, etc.)
    private lateinit var appBarConfiguration: AppBarConfiguration

    // ViewBinding for accessing views in activity_main.xml
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the custom toolbar as the ActionBar
        setSupportActionBar(binding.toolbar)

        // Retrieve NavController from the NavHostFragment
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configure ActionBar to work with the NavController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Floating Action Button listener (placeholder action)
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                // Keeps the Snackbar above the FAB
                .setAnchorView(R.id.fab)
                .show()
        }
    }

    // Inflate the Activity's menu (toolbar menu)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handle menu item selections
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true  // Placeholder action
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle Up navigation (ActionBar back button)
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
