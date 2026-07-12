package com.astran.russianspy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.astran.russianspy.navigation.RussianSpyNavGraph
import com.astran.russianspy.ui.theme.RussianSpyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RussianSpyTheme {
                RussianSpyNavGraph()
            }
        }
    }
}
