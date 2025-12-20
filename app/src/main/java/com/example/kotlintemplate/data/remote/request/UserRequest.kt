package com.example.kotlintemplate.data.remote.request

data class UserRequest(
    val names: List<NameItemRequest>,
)

data class NameItemRequest(
    val name: String
)