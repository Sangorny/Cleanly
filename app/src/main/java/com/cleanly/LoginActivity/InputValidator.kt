package com.cleanly


object InputValidator {

    fun isEmailValid(email: String): Boolean {
        val emailPattern = "^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,3}$"
        return email.matches(Regex(emailPattern))
    }

    fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^[a-zA-Z0-9]{6,}$"
        return password.matches(Regex(passwordPattern))
    }

    fun isNicknameValid(nickname: String): Boolean {
        return nickname.length <= 12 && nickname.matches(Regex("^[a-zA-Z0-9]*$"))
    }
}