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
    // Health
    @GET("api/health")
    suspend fun health(): Response<Unit>

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // Incomes
    @GET("api/incomes")
    suspend fun getIncomes(): Response<List<Expense>>

    @GET("api/incomes/{id}")
    suspend fun getIncomeById(@Path("id") id: Int): Response<Expense>

    @POST("api/incomes")
    suspend fun createIncome(@Body request: Expense): Response<Expense>

    @PUT("api/incomes/{id}")
    suspend fun updateIncome(@Path("id") id: Int, @Body request: Expense): Response<Expense>

    @DELETE("api/incomes/{id}")
    suspend fun deleteIncome(@Path("id") id: Int): Response<Unit>

    // Expenses
    @GET("api/expenses")
    suspend fun getExpenses(): Response<List<Expense>>

    @GET("api/expenses/{id}")
    suspend fun getExpenseById(@Path("id") id: Int): Response<Expense>

    @POST("api/expenses")
    suspend fun createExpense(@Body request: Expense): Response<Expense>

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: Int, @Body request: Expense): Response<Expense>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Response<Unit>

    // Categories
    @GET("api/categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("api/categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<Category>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>

    // Goals
    @GET("api/goals")
    suspend fun getGoals(): Response<List<Goal>>

    @GET("api/goals/{id}")
    suspend fun getGoalById(@Path("id") id: Int): Response<Goal>

    @POST("api/goals")
    suspend fun createGoal(@Body request: Goal): Response<Goal>

    @PUT("api/goals/{id}")
    suspend fun updateGoal(@Path("id") id: Int, @Body request: Goal): Response<Goal>

    @DELETE("api/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Int): Response<Unit>

    // Dashboard
    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    // Credit cards
    @GET("api/credit-cards")
    suspend fun getCreditCards(): Response<List<Card>>

    @GET("api/credit-cards/{id}")
    suspend fun getCreditCardById(@Path("id") id: Int): Response<Card>

    @POST("api/credit-cards")
    suspend fun createCreditCard(@Body request: Card): Response<Card>

    @PUT("api/credit-cards/{id}")
    suspend fun updateCreditCard(@Path("id") id: Int, @Body request: Card): Response<Card>

    @DELETE("api/credit-cards/{id}")
    suspend fun deleteCreditCard(@Path("id") id: Int): Response<Unit>

    // User profile
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
