package com.example.kotlintemplate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.kotlintemplate.WebScreen
import com.example.kotlintemplate.ui.feature.home.HomeRoute

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.SCANQR
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
//        composable(Routes.HOME) {
//            HomeRoute()
//      }
        composable(Routes.SCANQR) {
            WebScreen(
                url = "http://192.168.1.6:3000",
                allowedHost = "192.168.1.6"
            )
        }
    }
}