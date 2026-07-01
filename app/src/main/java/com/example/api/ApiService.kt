package com.example.api

import com.example.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Map<String, Any>>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Map<String, Any>>

    @GET("api/incomes")
    suspend fun getIncomes(): Response<List<Expense>>

    @POST("api/incomes")
    suspend fun createIncome(@Body request: Expense): Response<Expense>

    @DELETE("api/incomes/{id}")
    suspend fun deleteIncome(@Path("id") id: Int): Response<Unit>

    @GET("api/expenses")
    suspend fun getExpenses(): Response<List<Expense>>

    @POST("api/expenses")
    suspend fun createExpense(@Body request: Expense): Response<Expense>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Response<Unit>

    @GET("api/categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("api/categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<Category>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>

    @GET("api/goals")
    suspend fun getGoals(): Response<List<Goal>>

    @POST("api/goals")
    suspend fun createGoal(@Body request: Goal): Response<Goal>

    @PUT("api/goals/{id}")
    suspend fun updateGoal(@Path("id") id: Int, @Body request: Goal): Response<Goal>

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Int): Response<Unit>

    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("api/credit-cards")
    suspend fun getCreditCards(): Response<List<Card>>

    @POST("api/credit-cards")
    suspend fun createCreditCard(@Body request: Card): Response<Card>

    @DELETE("api/credit-cards/{id}")
    suspend fun deleteCreditCard(@Path("id") id: Int): Response<Unit>

    @GET("api/users/profile")
    suspend fun getUserProfile(): Response<AuthResponse>

    @PUT("api/users/profile")
    suspend fun updateUserProfile(@Body request: ProfileUpdateRequest): Response<AuthResponse>

    @PUT("api/users/avatar")
    suspend fun updateUserAvatar(@Body request: Map<String, String>): Response<AuthResponse>

    @PUT("api/users/password")
    suspend fun updateUserPassword(@Body request: PasswordUpdateRequest): Response<Unit>

    @DELETE("api/users/profile")
    suspend fun deleteAccount(): Response<Unit>
}
