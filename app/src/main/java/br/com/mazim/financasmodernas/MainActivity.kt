package br.com.mazim.financasmodernas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.mazim.financasmodernas.ui.FinancasApp
import br.com.mazim.financasmodernas.ui.MainViewModel
import br.com.mazim.financasmodernas.ui.theme.FinancasModernasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancasModernasTheme {
                val viewModel: MainViewModel = viewModel()
                FinancasApp(viewModel = viewModel)
            }
        }
    }
}
